package net.packsi.tunnels

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import net.packsi.tunnels.data.ConnectionLog
import net.packsi.tunnels.data.WireGuardManager
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IranVpnService : GoBackend.VpnService() {

    companion object {
        const val ACTION_START = "ir.iranconnect.START"
        const val ACTION_STOP  = "ir.iranconnect.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // Serializes backend.setState calls. Without this, cancelling a slow connect races the
    // still in-flight UP call against the cancel's DOWN call — whichever finishes last wins,
    // which can leave the tunnel up (or isRunning wrong) even though the user asked to stop.
    private val stateMutex = Mutex()

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
                val browserEnabled = getSharedPreferences("vpn_settings", Context.MODE_PRIVATE)
                    .getBoolean("browser_through_vpn", false)
                val installedApps = WireGuardManager.getEnabledApps(this@IranVpnService, browserEnabled)
                ConnectionLog.add("installedApps (${installedApps.size}), browsers=$browserEnabled")

                if (installedApps.isEmpty()) {
                    WireGuardManager.lastError = "No apps selected. Go to Apps tab and enable at least one."
                    WireGuardManager.earlyFail = true
                    ConnectionLog.add("ERROR: no Iranian apps installed — aborting")
                    stopVpn()
                    return@launch
                }

                val config = WireGuardManager.buildConfig(this@IranVpnService, installedApps)
                if (config == null) {
                    WireGuardManager.lastError = "VPN config missing. Please restart the app."
                    WireGuardManager.earlyFail = true
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
                stateMutex.withLock {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                    isRunning = true
                }
                ConnectionLog.add("backend.setState returned — isRunning = true")
            } catch (e: Exception) {
                val msg = "${e::class.simpleName}: ${e.message}"
                WireGuardManager.lastError = "Connection error: $msg"
                WireGuardManager.earlyFail = true
                ConnectionLog.add("EXCEPTION in startVpn: $msg")
                e.cause?.let { ConnectionLog.add("  caused by: ${it::class.simpleName}: ${it.message}") }
                stopVpn()
            }
        }
    }

    private fun stopVpn() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        scope.launch {
            try {
                val t = WireGuardManager.activeTunnel
                if (t != null) {
                    val b = WireGuardManager.getBackend(this@IranVpnService)
                    stateMutex.withLock {
                        b.setState(t, Tunnel.State.DOWN, null)
                        isRunning = false
                    }
                    WireGuardManager.activeTunnel = null
                } else {
                    isRunning = false
                }
            } catch (_: Exception) {
                isRunning = false
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // User explicitly closed the app (swipe from recents / Close All) — stop the VPN.
        stopVpn()
    }

    override fun onRevoke() {
        super.onRevoke()
        isRunning = false
        WireGuardManager.activeTunnel = null
    }
}
