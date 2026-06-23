package com.iranconnection.app.data.auth

import okhttp3.Interceptor
import okhttp3.Response

/** Injects `Authorization: Bearer <accessToken>` when a token is present. */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenStore.accessToken
        val request = chain.request()
        val authed = if (token.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(authed)
    }
}
