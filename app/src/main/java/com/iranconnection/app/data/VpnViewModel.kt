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
        val context = getApplication<Application>()
        _state.value = _state.value.copy(status = VpnStatus.CONNECTING, seconds = 0L)
        WireGuardManager.connect(context)
        viewModelScope.launch {
            // Poll up to 30 s for IranVpnService to signal isRunning.
            repeat(30) {
                delay(1000)
                if (WireGuardManager.isConnected()) {
                    _state.value = _state.value.copy(status = VpnStatus.CONNECTED)
                    prefs.setConnected(true)
                    return@launch
                }
            }
            if (!WireGuardManager.isConnected()) {
                _state.value = _state.value.copy(status = VpnStatus.FAILED)
            }
        }
    }

    fun stopTunnel() {
        val context = getApplication<Application>()
        WireGuardManager.disconnect(context)
        _state.value = _state.value.copy(status = VpnStatus.DISCONNECTED, seconds = 0L)
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
