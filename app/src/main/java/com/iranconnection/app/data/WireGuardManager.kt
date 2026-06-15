package com.iranconnection.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.iranconnection.app.IranVpnService
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.InetNetwork
import com.wireguard.config.Interface as WgInterface
import com.wireguard.config.Peer
import java.net.InetAddress

object WireGuardManager {

    private val IRANIAN_APPS = listOf(
        "com.bmi.ibanking",
        "com.mellat.bank",
        "com.tejarat.bank",
        "com.digikala",
        "ir.snapp.passenger",
        "com.snappfood",
        "ir.tapsi.passenger",
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

    fun buildConfig(context: Context): Config? = try {
        val prefs = context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)
        val serverPubKey  = prefs.getString("server_pub_key",  null) ?: return null
        val clientPrivKey = prefs.getString("client_priv_key", null) ?: return null
        val address       = prefs.getString("address",         null) ?: return null
        val dns           = prefs.getString("dns",             null) ?: return null
        val endpoint      = prefs.getString("endpoint",        null) ?: return null

        val pm = context.packageManager
        val installedApps = IRANIAN_APPS.filter { pkg ->
            try { pm.getPackageInfo(pkg, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }

        var ifaceBuilder = WgInterface.Builder()
            .parsePrivateKey(clientPrivKey)
            .addAddress(InetNetwork.parse(address))
            .addDnsServer(InetAddress.getByName(dns))

        // GoBackend calls addAllowedApplication() for each of these when establishing the VPN.
        for (app in installedApps) {
            ifaceBuilder = ifaceBuilder.includeApplication(app)
        }

        val iface = ifaceBuilder.build()

        val peer = Peer.Builder()
            .parsePublicKey(serverPubKey)
            .parseEndpoint(endpoint)
            .addAllowedIp(InetNetwork.parse("0.0.0.0/0"))
            .setPersistentKeepalive(25)
            .build()

        Config.Builder()
            .setInterface(iface)
            .addPeer(peer)
            .build()
    } catch (e: Exception) {
        null
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
