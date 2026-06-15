package com.iranconnection.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, FAILED }

data class VpnUiState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val seconds: Long = 0L,
    val selectedServerId: String? = null,
    val enabledApps: Set<String> = emptySet(),
    val loaded: Boolean = false,
    val serverIp: String? = null,
) {
    val connected: Boolean get() = status == VpnStatus.CONNECTED
    val statusLabel: String get() = when (status) {
        VpnStatus.DISCONNECTED -> "Not Connected"
        VpnStatus.CONNECTING   -> "Connecting..."
        VpnStatus.CONNECTED    -> "Connection Secure"
        VpnStatus.FAILED       -> "Connection Failed"
    }
}

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = VpnPreferences(app)

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                status = VpnStatus.DISCONNECTED,
                seconds = 0L,
                selectedServerId = prefs.selectedServer.first(),
                enabledApps = prefs.enabledApps.first(),
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
        val endpoint = context.getSharedPreferences("wireguard", android.content.Context.MODE_PRIVATE)
            .getString("endpoint", null)
        ConnectionLog.add("ViewModel: endpoint=$endpoint")
        val serverIp = endpoint?.let { ep ->
            if (ep.startsWith("[")) ep.substringBefore("]").removePrefix("[")
            else ep.substringBeforeLast(":").takeIf { it.isNotEmpty() } ?: ep
        }
        _state.value = _state.value.copy(status = VpnStatus.CONNECTING, seconds = 0L, serverIp = serverIp)
        WireGuardManager.connect(context)
        viewModelScope.launch {
            repeat(30) { i ->
                delay(1000)
                val connected = WireGuardManager.isConnected()
                if (i == 0 || i % 5 == 4 || connected) {
                    ConnectionLog.add("Poll ${i + 1}/30: isConnected=$connected")
                }
                if (connected) {
                    ConnectionLog.add("CONNECTED after ${i + 1}s")
                    _state.value = _state.value.copy(status = VpnStatus.CONNECTED)
                    prefs.setConnected(true)
                    return@launch
                }
            }
            if (!WireGuardManager.isConnected()) {
                ConnectionLog.add("FAILED: not connected after 30s polling")
                _state.value = _state.value.copy(status = VpnStatus.FAILED)
            }
        }
    }

    fun stopTunnel() {
        val context = getApplication<Application>()
        WireGuardManager.disconnect(context)
        _state.value = _state.value.copy(status = VpnStatus.DISCONNECTED, seconds = 0L, serverIp = null)
        viewModelScope.launch { prefs.setConnected(false) }
    }

    fun selectServer(id: String?) {
        _state.value = _state.value.copy(selectedServerId = id)
        viewModelScope.launch { prefs.setSelectedServer(id) }
    }

    fun toggleApp(id: String) {
        val cur = _state.value.enabledApps
        val next = if (cur.contains(id)) cur - id else cur + id
        _state.value = _state.value.copy(enabledApps = next)
        viewModelScope.launch { prefs.setEnabledApps(next) }
    }
}
