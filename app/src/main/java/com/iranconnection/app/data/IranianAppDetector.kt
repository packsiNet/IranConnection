package com.iranconnection.app.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Detects which Iranian apps from the server-pushed allow-list are actually installed on the device. */
object IranianAppDetector {

    suspend fun detectInstalledIranianApps(context: Context): List<IranianAppInfo> =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)

            // Falls back to the bundled catalog when the server hasn't pushed allowed_packages/
            // free_packages yet, so the screen isn't empty on a fresh install.
            val allowedPackages = prefs.getString("allowed_packages", null)
                ?.splitToPackageSet()
                ?: IranianAppList.packageNames.toSet()
            val freePackages = prefs.getString("free_packages", null)
                ?.splitToPackageSet()
                ?: IranianAppList.FREE_PACKAGES

            if (allowedPackages.isEmpty()) return@withContext emptyList()

            val pm = context.packageManager
            val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            installed
                .filter { it.packageName in allowedPackages }
                .map { app ->
                    IranianAppInfo(
                        packageName = app.packageName,
                        appName = pm.getApplicationLabel(app).toString(),
                        icon = pm.getApplicationIcon(app.packageName),
                        isFree = app.packageName in freePackages,
                    )
                }
                .sortedWith(compareBy({ !it.isFree }, { it.appName.lowercase() }))
        }

    private fun String.splitToPackageSet(): Set<String> =
        split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
}
