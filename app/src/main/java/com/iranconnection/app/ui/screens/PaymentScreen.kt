package com.iranconnection.app.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iranconnection.app.data.payment.PaymentViewModel
import com.iranconnection.app.data.payment.PickedReceipt
import com.iranconnection.app.data.payment.ReceiptResponse
import kotlinx.coroutines.delay

// ---- Bank card data per currency (destination card + amount live in the frontend, not the API) ----
private data class BankCard(
    val bank: String,
    val number: String,
    val holder: String,
    val gradient: List<Color>,
    val shadow: Color,
    val amount: String,
)

private fun bankCardFor(currency: String): BankCard =
    if (currency == "usd") BankCard(
        bank = "USD Bank",
        number = "4937 2420 2574 6817",
        holder = "LALEH MANSOURI",
        gradient = listOf(Color(0xFFE63950), Color(0xFFB01030), Color(0xFF7A0C24)),
        shadow = Color(0x5CC81838),
        amount = "$2.50 USD",
    ) else BankCard(
        bank = "Iranian Bank",
        number = "6219 8618 0150 9695",
        holder = "SHAHRAM OVEISI",
        gradient = listOf(Color(0xFF2D8FD8), Color(0xFF1868B2), Color(0xFF0C3D78)),
        shadow = Color(0x5C1878C8),
        amount = "300,000 TMN",
    )

// ---- Duration presets (durationDays 1..365) ----
private data class DurationOption(val days: Int, val label: String)
private val DURATIONS = listOf(
    DurationOption(30, "1 Month"),
    DurationOption(90, "3 Months"),
    DurationOption(365, "1 Year"),
)

