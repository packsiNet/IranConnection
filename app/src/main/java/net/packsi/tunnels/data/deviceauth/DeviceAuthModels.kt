package net.packsi.tunnels.data.deviceauth

import net.packsi.tunnels.data.auth.AuthResponse

data class DeviceLoginRequest(
    val deviceId: String,
)

data class DeviceApiError(
    val message: String? = null,
    val errors: List<String>? = null,
)

/** Reuses the shared [AuthResponse] so a device login can also populate the app-wide token store. */
sealed class DeviceAuthResult {
    data class Success(val auth: AuthResponse) : DeviceAuthResult()
    data class WrongPassword(val message: String) : DeviceAuthResult()
    data class AccountDisabled(val message: String) : DeviceAuthResult()
    data class ValidationError(val message: String) : DeviceAuthResult()
    data class NetworkError(val message: String) : DeviceAuthResult()
}

data class VpnConfigResponse(
    val privateKey: String,
    val assignedIp: String,
    val serverPublicKey: String,
    val serverEndpoint: String,
    val dns: String,
)

sealed class VpnConfigResult {
    data class Success(val config: VpnConfigResponse) : VpnConfigResult()
    object Unauthorized : VpnConfigResult()
    data class Error(val message: String) : VpnConfigResult()
}

data class AppCatalogItem(
    val packageName: String,
    val nameEn: String,
    val nameFa: String,
    val isFree: Boolean,
)
