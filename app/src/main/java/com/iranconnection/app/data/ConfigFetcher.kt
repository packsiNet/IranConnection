package com.iranconnection.app.data

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object ConfigFetcher {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetch(url: String): WireGuardConfig? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null
            gson.fromJson(body, WireGuardConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
