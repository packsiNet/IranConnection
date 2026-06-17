package com.iranconnection.app.data

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.tasks.await

object UpdateManager {

    private const val KEY_VERSION = "version"
    private const val KEY_DOWNLOAD_URL = "download_url"

    suspend fun checkForUpdate(currentVersion: String): UpdateInfo {
        val config = Firebase.remoteConfig
        config.setConfigSettingsAsync(
            remoteConfigSettings { minimumFetchIntervalInSeconds = 0 }
        ).await()
        config.setDefaultsAsync(
            mapOf(KEY_VERSION to "0.0.0", KEY_DOWNLOAD_URL to "")
        ).await()

        return try {
            config.fetchAndActivate().await()
            val remoteVersion = config.getString(KEY_VERSION)
            val downloadUrl = config.getString(KEY_DOWNLOAD_URL)
            if (isNewerVersion(remoteVersion, currentVersion)) {
                UpdateInfo.UpdateAvailable(remoteVersion, downloadUrl)
            } else {
                UpdateInfo.UpToDate
            }
        } catch (e: Exception) {
            android.util.Log.e("UpdateManager", "Remote Config fetch failed", e)
            UpdateInfo.UpToDate
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteParts = remote.trim().split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.trim().split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}

sealed class UpdateInfo {
    object UpToDate : UpdateInfo()
    data class UpdateAvailable(val newVersion: String, val downloadUrl: String) : UpdateInfo()
}
