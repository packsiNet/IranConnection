package net.packsi.tunnels.data

import net.packsi.tunnels.data.appconfig.AppConfigRepository

object UpdateManager {

    /**
     * Compares the remote [net.packsi.tunnels.data.appconfig.AppConfig.version] (from
     * GET /api/app/config) against the installed version and reports whether an update is
     * available. A MAJOR bump is mandatory. Reads the shared in-memory config so no extra network
     * call is made when the splash flow already fetched it.
     */
    suspend fun checkForUpdate(currentVersion: String): UpdateInfo {
        val config = AppConfigRepository.getOrFetch() ?: return UpdateInfo.UpToDate
        val remoteVersion = config.version
        if (remoteVersion.isBlank()) return UpdateInfo.UpToDate

        return if (isNewerVersion(remoteVersion, currentVersion)) {
            UpdateInfo.UpdateAvailable(
                newVersion = remoteVersion,
                downloadUrl = config.downloadUrl,
                isMajor = isMajor(remoteVersion, currentVersion),
            )
        } else {
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

    /**
     * A MAJOR bump (the first version component increased, e.g. 1.x.x → 2.x.x) is mandatory:
     * the user can't keep using the app without updating. A minor/patch bump is optional.
     */
    private fun isMajor(remote: String, current: String): Boolean {
        val r = remote.trim().split(".").firstOrNull()?.toIntOrNull() ?: 0
        val c = current.trim().split(".").firstOrNull()?.toIntOrNull() ?: 0
        return r > c
    }
}

sealed class UpdateInfo {
    object UpToDate : UpdateInfo()
    data class UpdateAvailable(
        val newVersion: String,
        val downloadUrl: String,
        val isMajor: Boolean,
    ) : UpdateInfo()
}
