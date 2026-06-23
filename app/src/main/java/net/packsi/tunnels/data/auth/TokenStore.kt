package net.packsi.tunnels.data.auth

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Token + cached identity store. Backed by SharedPreferences so the OkHttp
 * interceptor/authenticator can read the access token synchronously off the
 * network thread. Exposes [loggedIn] as a reactive flow for the UI layer.
 */
object TokenStore {
    private lateinit var prefs: SharedPreferences

    private const val K_ACCESS = "access_token"
    private const val K_REFRESH = "refresh_token"
    private const val K_EXPIRES = "expires_at"
    private const val K_EMAIL = "email"
    private const val K_FULL_NAME = "full_name"
    private const val K_PLAN = "plan"

    private val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("ic_auth_tokens", Context.MODE_PRIVATE)
        _loggedIn.value = accessToken != null
    }

    val accessToken: String? get() = prefs.getString(K_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(K_REFRESH, null)
    val email: String get() = prefs.getString(K_EMAIL, "") ?: ""
    val fullName: String get() = prefs.getString(K_FULL_NAME, "") ?: ""
    val plan: String get() = prefs.getString(K_PLAN, "") ?: ""

    /** Persist tokens + cached identity from a login/refresh response. */
    fun saveAuth(r: AuthResponse) {
        prefs.edit().apply {
            putString(K_ACCESS, r.accessToken)
            putString(K_REFRESH, r.refreshToken)
            putString(K_EXPIRES, r.expiresAt)
            r.email?.let { putString(K_EMAIL, it) }
            r.fullName?.let { putString(K_FULL_NAME, it) }
            r.plan?.let { putString(K_PLAN, it) }
            apply()
        }
        _loggedIn.value = true
    }

    fun clear() {
        prefs.edit().clear().apply()
        _loggedIn.value = false
    }
}
