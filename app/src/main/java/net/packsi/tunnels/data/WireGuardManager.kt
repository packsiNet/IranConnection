package net.packsi.tunnels.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import net.packsi.tunnels.IranVpnService
import org.amnezia.awg.backend.GoBackend
import org.amnezia.awg.backend.Tunnel
import org.amnezia.awg.config.Config
import org.amnezia.awg.config.InetAddresses
import org.amnezia.awg.config.InetNetwork
import org.amnezia.awg.config.Interface as WgInterface
import org.amnezia.awg.config.Peer

object WireGuardManager {

    val DEFAULT_IRANIAN_APPS = listOf(
        "com.samanpr.blu",
        "ir.mobillet.app",
    )

    val BROWSER_PACKAGES = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.sec.android.app.sbrowser",
        "com.kiwibrowser.browser",
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
    )

    fun getEnabledApps(context: Context, includeBrowsers: Boolean): List<String> {
        val pm = context.packageManager
        val prefs = context.getSharedPreferences("enabled_apps", Context.MODE_PRIVATE)
        val iranianApps = if (prefs.contains("enabled_apps")) {
            prefs.getString("enabled_apps", "")
                .orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            val defaults = IranianAppList.packageNames
            prefs.edit().putString("enabled_apps", defaults.joinToString(",")).apply()
            defaults
        }
        // When the browser toggle is OFF, browsers must NEVER be tunneled — even if a
        // browser package leaked into the saved enabled_apps set (catalog detection or an
        // older build). So strip BROWSER_PACKAGES explicitly instead of merely not adding them.
        val allPkgs = if (includeBrowsers) {
            iranianApps + BROWSER_PACKAGES
        } else {
            iranianApps.filter { it !in BROWSER_PACKAGES }
        }
        return allPkgs.filter { pkg ->
            try { pm.getPackageInfo(pkg, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }
    }


    private const val FALLBACK_DNS = "1.1.1.1"

    /**
     * Returns a PUBLIC DNS IPv4 to put on the VPN interface. If the server-sent dns is a
     * usable public IPv4, keep it; otherwise (blank / private / CGNAT / IPv6) fall back to
     * 1.1.1.1, because a non-public DNS breaks excluded apps under split-tunnel.
     */
    private fun pickPublicDns(serverDns: String?): String {
        val d = serverDns?.trim().orEmpty()
        // Only accept a plain IPv4 literal; comma lists, hostnames, IPv6 → fallback.
        val octets = d.split(".")
        if (octets.size != 4) return FALLBACK_DNS
        val n = octets.map { it.toIntOrNull() ?: return FALLBACK_DNS }
        if (n.any { it !in 0..255 }) return FALLBACK_DNS
        val isPrivate =
            n[0] == 10 ||
            (n[0] == 172 && n[1] in 16..31) ||
            (n[0] == 192 && n[1] == 168) ||
            (n[0] == 100 && n[1] in 64..127) ||   // CGNAT 100.64.0.0/10
            n[0] == 127 ||                          // loopback
            n[0] == 0 || n[0] >= 224                // unspecified / multicast / reserved
        return if (isPrivate) FALLBACK_DNS else d
    }

    private var _backend: GoBackend? = null

    @Volatile
    internal var activeTunnel: Tunnel? = null

    @Volatile
    var lastError: String? = null

    @Volatile
    var earlyFail: Boolean = false

    fun getBackend(context: Context): GoBackend {
        if (_backend == null) {
            _backend = GoBackend(context.applicationContext)
        }
        return _backend!!
    }

    /**
     * Builds the WireGuard tunnel config.
     * [apps] is the whitelist of package names to route through the tunnel.
     * GoBackend internally calls addAllowedApplication() for each entry and catches
     * NameNotFoundException per package, so uninstalled apps are silently skipped.
     */
    fun buildConfig(context: Context, apps: List<String>): Config? {
        return try {
            val prefs = context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)
            val serverPubKey  = prefs.getString("server_pub_key",  null)
            val clientPrivKey = prefs.getString("client_priv_key", null)
            val address       = prefs.getString("address",         null)
            val dns           = prefs.getString("dns",             null)
            val endpoint      = prefs.getString("endpoint",        null)

            ConnectionLog.add("buildConfig prefs: endpoint=$endpoint address=$address dns=$dns")
            ConnectionLog.add("  pubKey=${serverPubKey?.take(8)}... privKey=${if (clientPrivKey != null) "OK" else "NULL"}")

            if (serverPubKey == null)  { ConnectionLog.add("ERROR: server_pub_key missing");  return null }
            if (clientPrivKey == null) { ConnectionLog.add("ERROR: client_priv_key missing"); return null }
            if (address == null)       { ConnectionLog.add("ERROR: address missing");          return null }
            if (endpoint == null)      { ConnectionLog.add("ERROR: endpoint missing");         return null }

            // DNS MUST be a PUBLIC resolver. Android applies VPN-interface DNS system-wide,
            // so the DNS IP has to be reachable BOTH ways:
            //   - tunneled apps reach it through the tunnel (0.0.0.0/0),
            //   - non-tunneled (excluded) apps reach it directly on the underlying network.
            // A public resolver (1.1.1.1) satisfies both. A tunnel-PRIVATE DNS (10.x etc.)
            // does NOT — excluded apps can't route to it and lose internet (the earlier
            // Samsung/A16 breakage). Without ANY DNS, tunneled apps fall back to the phone's
            // system resolver, which on some carriers (e.g. Canada CGNAT/IPv6) is unreachable
            // from the VPN server → connect succeeds but apps see no internet.
            val dnsServer = pickPublicDns(dns)
            ConnectionLog.add("  dns server=$dnsServer (server-sent=$dns)")
            // MTU pinned to 1280. WireGuard's default (1420) is too large on some client
            // uplinks (PPPoE/CGNAT/IPv6 6rd, common outside Iran): the handshake (small
            // packets) succeeds so the tunnel shows CONNECTED, but full-size data packets
            // (DNS replies, TLS) exceed the path MTU and get silently dropped → apps see
            // "no internet" / DNS_PROBE_FINISHED_NO_INTERNET. 1280 is the IPv6 minimum MTU
            // and passes on virtually every path.
            var ifaceBuilder = WgInterface.Builder()
                .parsePrivateKey(clientPrivKey)
                .addAddress(InetNetwork.parse(address))
                .addDnsServer(InetAddresses.parse(dnsServer))
                .setMtu(1280)

            // AmneziaWG obfuscation params. Must be present AND match the server's [Interface]
            // exactly, or the handshake never completes (plain-WG signature is DPI-blocked on some
            // foreign networks — see the abroad peer stuck at handshake=0). Applied only when all
            // are saved; a missing set means a plain-WG server, so we skip and behave like vanilla.
            val awg = prefs.run {
                mapOf(
                    "Jc" to getString("awg_jc", null), "Jmin" to getString("awg_jmin", null),
                    "Jmax" to getString("awg_jmax", null), "S1" to getString("awg_s1", null),
                    "S2" to getString("awg_s2", null), "H1" to getString("awg_h1", null),
                    "H2" to getString("awg_h2", null), "H3" to getString("awg_h3", null),
                    "H4" to getString("awg_h4", null),
                )
            }
            if (awg.values.all { it != null }) {
                ifaceBuilder
                    .parseJunkPacketCount(awg["Jc"]!!)
                    .parseJunkPacketMinSize(awg["Jmin"]!!)
                    .parseJunkPacketMaxSize(awg["Jmax"]!!)
                    .parseInitPacketJunkSize(awg["S1"]!!)
                    .parseResponsePacketJunkSize(awg["S2"]!!)
                    .parseInitPacketMagicHeader(awg["H1"]!!)
                    .parseResponsePacketMagicHeader(awg["H2"]!!)
                    .parseUnderloadPacketMagicHeader(awg["H3"]!!)
                    .parseTransportPacketMagicHeader(awg["H4"]!!)
                ConnectionLog.add("  AmneziaWG obfuscation applied (Jc=${awg["Jc"]} H1=${awg["H1"]})")
            } else {
                ConnectionLog.add("  AmneziaWG params absent → plain WG mode")
            }

            for (pkg in apps) {
                ifaceBuilder = ifaceBuilder.includeApplication(pkg)
            }
            ConnectionLog.add("  included ${apps.size} apps in tunnel")

            val peer = Peer.Builder()
                .parsePublicKey(serverPubKey)
                .parseEndpoint(endpoint)
                .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
                .setPersistentKeepalive(25)
                .build()

            Config.Builder()
                .setInterface(ifaceBuilder.build())
                .addPeer(peer)
                .build()
        } catch (e: Exception) {
            ConnectionLog.add("buildConfig EXCEPTION: ${e::class.simpleName}: ${e.message}")
            null
        }
    }

    fun connect(context: Context) {
        lastError = null
        earlyFail = false
        context.startService(
            Intent(context, IranVpnService::class.java).apply { action = IranVpnService.ACTION_START }
        )
    }

    fun disconnect(context: Context) {
        context.startService(
            Intent(context, IranVpnService::class.java).apply { action = IranVpnService.ACTION_STOP }
        )
    }

    fun isConnected(): Boolean = IranVpnService.isRunning
}
