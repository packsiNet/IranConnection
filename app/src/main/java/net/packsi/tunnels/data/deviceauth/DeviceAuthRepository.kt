package net.packsi.tunnels.data.deviceauth

import android.content.Context
import com.google.gson.Gson
import net.packsi.tunnels.data.auth.AuthResponse
import net.packsi.tunnels.data.auth.TokenStore
import net.packsi.tunnels.utils.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object DeviceAuthRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val JSON = "application/json".toMediaType()

    /** Password-less device login. Server identifies device by hashed deviceId only. */
    suspend fun deviceLogin(deviceId: String): DeviceAuthResult =
        withContext(Dispatchers.IO) {
            try {
                val body = gson.toJson(DeviceLoginRequest(deviceId)).toRequestBody(JSON)
                val request = Request.Builder()
                    .url("${AppConstants.BASE_URL}auth/device-login")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                when (response.code) {
                    200 -> {
                        val auth = gson.fromJson(responseBody, AuthResponse::class.java)
                        DeviceAuthResult.Success(auth)
                    }
                    400 -> {
                        val error = runCatching {
                            gson.fromJson(responseBody, DeviceApiError::class.java)
                        }.getOrNull()
                        DeviceAuthResult.ValidationError(
                            error?.errors?.firstOrNull() ?: error?.message ?: "Device ID validation error"
                        )
                    }
                    403 -> DeviceAuthResult.AccountDisabled("Your account has been disabled. Contact support.")
                    else -> DeviceAuthResult.NetworkError("Server error (${response.code}). Please try again.")
                }
            } catch (e: Exception) {
                DeviceAuthResult.NetworkError("Cannot connect to server. Check your internet.")
            }
        }

    /**
     * Persists the device session. Also feeds [TokenStore] so the existing OkHttp interceptor
     * sends this JWT on every authenticated call (subscription, payment, profile…).
     */
    fun saveAuth(context: Context, auth: AuthResponse) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", auth.accessToken)
            .putString("refresh_token", auth.refreshToken)
            .putString("plan", auth.plan)
            .putString("email", auth.email)
            .apply()

        // Sync plan to wireguard prefs for AppsScreen / Premium gating.
        context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)
            .edit()
            .putString("user_plan", auth.plan)
            .apply()

        // App-wide token store used by AuthInterceptor / TokenAuthenticator.
        runCatching {
            TokenStore.init(context)
            TokenStore.saveAuth(auth)
        }
    }

    suspend fun getVpnConfig(context: Context): VpnConfigResult =
        withContext(Dispatchers.IO) {
            try {
                val token = getAccessToken(context)
                    ?: return@withContext VpnConfigResult.Unauthorized

                val request = Request.Builder()
                    .url("${AppConstants.BASE_URL}vpn/config")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                when (response.code) {
                    200 -> VpnConfigResult.Success(
                        gson.fromJson(responseBody, VpnConfigResponse::class.java)
                    )
                    401 -> VpnConfigResult.Unauthorized
                    else -> VpnConfigResult.Error("Failed to load VPN config (${response.code})")
                }
            } catch (e: Exception) {
                VpnConfigResult.Error("Cannot connect to server.")
            }
        }

    suspend fun getAppCatalog(context: Context): Result<List<AppCatalogItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val token = getAccessToken(context) ?: error("No token")
                val request = Request.Builder()
                    .url("${AppConstants.BASE_URL}subscription/apps")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    gson.fromJson(body, Array<AppCatalogItem>::class.java).toList()
                } else {
                    error("HTTP ${response.code}")
                }
            }
        }

    fun saveAppCatalog(context: Context, catalog: List<AppCatalogItem>) {
        context.getSharedPreferences("catalog", Context.MODE_PRIVATE)
            .edit()
            .putString("apps_json", gson.toJson(catalog))
            .apply()
    }

    fun saveVpnConfig(context: Context, config: VpnConfigResponse) {
        context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)
            .edit()
            .putString("endpoint", config.serverEndpoint)
            .putString("server_pub_key", config.serverPublicKey)
            .putString("client_priv_key", config.privateKey)
            .putString("address", config.assignedIp)
            .putString("dns", config.dns)
            .apply()
    }

    fun getAccessToken(context: Context): String? =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("access_token", null)

    fun getPlan(context: Context): String =
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("plan", "Free") ?: "Free"

    fun isLoggedIn(context: Context): Boolean = getAccessToken(context) != null

    fun logout(context: Context) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("wireguard", Context.MODE_PRIVATE)
            .edit()
            .remove("user_plan")
            .remove("endpoint")
            .remove("server_pub_key")
            .remove("client_priv_key")
            .remove("address")
            .remove("dns")
            .apply()
        runCatching { TokenStore.init(context); TokenStore.clear() }
    }
}
