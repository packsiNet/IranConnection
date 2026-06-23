package com.iranconnection.app.data

import com.google.gson.annotations.SerializedName

data class WireGuardConfig(
    @SerializedName("server_endpoint") val serverEndpoint: String,
    @SerializedName("server_public_key") val serverPublicKey: String,
    @SerializedName("client_private_key") val clientPrivateKey: String,
    @SerializedName("client_address") val clientAddress: String,
    val dns: String,
    val version: String,
    @SerializedName("iranian_apps") val iranianApps: List<String>? = null,
)
