package com.iranconnection.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import com.iranconnection.app.data.ConnectionLog
import com.iranconnection.app.data.VpnPreferences
import com.iranconnection.app.data.WireGuardManager
import kotlinx.coroutines.flow.first
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IranVpnService : GoBackend.VpnService() {

    companion object {
        const val ACTION_START = "ir.iranconnect.START"
        const val ACTION_STOP  = "ir.iranconnect.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP  -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        ConnectionLog.add("startVpn called")
        NotificationHelper.createNotificationChannel(this)
        val notif = NotificationHelper.buildNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this, NotificationHelper.NOTIFICATION_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notif)
        }

        scope.launch {
            try {
                val prefs = getSharedPreferences("wireguard", Context.MODE_PRIVATE)
                val appsString = prefs.getString("iranian_apps", "") ?: ""
                ConnectionLog.add("iranian_apps pref: '${appsString.take(120)}'")
                android.util.Log.d("IranVpn", "raw pref value: $appsString")

                val userEnabled = VpnPreferences(this@IranVpnService).enabledApps.first()
                val chromePackages = if (userEnabled.contains("chrome"))
                    listOf("com.android.chrome", "com.google.android.apps.chrome") else emptyList()

                val configuredApps = (if (appsString.isNotEmpty()) {
                    appsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    WireGuardManager.DEFAULT_IRANIAN_APPS
                } + chromePackages).distinct()
                ConnectionLog.add("configuredApps (${configuredApps.size}): ${configuredApps.joinToString()}")
                android.util.Log.d("IranVpn", "split result: $configuredApps")

                configuredApps.forEach { pkg ->
                    val installed = try {
                        packageManager.getPackageInfo(pkg.trim(), 0)
                        true
                    } catch (e: PackageManager.NameNotFoundException) {
                        false
                    }
                    android.util.Log.d("IranVpn", "checking: '$pkg' → installed=$installed")
                }

                val installedApps = configuredApps.filter { pkg ->
                    try {
                        packageManager.getPackageInfo(pkg, 0)
                        true
                    } catch (e: PackageManager.NameNotFoundException) {
                        ConnectionLog.add("  not installed: $pkg")
                        false
                    }
                }
                ConnectionLog.add("installedApps (${installedApps.size}): ${installedApps.joinToString()}")

                if (installedApps.isEmpty()) {
                    ConnectionLog.add("ERROR: no Iranian apps installed — aborting")
                    stopVpn()
                    return@launch
                }

                val config = WireGuardManager.buildConfig(this@IranVpnService, installedApps)
                if (config == null) {
                    ConnectionLog.add("ERROR: buildConfig returned null")
                    stopVpn()
                    return@launch
                }
                ConnectionLog.add("Config built OK")

                val backend = WireGuardManager.getBackend(this@IranVpnService)
                val tunnel = object : Tunnel {
                    override fun getName() = "IranConnect"
                    override fun onStateChange(newState: Tunnel.State) {
                        ConnectionLog.add("Tunnel state → $newState")
                    }
                }
                WireGuardManager.activeTunnel = tunnel
                ConnectionLog.add("Calling backend.setState(UP)...")
                backend.setState(tunnel, Tunnel.State.UP, config)
                ConnectionLog.add("backend.setState returned — isRunning = true")
                isRunning = true
            } catch (e: Exception) {
                ConnectionLog.add("EXCEPTION in startVpn: ${e::class.simpleName}: ${e.message}")
                e.cause?.let { ConnectionLog.add("  caused by: ${it::class.simpleName}: ${it.message}") }
                isRunning = false
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        scope.launch {
            try {
                val t = WireGuardManager.activeTunnel ?: return@launch
                val b = WireGuardManager.getBackend(this@IranVpnService)
                b.setState(t, Tunnel.State.DOWN, null)
                WireGuardManager.activeTunnel = null
            } catch (_: Exception) {}
        }
        isRunning = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        super.onRevoke()
        isRunning = false
        WireGuardManager.activeTunnel = null
    }
}
