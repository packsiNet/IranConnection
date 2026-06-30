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
    // AmneziaWG obfuscation, sent NESTED under "obfuscation" by /vpn/config. Null → plain-WG
    // server. MUST match the server's [Interface] exactly or the handshake never completes
    // (server drops the un-obfuscated 148-byte init).
    val obfuscation: Obfuscation? = null,
)

/** AmneziaWG [Interface] obfuscation params. H1-H4 are uint32 → Long (h2 can exceed Int.MAX). */
data class Obfuscation(
    val jc: Int? = null,    // Jc   junk packet count
    val jmin: Int? = null,  // Jmin junk packet min size
    val jmax: Int? = null,  // Jmax junk packet max size
    val s1: Int? = null,    // S1   init packet junk size
    val s2: Int? = null,    // S2   response packet junk size
    val h1: Long? = null,   // H1   init packet magic header
    val h2: Long? = null,   // H2   response packet magic header
    val h3: Long? = null,   // H3   underload packet magic header
    val h4: Long? = null,   // H4   transport packet magic header
)

sealed class VpnConfigResult {
    data class Success(val config: VpnConfigResponse) : VpnConfigResult()
    object Unauthorized : VpnConfigResult()
    data class Error(val message: String) : VpnConfigResult()
}