// ---- Payment screen ----
@Composable
fun PaymentScreen(
    currency: String,
    onBack: () -> Unit,
    onApproved: () -> Unit = {},
    vm: PaymentViewModel = viewModel(),
) {
    val clipboard = LocalClipboardManager.current
    val card = remember(currency) { bankCardFor(currency) }
    val state by vm.state.collectAsState()

    var name by rememberSaveable { mutableStateOf("") }
    var last4 by rememberSaveable { mutableStateOf("") }
    var durationDays by rememberSaveable { mutableStateOf(30) }
    var picked by remember { mutableStateOf<PickedReceipt?>(null) }
    var copied by remember { mutableStateOf(false) }
    var showReceipts by rememberSaveable { mutableStateOf(false) }
    var triedSubmit by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) picked = vm.inspectFile(uri) }

    LaunchedEffect(copied) {
        if (copied) { delay(2200); copied = false }
    }

    // On 201, jump to the receipts list so the user can track the status.
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) showReceipts = true
    }

    // Load + light-poll receipts while the list is open.
    LaunchedEffect(showReceipts) {
        if (showReceipts) {
            vm.loadReceipts()
            while (true) { delay(6000); vm.loadReceipts() }
        }
    }

    // When a receipt flips to Approved, refresh the subscription so the new plan shows.
    var approvedNotified by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.receipts) {
        val anyApproved = state.receipts.any { it.status.equals("Approved", ignoreCase = true) }
        if (anyApproved && !approvedNotified) { approvedNotified = true; onApproved() }
    }

    BackHandler(onBack = { if (showReceipts) showReceipts = false else onBack() })

    if (showReceipts) {
        ReceiptsView(
            receipts = state.receipts,
            loading = state.receiptsLoading,
            error = state.receiptsError,
            successMessage = state.successMessage,
            onBack = { showReceipts = false },
            onRefresh = vm::loadReceipts,
        )
        return
    }

    val fileErr = picked?.let { vm.validateFile(it) }
    val nameValid = name.trim().isNotEmpty() && name.trim().length <= 200
    val last4Valid = last4.length == 4
    val fileValid = picked != null && fileErr == null
    val canSubmit = nameValid && last4Valid && fileValid && durationDays in 1..365 &&
        !state.submitLoading && !state.tooManyPending

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEEF2))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(8.dp, 14.dp)) {
                    val p = Path().apply {
                        moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height)
                    }
                    drawPath(p, Color(0xFF18182A),
                        style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Complete Payment", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF18182A), letterSpacing = (-0.3).sp)
                Text("Monthly Premium · ${card.amount}", fontSize = 10.sp,
                    color = Color(0xFFA0AAB8), modifier = Modifier.padding(top = 1.dp))
            }
            // My receipts
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .clickable { showReceipts = true }
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            ) {
                Text("My Receipts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF279491))
            }
        }

        // Online gateway — disabled
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .alpha(0.52f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF0F2F6)),
                contentAlignment = Alignment.Center
            ) { Canvas(Modifier.size(18.dp)) { drawGatewayIcon() } }
            Column(modifier = Modifier.weight(1f)) {
                Text("Online Payment Gateway", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7A99))
                Text("Temporarily unavailable", fontSize = 10.sp, color = Color(0xFFA0AAB8),
                    modifier = Modifier.padding(top = 1.dp))
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFF0F2F6))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) { Text("SOON", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA0AAB8), letterSpacing = 0.4.sp) }
        }

        // OR PAY MANUALLY divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E5EC)))
            Text("OR PAY MANUALLY", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFA0AAB8))
            Box(modifier = Modifier.weight(1f).height(1.dp).background(Color(0xFFE2E5EC)))
        }

        // Bank card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = card.shadow)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(card.gradient, start = Offset(0f, 0f), end = Offset(500f, 400f)))
                .padding(20.dp)
        ) {
            Box(modifier = Modifier.size(140.dp).align(Alignment.TopEnd).offset(x = 34.dp, y = (-34).dp)
                .clip(CircleShape).border(28.dp, Color.White.copy(alpha = 0.08f), CircleShape))
            Box(modifier = Modifier.size(90.dp).align(Alignment.BottomStart).offset(x = (-14).dp, y = 28.dp)
                .clip(CircleShape).border(18.dp, Color.White.copy(alpha = 0.06f), CircleShape))

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text("TRANSFER TO", fontSize = 9.sp, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp)
                        Text(card.bank, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                            letterSpacing = (-0.2).sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    CardChip()
                }
                Spacer(Modifier.height(20.dp))
                Text(card.number, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color.White,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("CARD HOLDER", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 3.dp))
                        Text(card.holder, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            letterSpacing = 0.3.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                            .clickable {
                                clipboard.setText(AnnotatedString(card.number.replace(" ", "")))
                                copied = true
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(if (copied) "✓ Copied" else "Copy", fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }

        // Amount badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3DBFBA)))
                Text("Amount to transfer", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18182A))
            }
            Text(card.amount, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF18182A),
                letterSpacing = (-0.3).sp)
        }

        // Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("Your payment details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18182A))

            PayField(label = "Full Name", value = name, onChange = { name = it.take(200) },
                placeholder = "e.g. John Smith", keyboardType = KeyboardType.Text)
            if (triedSubmit && !nameValid) FieldHint("Payer name is required (max 200 characters)")

            PayField(label = "Last 4 digits of your card", value = last4,
                onChange = { last4 = it.filter(Char::isDigit).take(4) },
                placeholder = "XXXX", keyboardType = KeyboardType.Number, mono = true)
            if (triedSubmit && !last4Valid) FieldHint("Enter the last 4 digits of the card")

            // Subscription duration
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Subscription duration", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8A96A8), letterSpacing = 0.3.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DURATIONS.forEach { opt ->
                        DurationChip(
                            label = opt.label,
                            active = durationDays == opt.days,
                            modifier = Modifier.weight(1f),
                            onClick = { durationDays = opt.days },
                        )
                    }
                }
            }

            // Upload receipt
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Upload Receipt", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8A96A8), letterSpacing = 0.3.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFF6F7FA))
                        .border(
                            1.5.dp,
                            if (fileErr != null) Color(0xFFEF4444) else Color(0xFFD0D6E2),
                            RoundedCornerShape(11.dp),
                        )
                        .clickable { picker.launch("*/*") }
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFEDEEF2)),
                        contentAlignment = Alignment.Center
                    ) { Canvas(Modifier.size(16.dp)) { drawUploadIcon() } }
                    Text(picked?.fileName ?: "Tap to upload receipt", fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, color = Color(0xFF8A96A8),
                        maxLines = 1)
                    Text("JPG, PNG or PDF · max 5MB", fontSize = 9.5.sp, color = Color(0xFFB0BAC8))
                }
                if (fileErr != null) FieldHint(fileErr)
            }

            if (state.tooManyPending) {
                NoticeBanner("You already have 3 receipts pending review. You can't submit a new one until they are resolved.",
                    Color(0xFFEF4444))
            }
            state.submitError?.let { if (!state.tooManyPending) NoticeBanner(it, Color(0xFFEF4444)) }

            // Submit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (canSubmit) Modifier.shadow(12.dp, RoundedCornerShape(12.dp), spotColor = Color(0xFF279491)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (canSubmit) Brush.linearGradient(listOf(Color(0xFF4ECAC5), Color(0xFF279491)))
                        else Brush.linearGradient(listOf(Color(0xFFE8EAF0), Color(0xFFE8EAF0)))
                    )
                    .clickable(enabled = !state.submitLoading && !state.tooManyPending) {
                        triedSubmit = true
                        val p = picked
                        if (nameValid && last4Valid && p != null && fileErr == null) {
                            vm.submit(name, last4, durationDays, p)
                        }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.submitLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Payment Info",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (canSubmit) Color.White else Color(0xFFA0AAB8))
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

// ---- Receipts list (status tracking) ----
@Composable
private fun ReceiptsView(
    receipts: List<ReceiptResponse>,
    loading: Boolean,
    error: String?,
    successMessage: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEEF2))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                    .background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(8.dp, 14.dp)) {
                    val p = Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height) }
                    drawPath(p, Color(0xFF18182A), style = Stroke(3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Text("My Receipts", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF18182A), modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .clickable(enabled = !loading) { onRefresh() }.padding(horizontal = 11.dp, vertical = 7.dp),
            ) { Text(if (loading) "..." else "↻ Refresh", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF279491)) }
        }

        successMessage?.let { NoticeBanner(it, Color(0xFF279491)) }
        error?.let { NoticeBanner(it, Color(0xFFEF4444)) }

        when {
            loading && receipts.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF279491))
                }
            receipts.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text("You have not submitted any receipts yet", fontSize = 12.sp, color = Color(0xFFA0AAB8))
                }
            else -> receipts.forEach { ReceiptCard(it) }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ReceiptCard(r: ReceiptResponse) {
    val (chipColor, chipText) = when (r.status?.lowercase()) {
        "approved" -> Color(0xFF10B981) to "Approved"
        "rejected" -> Color(0xFFEF4444) to "Rejected"
        else       -> Color(0xFFF59E0B) to "Pending review"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${r.requestedDurationDays ?: 30} days", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18182A))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(chipColor.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(chipText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = chipColor) }
        }
        ReceiptRow("Card", "**** ${r.lastFourDigits ?: "----"}")
        ReceiptRow("Name", r.payerFullName ?: "—")
        ReceiptRow("Submitted", r.submittedAt?.substringBefore('T') ?: "—")
        r.reviewedAt?.let { ReceiptRow("Reviewed", it.substringBefore('T')) }
        if (!r.adminNote.isNullOrBlank()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                    .background(chipColor.copy(alpha = 0.10f)).padding(horizontal = 10.dp, vertical = 8.dp)
            ) { Text("Note: ${r.adminNote}", fontSize = 10.5.sp, color = chipColor, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 10.5.sp, color = Color(0xFFA0AAB8))
        Text(value, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18182A))
    }
}

