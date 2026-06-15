#!/usr/bin/env bash
#
# setup-wireguard-server.sh
# -----------------------------------------------------------------------------
# Complete, idempotent WireGuard server bootstrap for the "IranConnection" app.
#
# What it does:
#   1. Verifies it runs as root on a supported Linux distro.
#   2. Installs WireGuard, UFW, iptables, curl, jq, qrencode.
#   3. Generates server + client key pairs (re-used if already present).
#   4. Detects the public IP and the default egress interface.
#   5. Enables IPv4/IPv6 forwarding and configures NAT (masquerade).
#   6. Writes /etc/wireguard/wg0.conf and starts wg-quick@wg0.
#   7. Opens the WireGuard UDP port (and SSH) on UFW, enables forwarding.
#   8. Builds config.json matching the Android app schema.
#   9. (Optional) Pushes config.json to the GitHub Gist the app reads, so the
#      phone connects to THIS server automatically on next launch.
#
# Usage:
#   sudo bash setup-wireguard-server.sh
#
# Environment overrides (all optional):
#   WG_PORT          WireGuard listen port            (default: 51820)
#   WG_NET           Tunnel subnet                     (default: 10.66.66.0/24)
#   WG_DNS           DNS pushed to client              (default: 1.1.1.1)
#   PUBLIC_IP        Force public IP (skip detection)
#   GITHUB_TOKEN     GitHub PAT with "gist" scope -> enables auto gist push
#   GIST_ID          Target gist id (default: app's gist id)
#   GIST_FILE        File name inside the gist          (default: config.json)
#   SSH_PORT         SSH port to keep open on UFW       (default: 22)
# -----------------------------------------------------------------------------

set -Eeuo pipefail

# ----------------------------- configuration ---------------------------------
WG_IFACE="wg0"
WG_PORT="${WG_PORT:-51820}"
WG_NET="${WG_NET:-10.66.66.0/24}"
WG_DNS="${WG_DNS:-1.1.1.1}"
WG_DIR="/etc/wireguard"
SSH_PORT="${SSH_PORT:-22}"

# Gist defaults taken from the app (MainActivity.kt CONFIG_URL).
GIST_ID="${GIST_ID:-4358f6d56dcb7cceefb38f6e3a7573ba}"
GIST_FILE="${GIST_FILE:-config.json}"
CONFIG_VERSION="1"

# Server tunnel IP = .1 of the subnet, client = .2
WG_BASE="${WG_NET%.*}"            # e.g. 10.66.66
WG_SERVER_IP="${WG_BASE}.1"
WG_CLIENT_IP="${WG_BASE}.2"
WG_NET_PREFIX="${WG_NET##*/}"     # e.g. 24

# Default Iranian apps that the app whitelists (kept in sync with WireGuardManager.kt)
IRANIAN_APPS_JSON='["com.samanpr.blu","ir.mobillet.app","com.android.chrome"]'

# ----------------------------- pretty logging --------------------------------
if [[ -t 1 ]]; then
  C_OK=$'\e[32m'; C_WARN=$'\e[33m'; C_ERR=$'\e[31m'; C_INFO=$'\e[36m'; C_RST=$'\e[0m'
else
  C_OK=""; C_WARN=""; C_ERR=""; C_INFO=""; C_RST=""
fi
log()  { printf '%s[*]%s %s\n' "$C_INFO" "$C_RST" "$*"; }
ok()   { printf '%s[+]%s %s\n' "$C_OK"   "$C_RST" "$*"; }
warn() { printf '%s[!]%s %s\n' "$C_WARN" "$C_RST" "$*" >&2; }
die()  { printf '%s[x]%s %s\n' "$C_ERR"  "$C_RST" "$*" >&2; exit 1; }

trap 'die "Failed at line $LINENO. See output above."' ERR

# ----------------------------- pre-flight checks -----------------------------
[[ "${EUID}" -eq 0 ]] || die "Run as root:  sudo bash $0"

[[ -r /etc/os-release ]] || die "Cannot read /etc/os-release; unsupported OS."
# shellcheck disable=SC1091
. /etc/os-release
log "Detected OS: ${PRETTY_NAME:-$ID}"

case "${ID:-}${ID_LIKE:-}" in
  *debian*|*ubuntu*) PKG="apt"  ;;
  *rhel*|*fedora*|*centos*|*rocky*|*alma*) PKG="dnf" ;;
  *) die "Unsupported distro '${ID:-unknown}'. Supported: Debian/Ubuntu, RHEL/Fedora family." ;;
esac

# WireGuard needs a kernel module or kernel >=5.6 (built-in). Warn, don't block.
if ! modprobe wireguard 2>/dev/null && [[ ! -d /sys/module/wireguard ]]; then
  warn "wireguard kernel module not loaded yet; package install will pull wireguard-dkms if needed."
