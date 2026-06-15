package com.iranconnection.app

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import com.iranconnection.app.data.WireGuardManager
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IranVpnService : GoBackend.VpnService() {

    companion object {
        const val ACTION_START = "ir.iranconnect.START"
        const val ACTION_STOP = "ir.iranconnect.STOP"

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Registers this service instance with GoBackend so it can create the VPN interface.
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP  -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
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
                val config = WireGuardManager.buildConfig(this@IranVpnService)
                    ?: run { stopVpn(); return@launch }

                val backend = WireGuardManager.getBackend(this@IranVpnService)
                val tunnel = object : Tunnel {
                    override fun getName() = "IranConnect"
                    override fun onStateChange(newState: Tunnel.State) {}
                }
                WireGuardManager.activeTunnel = tunnel
                backend.setState(tunnel, Tunnel.State.UP, config)
                isRunning = true
            } catch (e: Exception) {
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