@Composable
private fun DurationChip(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) Color(0xFF279491) else Color(0xFFF6F7FA))
            .border(1.5.dp, if (active) Color(0xFF279491) else Color(0xFFEAECF2), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = if (active) Color.White else Color(0xFF6B7A99))
    }
}

@Composable
private fun FieldHint(text: String) {
    Text(text, fontSize = 10.sp, color = Color(0xFFEF4444), modifier = Modifier.padding(start = 2.dp))
}

@Composable
private fun NoticeBanner(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

// ---- Card chip ----
@Composable
private fun CardChip() {
    Box(
        modifier = Modifier
            .size(32.dp, 24.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.size(18.dp, 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                .padding(2.dp),
            verticalArrangement = Arrangement.spacedBy(1.5.dp)
        ) {
            repeat(2) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
                    repeat(2) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()
                            .clip(RoundedCornerShape(1.dp)).background(Color.White.copy(alpha = 0.4f)))
                    }
                }
            }
        }
    }
}

// ---- Form field ----
@Composable
private fun PayField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    mono: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF8A96A8), letterSpacing = 0.3.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFF6F7FA))
                .border(1.5.dp, Color(0xFFEAECF2), RoundedCornerShape(11.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Canvas(Modifier.size(13.dp)) { if (mono) drawCardSmallIcon() else drawUserIcon() }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f).padding(vertical = 11.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                textStyle = TextStyle(
                    fontSize = if (mono) 13.sp else 12.sp,
                    color = Color(0xFF18182A),
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                    letterSpacing = if (mono) 3.sp else 0.sp,
                ),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, fontSize = if (mono) 13.sp else 12.sp, color = Color(0xFFC0C8D4),
                        fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default, letterSpacing = if (mono) 3.sp else 0.sp)
                    inner()
                }
            )
        }
    }
}

