package net.packsi.tunnels.data.auth

// ---- Request bodies ----
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String?,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RefreshRequest(
    val token: String,
)

data class LogoutRequest(
    val refreshToken: String,
)

data class VerifyEmailRequest(
    val email: String,
    val code: String,
)

data class ForgotPasswordRequest(
    val email: String,
)

data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String,
)

data class UpdateProfileRequest(
    val fullName: String?,
    val currentPassword: String?,
    val newPassword: String?,
)

// ---- Success responses ----
data class RegisterResponse(
    val userId: String?,
    val email: String?,
    val message: String?,
)

// Login + refresh-token return the same shape.
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String?,
    val email: String?,
    val fullName: String?,
    val plan: String?,
    val isEmailVerified: Boolean = false,
)

data class UserProfile(
    val id: String?,
    val email: String?,
    val fullName: String?,
    val isEmailVerified: Boolean = false,
    val createdAt: String?,
    val lastLoginAt: String?,
    val subscription: Subscription?,
)

data class Subscription(
    val plan: String?,
    val status: String?,
    val expireDate: String?,
    val daysRemaining: Int?,
    val isActive: Boolean = false,
)

// ---- Error body: { "error": "..." } ----
data class ErrorResponse(
    val error: String?,
)