fi

# ----------------------------- install packages ------------------------------
install_packages() {
  log "Installing prerequisites via ${PKG} ..."
  export DEBIAN_FRONTEND=noninteractive
  if [[ "$PKG" == "apt" ]]; then
    apt-get update -y
    apt-get install -y wireguard wireguard-tools ufw iptables curl jq qrencode iproute2
  else
    dnf install -y epel-release || true
    dnf install -y wireguard-tools ufw iptables curl jq qrencode iproute || \
      die "Package install failed. Ensure EPEL/repos are reachable."
  fi
  ok "Packages installed."
}
install_packages

command -v wg >/dev/null      || die "wg not found after install."
command -v wg-quick >/dev/null|| die "wg-quick not found after install."
command -v jq >/dev/null      || die "jq not found after install."

# ----------------------------- key generation --------------------------------
umask 077
mkdir -p "$WG_DIR"

gen_key_pair() {
  # $1 = name prefix (server|client). Creates <name>_private / <name>_public if absent.
  local name="$1"
  if [[ -s "${WG_DIR}/${name}_private" && -s "${WG_DIR}/${name}_public" ]]; then
    log "Re-using existing ${name} keys."
  else
    log "Generating ${name} key pair ..."
    wg genkey | tee "${WG_DIR}/${name}_private" | wg pubkey > "${WG_DIR}/${name}_public"
  fi
  chmod 600 "${WG_DIR}/${name}_private" "${WG_DIR}/${name}_public"
}
gen_key_pair server
gen_key_pair client

SERVER_PRIV="$(cat "${WG_DIR}/server_private")"
SERVER_PUB="$(cat "${WG_DIR}/server_public")"
CLIENT_PRIV="$(cat "${WG_DIR}/client_private")"
CLIENT_PUB="$(cat "${WG_DIR}/client_public")"

