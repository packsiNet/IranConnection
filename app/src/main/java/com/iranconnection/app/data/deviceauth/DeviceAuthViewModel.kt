package com.iranconnection.app.data.deviceauth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iranconnection.app.utils.DeviceIdHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Total number of splash loading steps — must match STEPS list in SplashScreen. */
private const val TOTAL_STEPS = 5

enum class AppStartState {
    CHECKING,
    LOADING_CONFIG,
    READY,
    ERROR,
}

data class DeviceAuthUiState(
    val appStartState: AppStartState = AppStartState.CHECKING,
    val errorMessage: String? = null,
    val loadingCompletedSteps: Int = 0,
    val loadingFailedStep: Int? = null,
)

class DeviceAuthViewModel(private val context: Context) : ViewModel() {

    private val _state = MutableStateFlow(DeviceAuthUiState())
    val state: StateFlow<DeviceAuthUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadVpnConfig() }
    }

    /**
     * Full loading flow — runs every time the app starts (or retries):
     *
     * Step 0  Connecting       — simulated delay
     * Step 1  Authenticating   — POST /api/auth/device-login  (no password)
     * Step 2  Fetching config  — GET  /api/vpn/config
     * Step 3  Detecting apps   — GET  /api/subscription/apps  (non-fatal)
     * Step 4  Routing rules    — simulated delay  → READY
     */
    private suspend fun loadVpnConfig() {
        _state.update {
            it.copy(
                appStartState = AppStartState.LOADING_CONFIG,
                loadingCompletedSteps = 0,
                loadingFailedStep = null,
                errorMessage = null,
            )
        }

        // Step 0: Connecting to server
        delay(600)
        _state.update { it.copy(loadingCompletedSteps = 1) }

        // Step 1: Authenticating session — POST /api/auth/device-login
        val deviceId = DeviceIdHelper.getDeviceId(context)
        when (val result = DeviceAuthRepository.deviceLogin(deviceId)) {
            is DeviceAuthResult.Success -> {
                DeviceAuthRepository.saveAuth(context, result.auth)
                _state.update { it.copy(loadingCompletedSteps = 2) }
            }
            else -> {
                val msg = when (result) {
                    is DeviceAuthResult.ValidationError -> result.message
                    is DeviceAuthResult.AccountDisabled -> result.message
                    is DeviceAuthResult.NetworkError    -> result.message
                    is DeviceAuthResult.WrongPassword   -> result.message
                    else -> "Authentication failed. Please retry."
                }
                _state.update {
                    it.copy(
                        loadingCompletedSteps = 2,
                        loadingFailedStep = 1,
                        appStartState = AppStartState.ERROR,
                        errorMessage = msg,
                    )
                }
                return
            }
        }

        // Step 2: Fetching VPN configuration — GET /api/vpn/config
        when (val result = DeviceAuthRepository.getVpnConfig(context)) {
            is VpnConfigResult.Success -> {
                DeviceAuthRepository.saveVpnConfig(context, result.config)
                _state.update { it.copy(loadingCompletedSteps = 3) }
            }
            is VpnConfigResult.Unauthorized -> {
                // Token just issued but already invalid — fatal
                DeviceAuthRepository.logout(context)
                _state.update {
                    it.copy(
                        loadingCompletedSteps = 3,
                        loadingFailedStep = 2,
                        appStartState = AppStartState.ERROR,
                        errorMessage = "Session error. Please retry.",
                    )
                }
                return
            }
            is VpnConfigResult.Error -> {
                _state.update {
                    it.copy(
                        loadingCompletedSteps = 3,
                        loadingFailedStep = 2,
                        appStartState = AppStartState.ERROR,
                        errorMessage = result.message,
                    )
                }
                return
            }
        }

        // Step 3: Detecting Persian apps — GET /api/subscription/apps (non-fatal)
        DeviceAuthRepository.getAppCatalog(context).fold(
            onSuccess = { catalog ->
                DeviceAuthRepository.saveAppCatalog(context, catalog)
                _state.update { it.copy(loadingCompletedSteps = 4) }
            },
            onFailure = {
                _state.update { it.copy(loadingCompletedSteps = 4, loadingFailedStep = 3) }
            },
        )

        // Step 4: Applying routing rules
        delay(350)
        _state.update { it.copy(loadingCompletedSteps = TOTAL_STEPS) }

        // Brief pause — user sees the green button before transition
        delay(600)
        _state.update { it.copy(appStartState = AppStartState.READY) }
    }

    fun retryLoadConfig() {
        viewModelScope.launch { loadVpnConfig() }
    }

    fun forceReady() {
        _state.update { it.copy(appStartState = AppStartState.READY) }
    }

    fun signOut() {
        DeviceAuthRepository.logout(context)
        viewModelScope.launch { loadVpnConfig() }
    }
}
