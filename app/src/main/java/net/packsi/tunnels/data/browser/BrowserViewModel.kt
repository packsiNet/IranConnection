package net.packsi.tunnels.data.browser

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabIndex: Int = 0,
    val showTabGrid: Boolean = false,
    val addressBarText: String = "",
) {
    val activeTab: BrowserTab
        get() = tabs.getOrElse(activeTabIndex) { BrowserTab() }
    val tabCount: Int get() = tabs.size
}

class BrowserViewModel : ViewModel() {
    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    // ---- Tab management ----

    fun addTab(url: String = "") {
        val newTab = BrowserTab(url = url, title = "New tab")
        _state.update { s ->
            s.copy(
                tabs = s.tabs + newTab,
                activeTabIndex = s.tabs.size,
                showTabGrid = false,
                addressBarText = url,
            )
        }
    }

    fun closeTab(tabId: String) {
        val tabs = _state.value.tabs
        if (tabs.size == 1) {
            // Last tab — reset instead of close.
            _state.update { s ->
                s.copy(
                    tabs = listOf(BrowserTab()),
                    activeTabIndex = 0,
                    addressBarText = "",
                )
            }
            return
        }
        val idx = tabs.indexOfFirst { it.id == tabId }
        val newTabs = tabs.filter { it.id != tabId }
        val activeIdx = _state.value.activeTabIndex
        // Keep the currently-active tab selected where possible.
        val newIdx = when {
            idx < 0 -> activeIdx.coerceIn(0, newTabs.size - 1)
            idx < activeIdx -> activeIdx - 1
            idx == activeIdx -> idx.coerceAtMost(newTabs.size - 1)
            else -> activeIdx
        }
        _state.update { s ->
            s.copy(
                tabs = newTabs,
                activeTabIndex = newIdx,
                showTabGrid = false,
                addressBarText = newTabs[newIdx].url,
            )
        }
    }

    fun selectTab(tabId: String) {
        val idx = _state.value.tabs.indexOfFirst { it.id == tabId }
        if (idx >= 0) {
            _state.update { s ->
                s.copy(
                    activeTabIndex = idx,
                    showTabGrid = false,
                    addressBarText = s.tabs[idx].url,
                )
            }
        }
    }

    fun toggleTabGrid() {
        _state.update { s -> s.copy(showTabGrid = !s.showTabGrid) }
    }

    fun updateAddressBar(text: String) {
        _state.update { s -> s.copy(addressBarText = text) }
    }

    fun updateTab(tabId: String, update: (BrowserTab) -> BrowserTab) {
        _state.update { s ->
            val newTabs = s.tabs.map { if (it.id == tabId) update(it) else it }
            val addressBar = if (s.activeTab.id == tabId)
                newTabs[s.activeTabIndex].url
            else s.addressBarText
            s.copy(tabs = newTabs, addressBarText = addressBar)
        }
    }

    fun loadUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val finalUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${Uri.encode(trimmed)}"
        }
        val tabId = _state.value.activeTab.id
        updateTab(tabId) { it.copy(url = finalUrl, isLoading = true) }
        _state.update { s -> s.copy(addressBarText = finalUrl) }
    }

    // ---- WebView callbacks ----

    fun onPageStarted(tabId: String, url: String) {
        updateTab(tabId) {
            it.copy(url = url, isLoading = true, progress = 0)
        }
        if (_state.value.activeTab.id == tabId)
            _state.update { s -> s.copy(addressBarText = url) }
    }

    fun onPageFinished(tabId: String, url: String, title: String?) {
        updateTab(tabId) {
            it.copy(
                url = url.ifEmpty { it.url },
                title = title?.takeIf { t -> t.isNotBlank() } ?: it.title,
                isLoading = false,
                progress = 100,
            )
        }
        if (_state.value.activeTab.id == tabId && url.isNotEmpty())
            _state.update { s -> s.copy(addressBarText = url) }
    }

    fun onProgressChanged(tabId: String, progress: Int) {
        updateTab(tabId) {
            it.copy(progress = progress, isLoading = progress < 100)
        }
    }

    fun onNavigationStateChanged(
        tabId: String,
        canGoBack: Boolean,
        canGoForward: Boolean,
    ) {
        updateTab(tabId) {
            it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }
}
