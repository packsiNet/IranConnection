package com.iranconnection.app.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

/**
 * Produces a stable, SHA-256 hashed device identifier. The raw ANDROID_ID is never sent.
 * Same device → same hash → same backend account (even after reinstall).
 */
object DeviceIdHelper {

    fun getDeviceId(context: Context): String {
        val rawId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        ) ?: fallbackId()
        return hashDeviceId(rawId)
    }

    @Suppress("DEPRECATION", "HardwareIds")
    private fun fallbackId(): String =
        runCatching { Build.SERIAL }.getOrNull()?.takeIf { it.isNotBlank() && it != "unknown" }
            ?: UUID.randomUUID().toString()

    private fun hashDeviceId(deviceId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(deviceId.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
