package net.packsi.tunnels.data.payment

import net.packsi.tunnels.data.auth.ApiClient
import net.packsi.tunnels.data.auth.ErrorResponse
import net.packsi.tunnels.data.subscription.HttpFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response

object PaymentRepository {

    private val TEXT = "text/plain".toMediaTypeOrNull()

    /** Returns the server's success message string on 201. 429 → HttpFailure(429, ...). */
    suspend fun submitReceipt(
        payerFullName: String,
        lastFourDigits: String,
        durationDays: Int,
        file: ReceiptFile,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val filePart = MultipartBody.Part.createFormData(
                name = "receiptFile",
                filename = file.fileName,
                body = file.bytes.toRequestBody(file.mimeType.toMediaTypeOrNull()),
            )
            val resp = ApiClient.paymentApi.submitReceipt(
                payerFullName = payerFullName.toRequestBody(TEXT),
                lastFourDigits = lastFourDigits.toRequestBody(TEXT),
                durationDays = durationDays.toString().toRequestBody(TEXT),
                receiptFile = filePart,
            )
            if (resp.isSuccessful) {
                Result.success(resp.body() ?: "Payment receipt submitted successfully")
            } else {
                Result.failure(HttpFailure(resp.code(), parseError(resp)))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to submit receipt"))
        }
    }

    suspend fun getMyReceipts(): Result<List<ReceiptResponse>> = withContext(Dispatchers.IO) {
        try {
            val resp = ApiClient.paymentApi.getMyReceipts()
            if (resp.isSuccessful) {
                Result.success(resp.body().orEmpty())
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
            400 -> "Invalid information"
            401 -> "Please sign in first"
            404 -> "User not found"
            429 -> "You already have 3 receipts pending review"
            else -> "Server error (${resp.code()})"
        }
    }
}
