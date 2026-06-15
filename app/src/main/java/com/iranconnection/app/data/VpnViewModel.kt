package com.iranconnection.app.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, FAILED }

data class VpnUiState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val seconds: Long = 0L,
    val selectedServerId: String? = null,
    val appToggles: Map<String, Boolean> = emptyMap(),
    val loaded: Boolean = false,
    val serverIp: String? = null,
) {
    val connected: Boolean get() = status == VpnStatus.CONNECTED
    val statusLabel: String get() = when (status) {
        VpnStatus.DISCONNECTED  -> "Not Connected"
        VpnStatus.CONNECTING    -> "Connecting..."
        VpnStatus.CONNECTED     -> "Connection Secure"
        VpnStatus.DISCONNECTING -> "Disconnecting..."
        VpnStatus.FAILED        -> "Connection Failed"
    }
}

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = VpnPreferences(app)
    private val appPrefs = app.getSharedPreferences("wireguard", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    init {
        val initialToggles = IranianAppList.apps.associate { iranApp ->
            iranApp.packageName to appPrefs.getBoolean("app_enabled_${iranApp.packageName}", true)
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(
                status = VpnStatus.DISCONNECTED,
                seconds = 0L,
                selectedServerId = prefs.selectedServer.first(),
                appToggles = initialToggles,
                loaded = true,
            )
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (s.connected) {
                    val ns = s.seconds + 1
                    _state.value = s.copy(seconds = ns)
                    prefs.setSeconds(ns)
                }
            }
        }
    }

    fun startTunnel() {
        ConnectionLog.clear()
        ConnectionLog.add("=== startTunnel ===")
        val context = getApplication<Application>()
        // Show CONNECTING instantly so the button feels responsive.
        _state.value = _state.value.copy(status = VpnStatus.CONNECTING, seconds = 0L, serverIp = null)
        viewModelScope.launch {
            // Read prefs + start the tunnel off the main thread.
            val serverIp = withContext(Dispatchers.IO) {
                val endpoint = context.getSharedPreferences("wireguard", android.content.Context.MODE_PRIVATE)
                    .getString("endpoint", null)
                ConnectionLog.add("ViewModel: endpoint=$endpoint")
                WireGuardManager.connect(context)
                endpoint?.let { ep ->
                    if (ep.startsWith("[")) ep.substringBefore("]").removePrefix("[")
                    else ep.substringBeforeLast(":").takeIf { it.isNotEmpty() } ?: ep
                }
            }
            _state.value = _state.value.copy(serverIp = serverIp)
            // Poll isRunning every 500ms, timeout 15s.
            repeat(30) { i ->
                delay(500)
                val connected = withContext(Dispatchers.IO) { WireGuardManager.isConnected() }
                if (connected) {
                    ConnectionLog.add("CONNECTED after ${(i + 1) * 500}ms")
                    _state.value = _state.value.copy(status = VpnStatus.CONNECTED)
                    prefs.setConnected(true)
                    return@launch
                }
            }
            ConnectionLog.add("FAILED: not connected after 15s polling")
            _state.value = _state.value.copy(status = VpnStatus.FAILED)
        }
    }

    fun stopTunnel() {
        val context = getApplication<Application>()
        // Show DISCONNECTING instantly; tunnel teardown runs in background.
        _state.value = _state.value.copy(status = VpnStatus.DISCONNECTING)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { WireGuardManager.disconnect(context) }
            // Poll isRunning every 500ms, timeout 15s.
            var i = 0
            while (i < 30 && withContext(Dispatchers.IO) { WireGuardManager.isConnected() }) {
                delay(500)
                i++
            }
            _state.value = _state.value.copy(status = VpnStatus.DISCONNECTED, seconds = 0L, serverIp = null)
            prefs.setConnected(false)
        }
    }

    fun selectServer(id: String?) {
        _state.value = _state.value.copy(selectedServerId = id)
        viewModelScope.launch { prefs.setSelectedServer(id) }
    }

    fun setAppEnabled(pkg: String, enabled: Boolean) {
        val next = _state.value.appToggles + (pkg to enabled)
        _state.value = _state.value.copy(appToggles = next)
        appPrefs.edit().putBoolean("app_enabled_$pkg", enabled).apply()
    }
}
