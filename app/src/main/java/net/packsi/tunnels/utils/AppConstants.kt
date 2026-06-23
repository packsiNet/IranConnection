package net.packsi.tunnels.utils

import net.packsi.tunnels.BuildConfig

object AppConstants {
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"

    /**
     * Base URL for the auth API. Derived from the build's BASE_URL so the device-login
     * endpoint stays consistent with the rest of the app (default emulator host).
     * Final device-login URL = BASE_URL + "auth/device-login".
     */
    val BASE_URL: String = BuildConfig.BASE_URL.trimEnd('/') + "/api/"
}
