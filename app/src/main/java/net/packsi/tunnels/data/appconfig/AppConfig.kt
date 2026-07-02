package net.packsi.tunnels.data.appconfig

import retrofit2.Response
import retrofit2.http.GET

/**
 * Remote app configuration, fetched once on startup from GET /api/app/config.
 * Replaces the former Firebase Remote Config source.
 *
 *   adsEnabled              — master ad switch (admin-controlled). Sole source of truth for ads.
 *   version                 — latest app version; feeds the update dialog (major = mandatory).
 *   iranianAppsUpdateVersion — bumped whenever the Iranian-apps catalog changes; a new value
 *                              forces one splash run so the catalog is re-fetched live.
 *   downloadUrl             — optional APK download link for the update dialog. Absent in the
 *                              base contract; tolerated (empty) so the backend can add it later.
 */
data class AppConfig(
    val adsEnabled: Boolean = true,
    val version: String = "",
    val iranianAppsUpdateVersion: String = "",
    val downloadUrl: String = "",
)

interface AppConfigApi {
    @GET("api/app/config")
    suspend fun getConfig(): Response<AppConfig>
}
