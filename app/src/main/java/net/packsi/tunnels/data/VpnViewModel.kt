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
    /** Non-null while an ad-gated session is active. Counts down to 0 then auto-disconnects. */
    val adSessionRemaining: Long? = null,
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
    private val vpnSettingsPrefs = app.getSharedPreferences("vpn_settings", android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(VpnUiState())
    val state: StateFlow<VpnUiState> = _state.asStateFlow()

    companion object {
        const val AD_SESSION_SECONDS = 1800L // 30 minutes
    }

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
                // adSessionRemaining is not persisted — restored sessions run in normal (no-countdown) mode
            )
            if (!isNowConnected && wasConnected) {
                prefs.setConnected(false)
            }
        }

        // Unified timer loop: counts up in normal mode, counts down in ad session mode.
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                if (!s.connected) continue

                val rem = s.adSessionRemaining
                if (rem != null) {
                    if (rem <= 1) {
                        // Ad session expired — clear remaining first to prevent re-entry, then disconnect.
                        _state.value = s.copy(adSessionRemaining = null, seconds = 0L)
                        stopTunnel()
                    } else {
                        _state.value = s.copy(adSessionRemaining = rem - 1)
                    }
                } else {
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
        _state.value = _state.value.copy(
            status = VpnStatus.CONNECTING, seconds = 0L, serverIp = null,
            errorMessage = null, adSessionRemaining = null,
        )
        connectingJob = viewModelScope.launch {
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

    /**
     * Same as [startTunnel] but starts a 30-minute ad-gated session on success.
     * The countdown timer drives auto-disconnect when it reaches zero.
     */
    fun startTunnelInAdMode() {
        ConnectionLog.clear()
        ConnectionLog.add("=== startTunnelInAdMode ===")
        val context = getApplication<Application>()
        _state.value = _state.value.copy(
            status = VpnStatus.CONNECTING, seconds = 0L, serverIp = null,
            errorMessage = null, adSessionRemaining = null,
        )
        connectingJob = viewModelScope.launch {
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
                    ConnectionLog.add("CONNECTED (ad mode) after ${(i + 1) * 500}ms")
                    _state.value = _state.value.copy(
                        status = VpnStatus.CONNECTED,
                        adSessionRemaining = AD_SESSION_SECONDS,
                    )
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

    fun cancelConnecting() {
        if (_state.value.status != VpnStatus.CONNECTING) return
        ConnectionLog.add("=== cancelConnecting ===")
        connectingJob?.cancel()
        stopTunnel()
    }

    fun stopTunnel() {
        val context = getApplication<Application>()
        _state.value = _state.value.copy(status = VpnStatus.DISCONNECTING)
        viewModelScope.launch {
            withContext(Dispatchers.IO) { WireGuardManager.disconnect(context) }
            var i = 0
            while (i < 30 && withContext(Dispatchers.IO) { WireGuardManager.isConnected() }) {
                delay(500)
                i++
            }
            _state.value = _state.value.copy(
                status = VpnStatus.DISCONNECTED,
                seconds = 0L,
                serverIp = null,
                adSessionRemaining = null,
            )
            prefs.setConnected(false)
        }
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
            _state.update { it.copy(status = VpnStatus.DISCONNECTING) }
            withContext(Dispatchers.IO) { WireGuardManager.disconnect(context) }
            var i = 0
            while (i < 20 && withContext(Dispatchers.IO) { WireGuardManager.isConnected() }) {
                delay(300)
                i++
            }
            prefs.setConnected(false)
            startTunnel()
        }
    }
}
