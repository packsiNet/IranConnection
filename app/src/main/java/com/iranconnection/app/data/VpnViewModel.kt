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

data class VpnUiState(
    val connected: Boolean = true,
    val seconds: Long = 2438L,
    val selectedServerId: String? = "us",
    val enabledApps: Set<String> = setOf("melli"),
    val loaded: Boolean = false,
)

/**
 * Holds shared connection state for all three screens and drives the live
 * session timer. Equivalent to the prototype's per-page DCLogic state, unified
 * so Home / Servers / Apps stay in sync and survive process death via DataStore.
 */
class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = VpnPreferences(app)

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                connected = prefs.connected.first(),
                seconds = prefs.seconds.first(),
                selectedServerId = prefs.selectedServer.first(),
                enabledApps = prefs.enabledApps.first(),
                loaded = true,
            )
        }
        // Tick every second; only advances while connected (matches prototype).
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

    /** Toggle power button: flips connection and resets timer on (re)connect. */
    fun toggleConnection() {
        val nc = !_state.value.connected
        val newSecs = if (nc) 0L else _state.value.seconds
        _state.value = _state.value.copy(connected = nc, seconds = newSecs)
        viewModelScope.launch {
            prefs.setConnected(nc)
            if (nc) prefs.setSeconds(0L)
        }
    }

    /** Select a server (Servers screen). null = disconnect that selection. */
    fun selectServer(id: String?) {
        _state.value = _state.value.copy(selectedServerId = id)
        viewModelScope.launch { prefs.setSelectedServer(id) }
    }

    /** Flip a bank app toggle (Apps screen). */
    fun toggleApp(id: String) {
        val cur = _state.value.enabledApps
        val next = if (cur.contains(id)) cur - id else cur + id
        _state.value = _state.value.copy(enabledApps = next)
        viewModelScope.launch { prefs.setEnabledApps(next) }
    }
}
