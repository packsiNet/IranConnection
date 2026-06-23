package net.packsi.tunnels.data

import android.content.Context
import android.content.Intent
import net.packsi.tunnels.IranVpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface as WgInterface
import com.wireguard.config.Peer
import java.net.InetAddress

object WireGuardManager {

    val DEFAULT_IRANIAN_APPS = listOf(
        "com.samanpr.blu",
        "ir.mobillet.app",
        "com.android.chrome",
    )

    private var _backend: GoBackend? = null

    @Volatile
    internal var activeTunnel: Tunnel? = null

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
            if (dns == null)           { ConnectionLog.add("ERROR: dns missing");              return null }
            if (endpoint == null)      { ConnectionLog.add("ERROR: endpoint missing");         return null }

            var ifaceBuilder = WgInterface.Builder()
                .parsePrivateKey(clientPrivKey)
                .addAddress(InetNetwork.parse(address))
                .addDnsServer(InetAddress.getByName(dns))

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
