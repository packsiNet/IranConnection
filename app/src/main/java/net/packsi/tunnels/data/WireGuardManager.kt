package net.packsi.tunnels.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import net.packsi.tunnels.IranVpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface as WgInterface
import com.wireguard.config.Peer

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

            // DNS is intentionally NOT set on the VPN interface.
            // Android applies VPN-interface DNS SYSTEM-WIDE (not per-app), so with
            // split-tunnel (includeApplication) the DNS IP is only routable inside the
            // tunnel while non-tunneled apps still get pointed at it — their DNS queries
            // are dropped and ALL apps lose internet (confirmed on Samsung/A16:
            // NetdEventListenerService logs isBlocked=true for non-tunneled apps).
            // Tunneled apps instead resolve via the WireGuard peer (0.0.0.0/0); the VPN
            // server must NAT/forward their DNS upstream.
            var ifaceBuilder = WgInterface.Builder()
                .parsePrivateKey(clientPrivKey)
                .addAddress(InetNetwork.parse(address))

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
