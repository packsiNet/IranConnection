package net.packsi.tunnels.data.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Thin orchestration over [ApiClient]. Every call returns [Result]; failures
 * carry a Persian message taken from the server's `{ "error": "..." }` body,
 * falling back to a status-code-specific default.
 */
object AuthRepository {

    suspend fun register(email: String, password: String, fullName: String?): Result<RegisterResponse> =
        call {
            ApiClient.authApi.register(RegisterRequest(email, password, fullName?.ifBlank { null }))
        }

    /** On success the tokens are persisted in [TokenStore]. */
    suspend fun login(email: String, password: String): Result<AuthResponse> =
        call {
            ApiClient.authApi.login(LoginRequest(email, password))
        }.onSuccess { TokenStore.saveAuth(it) }

    suspend fun profile(): Result<UserProfile> =
        call { ApiClient.userApi.profile() }

    suspend fun verifyEmail(email: String, code: String): Result<String> =
        call { ApiClient.authApi.verifyEmail(VerifyEmailRequest(email.trim(), code)) }

    suspend fun resendVerification(): Result<String> =
        call { ApiClient.userApi.resendVerification() }

    suspend fun forgotPassword(email: String): Result<String> =
        call { ApiClient.authApi.forgotPassword(ForgotPasswordRequest(email.trim())) }

    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String> =
        call { ApiClient.authApi.resetPassword(ResetPasswordRequest(email.trim(), code, newPassword)) }

    /** On success the profile/auth identity should be refreshed from the returned object. */
    suspend fun updateProfile(fullName: String?, currentPassword: String?, newPassword: String?): Result<UserProfile> =
        call {
            ApiClient.userApi.updateProfile(
                UpdateProfileRequest(
                    fullName = fullName?.ifBlank { null },
                    currentPassword = currentPassword?.ifBlank { null },
                    newPassword = newPassword?.ifBlank { null },
                )
            )
        }

    /** Best-effort server logout, then always clear local tokens. */
    suspend fun logout() {
        val refresh = TokenStore.refreshToken
        if (refresh != null) {
            withContext(Dispatchers.IO) {
                try {
                    ApiClient.authApi.logout(LogoutRequest(refresh))
                } catch (e: Exception) {
                    // ignore — clearing locally below is what matters
                }
            }
        }
        TokenStore.clear()
    }

    // ---- helpers ----

    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val resp = block()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) {
                        Result.success(body)
                    } else {
                        // 200/201 with empty body (e.g. logout) — treat Unit-ish as success.
                        @Suppress("UNCHECKED_CAST")
                        Result.success(Unit as T)
                    }
                } else {
                    Result.failure(Exception(parseError(resp)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error communicating with the server."))
            }
        }

    private fun <T> parseError(resp: Response<T>): String {
        val serverMsg = try {
            resp.errorBody()?.string()?.let { raw ->
                ApiClient.gson.fromJson(raw, ErrorResponse::class.java)?.error
            }
        } catch (e: Exception) {
            null
        }
        if (!serverMsg.isNullOrBlank()) return serverMsg

        return when (resp.code()) {
            400 -> "Invalid login"
            401 -> "Incorrect email or password"
            403 -> "Your account is disabled"
            404 -> "User not found"
            409 -> "This email address is already registered"
            else -> "Server error (${resp.code()})"
        }
    }
}
