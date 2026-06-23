package com.iranconnection.app.data.browser

import android.graphics.Bitmap
import java.util.UUID

/** A single browser tab's state. */
data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New tab",
    val favicon: Bitmap? = null,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)
