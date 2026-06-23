package net.packsi.tunnels.data.payment

/**
 * One entry of GET /api/payment/receipts.
 * status: Pending | Approved | Rejected. adminNote/reviewedAt filled once reviewed.
 */
data class ReceiptResponse(
    val id: String?,
    val payerFullName: String?,
    val lastFourDigits: String?,
    val status: String?,
    val requestedDurationDays: Int?,
    val submittedAt: String?,
    val adminNote: String?,
    val reviewedAt: String?,
)

/** Local bundle of a picked receipt file, read off the content resolver. */
data class ReceiptFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
)
