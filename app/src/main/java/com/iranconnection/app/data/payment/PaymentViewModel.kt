package com.iranconnection.app.data.payment

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.iranconnection.app.data.subscription.HttpFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PickedReceipt(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
)

data class PaymentUiState(
    val submitLoading: Boolean = false,
    val submitError: String? = null,
    val successMessage: String? = null,
    /** True when the server returned 429 (3 pending receipts) — submit must stay disabled. */
    val tooManyPending: Boolean = false,
    val receipts: List<ReceiptResponse> = emptyList(),
    val receiptsLoading: Boolean = false,
    val receiptsError: String? = null,
)

class PaymentViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    companion object {
        const val MAX_FILE_BYTES = 5 * 1024 * 1024L
        val ALLOWED_MIME = setOf("image/jpeg", "image/png", "application/pdf")
    }

    /** Reads display name, mime and size for a picked Uri (no full read yet). */
    fun inspectFile(uri: Uri): PickedReceipt {
        val cr = getApplication<Application>().contentResolver
        var name = "receipt"
        var size = -1L
        runCatching {
            cr.query(uri, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) c.getString(nameIdx)?.let { name = it }
                    if (sizeIdx >= 0 && !c.isNull(sizeIdx)) size = c.getLong(sizeIdx)
                }
            }
        }
        val mime = cr.getType(uri) ?: "application/octet-stream"
        return PickedReceipt(uri, name, mime, size)
    }

    /** Client-side file rule check. Returns Persian error or null when valid. */
    fun validateFile(file: PickedReceipt?): String? = when {
        file == null -> "Receipt upload is required"
        file.mimeType !in ALLOWED_MIME -> "File must be JPG, PNG or PDF"
        file.sizeBytes > MAX_FILE_BYTES -> "File must be at most 5MB"
        else -> null   // size < 0 == unknown; guarded again after the byte read
    }

    fun clearMessages() {
        _state.value = _state.value.copy(submitError = null, successMessage = null)
    }

    fun submit(payerFullName: String, lastFourDigits: String, durationDays: Int, file: PickedReceipt) {
        if (_state.value.submitLoading) return
        _state.value = _state.value.copy(submitLoading = true, submitError = null, successMessage = null)
        viewModelScope.launch {
            // Read bytes off the main thread, with a hard size guard.
            val bytesResult = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(file.uri)
                        ?.use { it.readBytes() } ?: error("empty")
                }
            }
            val bytes = bytesResult.getOrNull()
            if (bytes == null) {
                _state.value = _state.value.copy(submitLoading = false, submitError = "Failed to read file")
                return@launch
            }
            if (bytes.size > MAX_FILE_BYTES) {
                _state.value = _state.value.copy(submitLoading = false, submitError = "File must be at most 5MB")
                return@launch
            }

            PaymentRepository.submitReceipt(
                payerFullName = payerFullName.trim(),
                lastFourDigits = lastFourDigits,
                durationDays = durationDays,
                file = ReceiptFile(file.fileName, file.mimeType, bytes),
            ).fold(
                onSuccess = { msg ->
                    _state.value = _state.value.copy(
                        submitLoading = false,
                        successMessage = msg,
                        tooManyPending = false,
                    )
                    loadReceipts()
                },
                onFailure = { e ->
                    val tooMany = (e as? HttpFailure)?.code == 429
                    _state.value = _state.value.copy(
                        submitLoading = false,
                        submitError = e.message,
                        tooManyPending = tooMany,
                    )
                },
            )
        }
    }

    fun loadReceipts() {
        _state.value = _state.value.copy(receiptsLoading = true, receiptsError = null)
        viewModelScope.launch {
            PaymentRepository.getMyReceipts().fold(
                onSuccess = { list ->
                    val pending = list.count { it.status.equals("Pending", ignoreCase = true) }
                    _state.value = _state.value.copy(
                        receiptsLoading = false,
                        receipts = list,
                        tooManyPending = pending >= 3,
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(receiptsLoading = false, receiptsError = e.message)
                },
            )
        }
    }
}
