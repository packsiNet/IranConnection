package com.iranconnection.app.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

    Scaffold(
        topBar = {
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
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App intro / self-promo card
        IntroCard()

        Box(modifier = Modifier.height(28.dp))
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
        // Manual 4-column grid (non-lazy) so it nests safely inside the scroll column.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            quickLinks.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { (name, url) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onLoadUrl(url) },
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
                    // Pad short final row to keep columns aligned.
                    repeat(4 - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

// ---- App intro / self-promo card (shown on empty new tab) ----
@Composable
private fun IntroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3DBFBA), Color(0xFF279491), Color(0xFF195E5C)),
                    start = Offset(0f, 0f), end = Offset(600f, 400f),
                )
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("IR", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Column {
                Text("IranConnection",
                    fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("مرورگر امن داخل اپ",
                    fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 1.dp))
            }
        }

        Box(modifier = Modifier.height(12.dp))

        Text(
            text = "دسترسی امن و آزاد به اینترنت برای همه‌ی کاربران خارج از ایران. " +
                "سریع، امن و بدون محدودیت — راهکاری مطمئن برای اتصال به دنیا، هر کجا که باشید.",
            fontSize = 12.sp,
            lineHeight = 19.sp,
            color = Color.White.copy(alpha = 0.92f),
        )

        Box(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("بدون محدودیت", "امن", "سریع").forEach { chip ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(chip, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
