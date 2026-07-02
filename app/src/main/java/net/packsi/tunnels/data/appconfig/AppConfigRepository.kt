package net.packsi.tunnels.data.appconfig

import net.packsi.tunnels.data.auth.ApiClient

/**
 * Single fetch point for [AppConfig]. The last successful result is kept in memory so the two
 * startup callers — the splash flow (DeviceAuthViewModel) and the update check (UpdateManager) —
 * share one network round-trip instead of hitting /api/app/config twice.
 */
object AppConfigRepository {

    @Volatile
    private var cached: AppConfig? = null

    /** Forces a network fetch. Returns the fresh config, or the last cached one on failure. */
    suspend fun fetch(): AppConfig? =
        try {
            val resp = ApiClient.appConfigApi.getConfig()
            if (resp.isSuccessful) resp.body()?.also { cached = it } else cached
        } catch (e: Exception) {
            android.util.Log.e("AppConfigRepository", "app config fetch failed", e)
            cached
        }

    /** Returns the in-memory config if the startup fetch already ran, otherwise fetches once. */
    suspend fun getOrFetch(): AppConfig? = cached ?: fetch()
}
