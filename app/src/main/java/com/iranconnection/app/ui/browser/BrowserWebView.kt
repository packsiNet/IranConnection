package com.iranconnection.app.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.iranconnection.app.data.browser.BrowserTab

/**
 * One WebView per tab. All tabs stay alive in the composition; only the active
 * one is visible (alpha trick in [BrowserScreen]). Traffic automatically routes
 * through the active VPN tunnel since the WebView uses the device network stack.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    tab: BrowserTab,
    isActive: Boolean,
    onPageStarted: (url: String) -> Unit,
    onPageFinished: (url: String, title: String?) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onNavigationChanged: (canBack: Boolean, canForward: Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    onWebViewDisposed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tracks the last URL we explicitly told the WebView to load, so the update
    // block doesn't reload on every recomposition (e.g. progress ticks) and
    // doesn't fight redirects reported back via onPageStarted/onPageFinished.
    val lastRequestedUrl = remember { mutableStateOfString() }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    setSupportZoom(true)
                    allowContentAccess = true
                    allowFileAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                isEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        onPageStarted(url ?: "")
                        onNavigationChanged(view.canGoBack(), view.canGoForward())
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        lastRequestedUrl.value = url ?: lastRequestedUrl.value
                        onPageFinished(url ?: "", view.title)
                        onNavigationChanged(view.canGoBack(), view.canGoForward())
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        val target = request.url.toString()
                        lastRequestedUrl.value = target
                        view.loadUrl(target)
                        return true
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        onProgressChanged(newProgress)
                        onNavigationChanged(view.canGoBack(), view.canGoForward())
                    }

                    override fun onReceivedTitle(view: WebView, title: String?) {
                        onPageFinished(view.url ?: "", title)
                    }
                }
                onWebViewCreated(this)
            }
        },
        update = { webView ->
            // Load only when this tab is active and the requested URL actually
            // changed; this drives navigation after BrowserViewModel.loadUrl sets
            // the new URL, without reloading on every recomposition (progress ticks)
            // or fighting redirects reported via onPageStarted/onPageFinished.
            if (isActive && tab.url.isNotEmpty() && tab.url != lastRequestedUrl.value) {
                lastRequestedUrl.value = tab.url
                webView.loadUrl(tab.url)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
            onWebViewDisposed()
        },
        modifier = modifier,
    )
}

// Small helper to keep a non-null mutable String state without importing the
// whole runtime surface at the call site.
private fun mutableStateOfString() =
    androidx.compose.runtime.mutableStateOf("")
