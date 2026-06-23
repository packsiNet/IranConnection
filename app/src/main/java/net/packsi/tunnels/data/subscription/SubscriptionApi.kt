package com.iranconnection.app.data.subscription

import retrofit2.Response
import retrofit2.http.GET

interface SubscriptionApi {
    /** Per-user subscription status. Requires auth. 404 when the user has no subscription. */
    @GET("api/subscription")
    suspend fun getSubscription(): Response<SubscriptionResponse>

    /** Global app catalog (same for every user). Requires auth. */
    @GET("api/subscription/apps")
    suspend fun getAppCatalog(): Response<List<CatalogApp>>
}
