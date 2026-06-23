package net.packsi.tunnels.data.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApi {
    @POST("api/auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/refresh-token")
    suspend fun refresh(@Body body: RefreshRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): Response<Unit>

    /** Verify email with 6-digit code. Public. Returns a JSON string message. */
    @POST("api/auth/verify-email")
    suspend fun verifyEmail(@Body body: VerifyEmailRequest): Response<String>

    /** Always 200 (does not reveal whether the email exists). JSON string message. */
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Response<String>

    /** Reset password with code. JSON string message. Invalidates all refresh tokens. */
    @POST("api/auth/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): Response<String>
}

interface UserApi {
    @GET("api/user/profile")
    suspend fun profile(): Response<UserProfile>

    /** Re-send the verification code. Requires auth. */
    @POST("api/user/resend-verification")
    suspend fun resendVerification(): Response<String>

    /** Update full name and/or password. Returns the full updated profile. */
    @PUT("api/user/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<UserProfile>
}
