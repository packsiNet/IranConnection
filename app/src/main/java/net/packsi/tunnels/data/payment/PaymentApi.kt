package net.packsi.tunnels.data.payment

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface PaymentApi {

    /**
     * Card-to-card receipt upload (multipart/form-data). Do not set Content-Type manually —
     * OkHttp adds the boundary. Returns 201 with a JSON *string* body (success message).
     */
    @Multipart
    @POST("api/payment/receipt")
    suspend fun submitReceipt(
        @Part("payerFullName") payerFullName: RequestBody,
        @Part("lastFourDigits") lastFourDigits: RequestBody,
        @Part("durationDays") durationDays: RequestBody,
        @Part("receiptType") receiptType: RequestBody,
        @Part receiptFile: MultipartBody.Part,
    ): Response<String>

    @GET("api/payment/receipts")
    suspend fun getMyReceipts(): Response<List<ReceiptResponse>>
}