// ---- icons ----
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGatewayIcon() {
    val c = Color(0xFFA0AAB8); val s = Stroke(1.5f)
    drawRoundRect(c, topLeft = Offset(size.width * 0.08f, size.height * 0.25f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.53f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f), style = s)
    drawLine(c, Offset(size.width * 0.08f, size.height * 0.42f), Offset(size.width * 0.92f, size.height * 0.42f), 1.5f)
    drawLine(c, Offset(size.width * 0.28f, size.height * 0.61f), Offset(size.width * 0.46f, size.height * 0.61f), 1.5f, cap = StrokeCap.Round)
    drawCircle(c, radius = size.width * 0.07f, center = Offset(size.width * 0.75f, size.height * 0.61f))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUploadIcon() {
    val c = Color(0xFF8A96A8); val s = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(Path().apply {
        moveTo(size.width / 2, size.height * 0.75f); lineTo(size.width / 2, size.height * 0.25f)
        moveTo(size.width * 0.31f, size.height * 0.44f); lineTo(size.width / 2, size.height * 0.25f); lineTo(size.width * 0.69f, size.height * 0.44f)
    }, c, style = s)
    drawPath(Path().apply {
        moveTo(size.width * 0.125f, size.height * 0.78f); cubicTo(size.width * 0.125f, size.height * 0.88f, size.width * 0.22f, size.height * 0.875f, size.width * 0.22f, size.height * 0.875f)
        lineTo(size.width * 0.78f, size.height * 0.875f)
    }, c, style = Stroke(1.5f, cap = StrokeCap.Round))
    drawLine(c, Offset(size.width * 0.125f, size.height * 0.78f), Offset(size.width * 0.875f, size.height * 0.78f), 1.5f, cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawUserIcon() {
    val c = Color(0xFFC0C8D4); val s = Stroke(1.2f, cap = StrokeCap.Round)
    drawCircle(c, radius = size.minDimension * 0.185f, center = Offset(size.width / 2, size.height * 0.35f), style = s)
    drawPath(Path().apply {
        moveTo(size.width * 0.115f, size.height * 0.92f)
        cubicTo(size.width * 0.115f, size.height * 0.73f, size.width * 0.29f, size.height * 0.58f, size.width / 2, size.height * 0.58f)
        cubicTo(size.width * 0.71f, size.height * 0.58f, size.width * 0.885f, size.height * 0.73f, size.width * 0.885f, size.height * 0.92f)
    }, c, style = s)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCardSmallIcon() {
    val c = Color(0xFFC0C8D4)
    drawRoundRect(c, topLeft = Offset(size.width * 0.08f, size.height * 0.23f),
        size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.54f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f), style = Stroke(1.2f))
    drawLine(c, Offset(size.width * 0.08f, size.height * 0.46f), Offset(size.width * 0.92f, size.height * 0.46f), 1.2f)
    drawLine(c, Offset(size.width * 0.27f, size.height * 0.65f), Offset(size.width * 0.42f, size.height * 0.65f), 1.2f, cap = StrokeCap.Round)
}
