package net.packsi.tunnels.data.subscription

import android.content.Context
import com.google.gson.Gson

/**
 * On-disk cache of the global app catalog. Written once per cold start by the splash flow
 * (DeviceAuthViewModel) and overwritten by the Apps-list refresh button. The Apps screen reads
 * from here on open instead of hitting the network every time.
 */
object CatalogCache {

    private const val PREFS = "catalog"
    private const val KEY = "apps_json"
    private const val KEY_VERSION = "apps_version"
    private val gson = Gson()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, apps: List<CatalogApp>) {
        prefs(context).edit().putString(KEY, gson.toJson(apps)).apply()
    }

    /** Returns the cached catalog, or null when nothing usable is stored yet. */
    fun load(context: Context): List<CatalogApp>? {
        val json = prefs(context).getString(KEY, null) ?: return null
        return runCatching { gson.fromJson(json, Array<CatalogApp>::class.java).toList() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    /** The IranianAppsUpdateVersion (Remote Config) the cached catalog was last fetched for. */
    fun savedVersion(context: Context): String? =
        prefs(context).getString(KEY_VERSION, null)

    fun setVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_VERSION, version).apply()
    }
}
