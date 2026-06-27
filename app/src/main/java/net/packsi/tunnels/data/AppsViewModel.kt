package net.packsi.tunnels.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.packsi.tunnels.data.subscription.CatalogCache
import net.packsi.tunnels.data.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppsUiState(
    val isLoading: Boolean = true,
    val freeApps: List<IranianAppInfo> = emptyList(),
    val premiumApps: List<IranianAppInfo> = emptyList(),
    val enabledPackages: Set<String> = emptySet(),
    val isPremium: Boolean = false,
    /** Set when the catalog/subscription fetch failed and we fell back to the bundled list. */
    val error: String? = null,
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val enabledPrefs = app.getSharedPreferences("enabled_apps", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init {
        loadApps()
    }

    /**
     * @param forceRefresh true = bypass the cache and hit the network (the Apps-list refresh button),
     *        rewriting the cache. false = cache-first: the catalog the splash flow already fetched is
     *        reused, and the network is only touched when nothing is cached yet (e.g. first run).
     */
    fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val ctx = getApplication<Application>()

            // 1) Global app catalog — cache-first. Network on refresh or when the cache is empty;
            //    fall back to the bundled list so the screen is never empty offline.
            val cached = if (forceRefresh) null else CatalogCache.load(ctx)
            var error: String? = null
            val catalog = if (cached != null) {
                cached
            } else {
                val fresh = SubscriptionRepository.getAppCatalog().getOrNull()?.takeIf { it.isNotEmpty() }
                if (fresh != null) {
                    CatalogCache.save(ctx, fresh)
                    fresh
                } else {
                    error = "Failed to load app list; showing local list"
                    CatalogCache.load(ctx) ?: IranianAppList.asCatalog()
                }
            }

            // 2) User plan decides whether Premium apps are locked — always live, must be accurate.
            val plan = SubscriptionRepository.getSubscription().getOrNull()?.plan
            val isPremium = plan == "Premium" || plan == "Admin"

            // 3) Keep only catalog apps installed on the device.
            val detected = IranianAppDetector.detectInstalledIranianApps(ctx, catalog)

            // Before the user touches a toggle, everything detected starts enabled.
            val savedEnabled = if (enabledPrefs.contains("enabled_apps")) {
                enabledPrefs.getString("enabled_apps", "")
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            } else {
                val defaultEnabled = detected.map { it.packageName }.toSet()
                enabledPrefs.edit().putString("enabled_apps", defaultEnabled.joinToString(",")).apply()
                defaultEnabled
            }

            _state.value = _state.value.copy(
                isLoading = false,
                freeApps = detected.filter { it.isFree },
                premiumApps = detected.filter { !it.isFree },
                enabledPackages = savedEnabled,
                isPremium = isPremium,
                error = error,
            )
        }
    }

    fun toggleApp(packageName: String) {
        val next = _state.value.enabledPackages.let {
            if (packageName in it) it - packageName else it + packageName
        }
        _state.value = _state.value.copy(enabledPackages = next)
        enabledPrefs.edit().putString("enabled_apps", next.joinToString(",")).apply()
    }
}
