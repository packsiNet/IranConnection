package net.packsi.tunnels.data.deviceauth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import net.packsi.tunnels.IranVpnService
import net.packsi.tunnels.data.appconfig.AppConfig
import net.packsi.tunnels.data.appconfig.AppConfigRepository
import net.packsi.tunnels.data.auth.TokenStore
import net.packsi.tunnels.data.subscription.CatalogCache
import net.packsi.tunnels.data.subscription.SubscriptionRepository
import net.packsi.tunnels.utils.DeviceIdHelper
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
        val hasCachedData = DeviceAuthRepository.isLoggedIn(context) &&
                            DeviceAuthRepository.hasCachedVpnConfig(context)

        if (hasCachedData) {
            // Cached session → show the app IMMEDIATELY (no splash flash). Then pull /api/app/config
            // in the background: a CHANGED adsEnabled or iranianAppsUpdateVersion re-runs the splash
            // to refresh data live; otherwise just refresh token/config silently.
            _state.update { it.copy(appStartState = AppStartState.READY) }
            viewModelScope.launch {
                val config = AppConfigRepository.fetch()
                val adsChanged = applyAdsEnabled(config)
                val appsVersion = appsVersionOf(config)
                val appsVersionChanged = appsVersion != null &&
                                         appsVersion != CatalogCache.savedVersion(context)
                if (adsChanged || appsVersionChanged) {
                    loadVpnConfig(appsVersion)   // flips back to splash, refetches, returns to READY
                } else {
                    refreshConfigInBackground()
                }
            }
        } else {
            // First run / cleared cache → full splash, capturing the current app config.
            viewModelScope.launch {
                val config = AppConfigRepository.fetch()
                applyAdsEnabled(config)
                loadVpnConfig(appsVersionOf(config))
            }
        }
    }

    /** The iranianAppsUpdateVersion from config, or null when absent/blank (treated as "no change"). */
    private fun appsVersionOf(config: AppConfig?): String? =
        config?.iranianAppsUpdateVersion?.ifBlank { null }

    /**
     * Persists the ads master switch from /api/app/config (its sole source of truth) and returns
     * whether it differs from the previously stored value. Null config → no change.
     */
    private fun applyAdsEnabled(config: AppConfig?): Boolean {
        if (config == null) return false
        val changed = config.adsEnabled != TokenStore.adsEnabled
        TokenStore.saveAdsEnabled(config.adsEnabled)
        return changed
    }

    /** Manual refresh (colored refresh button on Home) — always re-runs the splash to refresh
     *  config, VPN config, and the apps catalog, regardless of whether anything changed. */
    fun manualRefresh() {
        viewModelScope.launch {
            val config = AppConfigRepository.fetch()
            applyAdsEnabled(config)
            loadVpnConfig(appsVersionOf(config))
        }
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
    private suspend fun loadVpnConfig(appsVersion: String? = null) {
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

        // Step 3: Detecting Persian apps — GET /api/subscription/apps (non-fatal).
        // This is the once-per-cold-start catalog fetch; cache it so the Apps screen reads it
        // without re-hitting the network (its refresh button is the other fetch path).
        SubscriptionRepository.getAppCatalog().fold(
            onSuccess = { catalog ->
                if (catalog.isNotEmpty()) {
                    CatalogCache.save(context, catalog)
                    // Mark this catalog as current for the RC version that triggered the fetch, so
                    // the next launch with the same version skips the forced splash.
                    if (appsVersion != null) CatalogCache.setVersion(context, appsVersion)
                }
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

    /** Refreshes auth token + VPN config without showing splash. Does not overwrite VPN config
     *  if the tunnel is currently running (avoids invalidating in-flight connection). */
    private fun refreshConfigInBackground() {
        viewModelScope.launch {
            val deviceId = DeviceIdHelper.getDeviceId(context)
            when (val result = DeviceAuthRepository.deviceLogin(deviceId)) {
                is DeviceAuthResult.Success -> {
                    DeviceAuthRepository.saveAuth(context, result.auth)
                    if (!IranVpnService.isRunning) {
                        when (val cfg = DeviceAuthRepository.getVpnConfig(context)) {
                            is VpnConfigResult.Success -> DeviceAuthRepository.saveVpnConfig(context, cfg.config)
                            else -> {}
                        }
                    }
                }
                else -> {}
            }
        }
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
