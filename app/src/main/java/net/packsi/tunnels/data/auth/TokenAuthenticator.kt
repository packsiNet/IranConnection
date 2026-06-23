package com.iranconnection.app.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Handles 401 responses: calls refresh-token once, updates the stored tokens,
 * and replays the original request with the new access token. If refresh fails
 * the tokens are cleared (forcing a logout) and the request is abandoned.
 *
 * [refreshApi] must be built on a client WITHOUT this authenticator, otherwise
 * a failing refresh would recurse.
 */
class TokenAuthenticator(private val refreshApi: AuthApi) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Already retried once (prior-response chain non-empty) -> give up.
        if (response.priorResponse != null) return null

        val refresh = TokenStore.refreshToken ?: return null

        val result = runBlocking {
            try {
                refreshApi.refresh(RefreshRequest(refresh))
            } catch (e: Exception) {
                null
            }
        }

        val body = result?.takeIf { it.isSuccessful }?.body()
        if (body == null) {
            TokenStore.clear()
            return null
        }

        TokenStore.saveAuth(body)
        return response.request.newBuilder()
            .header("Authorization", "Bearer ${body.accessToken}")
            .build()
    }
}
