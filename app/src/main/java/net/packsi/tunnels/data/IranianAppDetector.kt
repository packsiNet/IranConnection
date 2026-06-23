package com.iranconnection.app.data

import android.content.Context
import android.content.pm.PackageManager
import com.iranconnection.app.data.subscription.CatalogApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Matches the global app catalog (from GET /api/subscription/apps) against the apps
 * actually installed on the device. Only catalog entries that are installed are returned.
 * `isFree` comes from the catalog, not from the device.
 */
object IranianAppDetector {

    suspend fun detectInstalledIranianApps(
        context: Context,
        catalog: List<CatalogApp>,
    ): List<IranianAppInfo> = withContext(Dispatchers.IO) {
        if (catalog.isEmpty()) return@withContext emptyList()

        val byPackage = catalog.associateBy { it.packageName }
        val pm = context.packageManager

        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { app ->
                val entry = byPackage[app.packageName] ?: return@mapNotNull null
                IranianAppInfo(
                    packageName = app.packageName,
                    appName = entry.nameEn?.ifBlank { null }
                        ?: entry.nameFa?.ifBlank { null }
                        ?: pm.getApplicationLabel(app).toString(),
                    nameEn = entry.nameEn.orEmpty(),
                    icon = pm.getApplicationIcon(app.packageName),
                    isFree = entry.isFree,
                )
            }
            .sortedWith(compareBy({ !it.isFree }, { it.appName.lowercase() }))
    }
}
