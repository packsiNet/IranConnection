package com.iranconnection.app.data.subscription

import com.iranconnection.app.data.auth.ApiClient
import com.iranconnection.app.data.auth.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Orchestration over [ApiClient.subscriptionApi]. Mirrors AuthRepository: every call
 * returns [Result]; failures carry a Persian message from the server's `{ "error": "..." }`
 * body (or a status-code default) and preserve the HTTP code via [HttpFailure].
 */
object SubscriptionRepository {

    suspend fun getSubscription(): Result<SubscriptionResponse> =
        call { ApiClient.subscriptionApi.getSubscription() }

    suspend fun getAppCatalog(): Result<List<CatalogApp>> =
        call { ApiClient.subscriptionApi.getAppCatalog() }

    private suspend fun <T> call(block: suspend () -> Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            try {
                val resp = block()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    if (body != null) Result.success(body)
                    else Result.failure(Exception("Empty response from server"))
                } else {
                    Result.failure(HttpFailure(resp.code(), parseError(resp)))
                }
            } catch (e: Exception) {
                Result.failure(Exception("Error communicating with the server"))
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
            401 -> "Please sign in first"
            404 -> "You have no subscription"
            else -> "Server error (${resp.code()})"
        }
    }
}
