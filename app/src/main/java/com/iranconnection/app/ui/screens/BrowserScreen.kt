package com.iranconnection.app.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iranconnection.app.data.browser.BrowserViewModel
import com.iranconnection.app.ui.browser.BrowserAddressBar
import com.iranconnection.app.ui.browser.BrowserWebView
import com.iranconnection.app.ui.browser.TabGridScreen

@Composable
fun BrowserScreen(viewModel: BrowserViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    // One live WebView per tab id; used to drive back/forward/refresh/stop.
    val webViewRefs = remember { mutableStateMapOf<String, WebView>() }
    val activeWebView = webViewRefs[state.activeTab.id]

    // Hardware back: navigate within the active tab when possible.
    BackHandler(enabled = state.activeTab.canGoBack && !state.showTabGrid) {
        activeWebView?.goBack()
    }
    // Hardware back closes the tab grid overlay.
    BackHandler(enabled = state.showTabGrid) {
        viewModel.toggleTabGrid()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                // WebView stack — all tabs stay alive, but the active tab is always
                // drawn LAST so it sits on top and receives touches. Inactive tabs
                // are hidden (alpha 0) and live underneath, so they can't intercept
                // input meant for the active tab.
                state.tabs.forEachIndexed { index, tab ->
                    if (index != state.activeTabIndex) {
                        key(tab.id) {
                            TabWebView(
                                tab = tab,
                                isActive = false,
                                viewModel = viewModel,
                                webViewRefs = webViewRefs,
                                modifier = Modifier.fillMaxSize().alpha(0f),
                            )
                        }
                    }
                }
                state.tabs.getOrNull(state.activeTabIndex)?.let { tab ->
                    key(tab.id) {
                        TabWebView(
                            tab = tab,
                            isActive = true,
                            viewModel = viewModel,
                            webViewRefs = webViewRefs,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // New-tab landing page on top of the (empty) active WebView.
                if (state.activeTab.url.isEmpty()) {
                    NewTabScreen(onLoadUrl = { viewModel.loadUrl(it) })
                }
            }

            // Address bar pinned at the bottom.
            BrowserAddressBar(
                url = state.addressBarText,
                tabCount = state.tabCount,
                isLoading = state.activeTab.isLoading,
                canGoBack = state.activeTab.canGoBack,
                canGoForward = state.activeTab.canGoForward,
                progress = state.activeTab.progress,
                onLoadUrl = { viewModel.loadUrl(it) },
                onGoBack = { webViewRefs[state.activeTab.id]?.goBack() },
                onGoForward = { webViewRefs[state.activeTab.id]?.goForward() },
                onRefresh = { webViewRefs[state.activeTab.id]?.reload() },
                onStop = { webViewRefs[state.activeTab.id]?.stopLoading() },
                onShowTabs = { viewModel.toggleTabGrid() },
                onNewTab = { viewModel.addTab() },
            )
        }

        // Tab grid overlay.
        if (state.showTabGrid) {
            TabGridScreen(
                tabs = state.tabs,
                activeTabIndex = state.activeTabIndex,
                onSelectTab = { viewModel.selectTab(it) },
                onCloseTab = { viewModel.closeTab(it) },
                onAddTab = { viewModel.addTab() },
                onDismiss = { viewModel.toggleTabGrid() },
            )
        }
    }
}

/** Single tab's WebView wired to the [BrowserViewModel] callbacks and ref map. */
@Composable
private fun TabWebView(
    tab: com.iranconnection.app.data.browser.BrowserTab,
    isActive: Boolean,
    viewModel: BrowserViewModel,
    webViewRefs: androidx.compose.runtime.snapshots.SnapshotStateMap<String, WebView>,
    modifier: Modifier = Modifier,
) {
    BrowserWebView(
        tab = tab,
        isActive = isActive,
        onPageStarted = { url -> viewModel.onPageStarted(tab.id, url) },
        onPageFinished = { url, title -> viewModel.onPageFinished(tab.id, url, title) },
        onProgressChanged = { p -> viewModel.onProgressChanged(tab.id, p) },
        onNavigationChanged = { back, forward ->
            viewModel.onNavigationStateChanged(tab.id, back, forward)
        },
        onWebViewCreated = { wv -> webViewRefs[tab.id] = wv },
        onWebViewDisposed = { webViewRefs.remove(tab.id) },
        modifier = modifier,
    )
}

@Composable
fun NewTabScreen(onLoadUrl: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "new tab",
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1A1A1A),
        )
        Box(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter an address or search term.",
            fontSize = 14.sp,
            color = Color(0xFF888888),
        )

        Box(modifier = Modifier.height(32.dp))
        Text(
            text = "سایت‌های پرکاربرد",
            fontSize = 13.sp,
            color = Color(0xFF888888),
        )
        Box(modifier = Modifier.height(12.dp))

        val quickLinks = listOf(
            "دیجی‌کالا" to "https://digikala.com",
            "دیوار" to "https://divar.ir",
            "شیپور" to "https://sheypoor.com",
            "ترب" to "https://torob.com",
            "ایمیل" to "https://mail.google.com",
            "یوتیوب" to "https://youtube.com",
            "ویکی‌پدیا" to "https://fa.wikipedia.org",
            "گوگل" to "https://google.com",
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(quickLinks) { (name, url) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onLoadUrl(url) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(0.5.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = name.first().toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1A73E8),
                        )
                    }
                    Box(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontSize = 10.sp,
                        color = Color(0xFF666666),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
