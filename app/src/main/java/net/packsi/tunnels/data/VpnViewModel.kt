package net.packsi.tunnels.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.packsi.tunnels.IranVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, FAILED }

data class VpnUiState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val seconds: Long = 0L,
    val selectedServerId: String? = null,
    val loaded: Boolean = false,
    val serverIp: String? = null,
    val errorMessage: String? = null,
    val browserVpnEnabled: Boolean = false,
    // True during a silent restart triggered by the browser toggle. The tunnel really goes down
    // and back up, but the UI must NOT flip to the red/disconnected look — only the labels change.
    val reconnecting: Boolean = false,
) {
    val connected: Boolean get() = status == VpnStatus.CONNECTED
    /** Drives the green "connected" visuals (button, rings, dot). Stays true across a toggle reconnect. */
    val active: Boolean get() = connected || reconnecting
    val statusLabel: String get() = when {
        reconnecting -> "Reconnecting…"
        else -> when (status) {
            VpnStatus.DISCONNECTED  -> "Not Connected"
            VpnStatus.CONNECTING    -> "Connecting..."
            VpnStatus.CONNECTED     -> "Connection Secure"
            VpnStatus.DISCONNECTING -> "Disconnecting..."
            VpnStatus.FAILED        -> "Connection Failed"
        }
    }
}

class VpnViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = VpnPreferences(app)
    private val vpnSettingsPrefs = app.getSharedPreferences("vpn_settings", android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val wasConnected   = prefs.connected.first()
            val savedSeconds   = prefs.seconds.first()
            val serverId       = prefs.selectedServer.first()
            val isNowConnected = IranVpnService.isRunning
            val browserEnabled = vpnSettingsPrefs.getBoolean("browser_through_vpn", false)

            _state.value = VpnUiState(
                status = if (isNowConnected) VpnStatus.CONNECTED else VpnStatus.DISCONNECTED,
                seconds = if (isNowConnected) savedSeconds else 0L,
                selectedServerId = serverId,
                loaded = true,
                browserVpnEnabled = browserEnabled,
            )
            if (!isNowConnected && wasConnected) {
                prefs.setConnected(false)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                // Keep ticking through a toggle reconnect so the timer doesn't visibly reset.
                if (s.connected || s.reconnecting) {
                    val ns = s.seconds + 1
                    _state.value = s.copy(seconds = ns)
                    prefs.setSeconds(ns)
                }
            }
        }
    }

    private var connectingJob: Job? = null
    private var browserToggleJob: Job? = null

    fun startTunnel() {
        ConnectionLog.clear()
        ConnectionLog.add("=== startTunnel ===")
        val context = getApplication<Application>()
        // Show CONNECTING instantly so the button feels responsive.
        _state.value = _state.value.copy(status = VpnStatus.CONNECTING, seconds = 0L, serverIp = null, errorMessage = null)
        connectingJob = viewModelScope.launch {
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
            // Poll every 500ms, timeout 15s. Also watch for early service failures.
            repeat(30) { i ->
                delay(500)
                if (WireGuardManager.earlyFail) {
                    val err = WireGuardManager.lastError ?: "Connection failed."
                    ConnectionLog.add("FAILED early: $err")
                    _state.value = _state.value.copy(status = VpnStatus.FAILED, errorMessage = err)
                    return@launch
                }
                val connected = withContext(Dispatchers.IO) { WireGuardManager.isConnected() }
                if (connected) {
                    ConnectionLog.add("CONNECTED after ${(i + 1) * 500}ms")
                    _state.value = _state.value.copy(status = VpnStatus.CONNECTED)
                    prefs.setConnected(true)
                    return@launch
                }
            }
            ConnectionLog.add("FAILED: not connected after 15s polling")
            _state.value = _state.value.copy(
                status = VpnStatus.FAILED,
                errorMessage = WireGuardManager.lastError ?: "Connection timed out.",
            )
        }
    }

    /** Lets the user cancel a connect attempt that's taking too long, instead of being stuck waiting on it. */
    fun cancelConnecting() {
        if (_state.value.status != VpnStatus.CONNECTING) return
        ConnectionLog.add("=== cancelConnecting ===")
        // Stop the in-flight connect poll first so it can't overwrite the DISCONNECTING/DISCONNECTED
        // state that stopTunnel() is about to set.
        connectingJob?.cancel()
        stopTunnel()
    }

    fun stopTunnel() {
        val context = getApplication<Application>()
        // Disconnecting also turns the browser-tunnel toggle off (it's only meaningful while up).
        vpnSettingsPrefs.edit().putBoolean("browser_through_vpn", false).apply()
        // Show DISCONNECTING instantly; tunnel teardown runs in background.
        _state.value = _state.value.copy(status = VpnStatus.DISCONNECTING, browserVpnEnabled = false)
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

    /** Tapping the (green) button mid-reconnect: abort the restart and fully disconnect. */
    fun cancelReconnect() {
        browserToggleJob?.cancel()
        _state.update { it.copy(reconnecting = false) }
        stopTunnel()
    }

    fun selectServer(id: String?) {
        _state.value = _state.value.copy(selectedServerId = id)
        viewModelScope.launch { prefs.setSelectedServer(id) }
    }

    fun setBrowserVpn(enabled: Boolean) {
        val wasConnected = _state.value.connected
        vpnSettingsPrefs.edit().putBoolean("browser_through_vpn", enabled).apply()
        _state.update { it.copy(browserVpnEnabled = enabled) }

        if (!wasConnected) return

        browserToggleJob?.cancel()
        browserToggleJob = viewModelScope.launch {
            val context = getApplication<Application>()
            // Silent restart: keep status CONNECTED (button stays green) and only flag `reconnecting`.
            // The granular DISCONNECTING/CONNECTING is surfaced via reconnectSubLabel, not the button.
            // We drive status directly here instead of calling startTunnel(), which would flip the
            // button to CONNECTING and reset the timer.
            _state.update { it.copy(reconnecting = true, status = VpnStatus.DISCONNECTING, errorMessage = null) }
            withContext(Dispatchers.IO) { WireGuardManager.disconnect(context) }
            var i = 0
            while (i < 20 && withContext(Dispatchers.IO) { WireGuardManager.isConnected() }) {
                delay(300)
                i++
            }
            prefs.setConnected(false)

            _state.update { it.copy(status = VpnStatus.CONNECTING) }
            withContext(Dispatchers.IO) { WireGuardManager.connect(context) }
            var j = 0
            var ok = false
            while (j < 30) {
                delay(500)
                if (WireGuardManager.earlyFail) break
                if (withContext(Dispatchers.IO) { WireGuardManager.isConnected() }) { ok = true; break }
                j++
            }
            if (ok) {
                _state.update { it.copy(status = VpnStatus.CONNECTED, reconnecting = false) }
                prefs.setConnected(true)
            } else {
                _state.update {
                    it.copy(
                        status = VpnStatus.FAILED,
                        reconnecting = false,
                        errorMessage = WireGuardManager.lastError ?: "Reconnect failed.",
                    )
                }
                prefs.setConnected(false)
            }
        }
    }
}