# ----------------------------- network detection -----------------------------
detect_public_ip() {
  if [[ -n "${PUBLIC_IP:-}" ]]; then echo "$PUBLIC_IP"; return; fi
  local ip=""
  for url in "https://api.ipify.org" "https://ifconfig.me/ip" "https://ipinfo.io/ip"; do
    ip="$(curl -fsS --max-time 8 "$url" 2>/dev/null | tr -d '[:space:]')" || true
    [[ "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] && { echo "$ip"; return; }
  done
  die "Could not auto-detect public IP. Re-run with PUBLIC_IP=<your.ip> ."
}
PUBLIC_IP="$(detect_public_ip)"
ok "Public IP: ${PUBLIC_IP}"

# Default egress interface (used for NAT masquerade rule).
WAN_IFACE="$(ip -4 route show default | awk '/default/ {print $5; exit}')"
[[ -n "$WAN_IFACE" ]] || die "Cannot determine default network interface."
ok "WAN interface: ${WAN_IFACE}"

# ----------------------------- IP forwarding ---------------------------------
log "Enabling IP forwarding ..."
cat > /etc/sysctl.d/99-wireguard-forward.conf <<EOF
net.ipv4.ip_forward = 1
net.ipv6.conf.all.forwarding = 1
EOF
sysctl --system >/dev/null
ok "IP forwarding enabled."

# ----------------------------- wg0.conf --------------------------------------
log "Writing ${WG_DIR}/${WG_IFACE}.conf ..."
cat > "${WG_DIR}/${WG_IFACE}.conf" <<EOF
# Managed by setup-wireguard-server.sh — do not edit by hand.
[Interface]
Address    = ${WG_SERVER_IP}/${WG_NET_PREFIX}
ListenPort = ${WG_PORT}
PrivateKey = ${SERVER_PRIV}

# NAT: route tunnel traffic out via the WAN interface.
PostUp   = iptables  -A FORWARD -i %i -j ACCEPT; iptables  -A FORWARD -o %i -j ACCEPT; iptables  -t nat -A POSTROUTING -o ${WAN_IFACE} -j MASQUERADE
PostDown = iptables  -D FORWARD -i %i -j ACCEPT; iptables  -D FORWARD -o %i -j ACCEPT; iptables  -t nat -D POSTROUTING -o ${WAN_IFACE} -j MASQUERADE

[Peer]
# IranConnection Android client
PublicKey  = ${CLIENT_PUB}
AllowedIPs = ${WG_CLIENT_IP}/32
EOF
chmod 600 "${WG_DIR}/${WG_IFACE}.conf"
ok "wg0.conf written."

# ----------------------------- UFW firewall ----------------------------------
# Provider has no port restriction; only UFW must allow the port.
log "Configuring UFW ..."
ufw allow "${SSH_PORT}/tcp" comment 'SSH' >/dev/null || true
ufw allow "${WG_PORT}/udp" comment 'WireGuard' >/dev/null

# Allow forwarding through UFW (otherwise NAT traffic is dropped).
if ! grep -q '^DEFAULT_FORWARD_POLICY="ACCEPT"' /etc/default/ufw 2>/dev/null; then
  sed -i 's/^DEFAULT_FORWARD_POLICY=.*/DEFAULT_FORWARD_POLICY="ACCEPT"/' /etc/default/ufw
fi

# Enable UFW non-interactively (idempotent).
if ufw status | grep -q "Status: inactive"; then
  log "Enabling UFW (SSH on ${SSH_PORT}/tcp is already allowed — connection is safe)."
  yes | ufw enable >/dev/null
else
  ufw reload >/dev/null || true
fi
ok "UFW: SSH ${SSH_PORT}/tcp + WireGuard ${WG_PORT}/udp open; forwarding ACCEPT."

# ----------------------------- start service ---------------------------------
log "Starting WireGuard service ..."
systemctl enable "wg-quick@${WG_IFACE}" >/dev/null 2>&1 || true
# Restart to pick up regenerated config / keys.
systemctl restart "wg-quick@${WG_IFACE}"
sleep 1
if ! wg show "${WG_IFACE}" >/dev/null 2>&1; then
  die "wg-quick@${WG_IFACE} failed to come up. Check: journalctl -u wg-quick@${WG_IFACE}"
fi
ok "WireGuard interface ${WG_IFACE} is up."

# ----------------------------- build config.json -----------------------------
ENDPOINT="${PUBLIC_IP}:${WG_PORT}"
CONFIG_JSON="$(jq -n \
  --arg endpoint   "$ENDPOINT" \
  --arg spub       "$SERVER_PUB" \
  --arg cpriv      "$CLIENT_PRIV" \
  --arg caddr      "${WG_CLIENT_IP}/32" \
  --arg dns        "$WG_DNS" \
  --arg version    "$CONFIG_VERSION" \
  --argjson apps   "$IRANIAN_APPS_JSON" \
  '{
     server_endpoint:    $endpoint,
     server_public_key:  $spub,
     client_private_key: $cpriv,
     client_address:     $caddr,
     dns:                $dns,
     version:            $version,
     iranian_apps:       $apps
   }')"

OUT_FILE="${WG_DIR}/${GIST_FILE}"
printf '%s\n' "$CONFIG_JSON" > "$OUT_FILE"
chmod 600 "$OUT_FILE"
ok "Wrote app config -> ${OUT_FILE}"

# ----------------------------- push to gist ----------------------------------
push_gist() {
  [[ -n "${GITHUB_TOKEN:-}" ]] || {
    warn "GITHUB_TOKEN not set — skipping gist upload."
    warn "To auto-update the app, re-run with:  GITHUB_TOKEN=ghp_xxx bash $0"
    return 0
  }
  log "Pushing config.json to gist ${GIST_ID} ..."
  local payload http
  payload="$(jq -n \
    --arg fname "$GIST_FILE" \
    --arg content "$CONFIG_JSON" \
    '{files: {($fname): {content: $content}}}')"

  http="$(curl -fsS -w '%{http_code}' -o /tmp/gist_resp.json \
    -X PATCH \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com/gists/${GIST_ID}" \
    -d "$payload")" || true

  if [[ "$http" == "200" ]]; then
    ok "Gist updated. The Android app will fetch this server on next launch."
  else
    warn "Gist update failed (HTTP ${http}). Response:"
    jq -r '.message // "unknown error"' /tmp/gist_resp.json >&2 2>/dev/null || cat /tmp/gist_resp.json >&2
    warn "Check: token has 'gist' scope, token owns gist ${GIST_ID}, filename '${GIST_FILE}' matches the raw URL."
  fi
  rm -f /tmp/gist_resp.json
}
push_gist

# ----------------------------- summary ---------------------------------------
echo
ok "================= DONE ================="
echo "  Endpoint        : ${ENDPOINT}"
echo "  Server pubkey   : ${SERVER_PUB}"
echo "  Client address  : ${WG_CLIENT_IP}/32"
echo "  DNS             : ${WG_DNS}"
echo "  Config file     : ${OUT_FILE}"
echo
echo "  Verify tunnel   : wg show ${WG_IFACE}"
echo "  Service logs    : journalctl -u wg-quick@${WG_IFACE} -e"
echo
if [[ -z "${GITHUB_TOKEN:-}" ]]; then
  echo "  Gist NOT pushed. Either:"
  echo "    A) re-run with GITHUB_TOKEN set, or"
  echo "    B) paste the contents of ${OUT_FILE} into the gist manually:"
  echo "       https://gist.github.com/${GIST_ID}"
  echo
  log "config.json contents:"
  cat "$OUT_FILE"
fi
ok "========================================"
