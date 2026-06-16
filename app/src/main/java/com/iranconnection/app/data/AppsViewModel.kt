package com.iranconnection.app.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppsUiState(
    val isLoading: Boolean = true,
    val freeApps: List<IranianAppInfo> = emptyList(),
    val premiumApps: List<IranianAppInfo> = emptyList(),
    val enabledPackages: Set<String> = emptySet(),
)

class AppsViewModel(app: Application) : AndroidViewModel(app) {

    private val enabledPrefs = app.getSharedPreferences("enabled_apps", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val detected = IranianAppDetector.detectInstalledIranianApps(getApplication())
            // Before the user has ever touched a toggle, everything detected starts enabled
            // (matches the previous app_enabled_* default) instead of routing nothing.
            val savedEnabled = if (enabledPrefs.contains("enabled_apps")) {
                enabledPrefs.getString("enabled_apps", "")
                    .orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            } else {
                detected.map { it.packageName }.toSet()
            }
            _state.value = _state.value.copy(
                isLoading = false,
                freeApps = detected.filter { it.isFree },
                premiumApps = detected.filter { !it.isFree },
                enabledPackages = savedEnabled,
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
