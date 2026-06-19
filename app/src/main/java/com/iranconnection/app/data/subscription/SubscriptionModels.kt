package com.iranconnection.app.data.subscription

/**
 * GET /api/subscription — per-user subscription status only (no app list).
 * plan: Free | Premium | Admin. status: e.g. Active. Dates are ISO/UTC.
 */
data class SubscriptionResponse(
    val plan: String?,
    val status: String?,
    val startDate: String?,
    val expireDate: String?,
    val daysRemaining: Int?,
    val isActive: Boolean = false,
)

/**
 * One entry of GET /api/subscription/apps — a global catalog shared by all users.
 * isFree == true → available on Free plan; false → Premium only.
 */
data class CatalogApp(
    val packageName: String,
    val nameEn: String?,
    val nameFa: String?,
    val isFree: Boolean = false,
)

/** Carries the HTTP status code so callers can special-case (e.g. 404 → "no subscription"). */
class HttpFailure(val code: Int, message: String) : Exception(message)
