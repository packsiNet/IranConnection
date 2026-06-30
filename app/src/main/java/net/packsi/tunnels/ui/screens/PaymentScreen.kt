package net.packsi.tunnels.ui.screens

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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.packsi.tunnels.data.payment.PaymentViewModel
import net.packsi.tunnels.data.payment.PickedReceipt
import net.packsi.tunnels.data.payment.ReceiptResponse
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

private fun bankCardFor(currency: String, plan: Plan): BankCard =
    if (currency == "usd") BankCard(
        bank = "USD Bank",
        number = "4937 2420 2574 6817",
        holder = "LALEH MANSOURI",
        gradient = listOf(Color(0xFFE63950), Color(0xFFB01030), Color(0xFF7A0C24)),
        shadow = Color(0x5CC81838),
        amount = plan.usdPrice,
    ) else BankCard(
        bank = "Iranian Bank",
        number = "6219 8618 0150 9695",
        holder = "SHAHRAM OVEISI",
        gradient = listOf(Color(0xFF2D8FD8), Color(0xFF1868B2), Color(0xFF0C3D78)),
        shadow = Color(0x5C1878C8),
        amount = "${plan.tmnPrice} تومان",
    )

// ---- Plan definitions ----
private enum class Plan(val label: String, val usdPrice: String, val tmnPrice: String) {
    PRO("PRO", "$3", "500,000"),
    PREMIUM("Premium", "$5", "700,000"),
}

enum class PaymentMode { SUBSCRIPTION, NO_ADS }

private fun noAdsBankCardFor(currency: String): BankCard =
    if (currency == "usd") BankCard(
        bank = "USD Bank",
        number = "4937 2420 2574 6817",
        holder = "LALEH MANSOURI",
        gradient = listOf(Color(0xFFE63950), Color(0xFFB01030), Color(0xFF7A0C24)),
        shadow = Color(0x5CC81838),
        amount = "$1",
    ) else BankCard(
        bank = "Iranian Bank",
        number = "6219 8618 0150 9695",
        holder = "SHAHRAM OVEISI",
        gradient = listOf(Color(0xFF2D8FD8), Color(0xFF1868B2), Color(0xFF0C3D78)),
        shadow = Color(0x5C1878C8),
        amount = "200,000 تومان",
    )

// ---- Payment screen ----
@Composable
fun PaymentScreen(
    onBack: () -> Unit,
    onApproved: () -> Unit = {},
    mode: PaymentMode = PaymentMode.SUBSCRIPTION,
    vm: PaymentViewModel = viewModel(),
) {
    val clipboard = LocalClipboardManager.current
    val state by vm.state.collectAsState()

    var selectedPlan by rememberSaveable { mutableStateOf(Plan.PRO) }
    var selectedCurrency by rememberSaveable { mutableStateOf("tmn") }
    val card = remember(selectedCurrency, selectedPlan, mode) {
        if (mode == PaymentMode.NO_ADS) noAdsBankCardFor(selectedCurrency)
        else bankCardFor(selectedCurrency, selectedPlan)
    }

    var name by rememberSaveable { mutableStateOf("") }
    var last4 by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<PickedReceipt?>(null) }
    var copied by remember { mutableStateOf(false) }
    var showReceipts by rememberSaveable { mutableStateOf(false) }
    var triedSubmit by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) picked = vm.inspectFile(uri)
    }

    LaunchedEffect(copied) { if (copied) { delay(2200); copied = false } }
    LaunchedEffect(state.successMessage) { if (state.successMessage != null) showReceipts = true }
    LaunchedEffect(showReceipts) {
        if (showReceipts) { vm.loadReceipts(); while (true) { delay(6000); vm.loadReceipts() } }
    }
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
    val canSubmit = nameValid && last4Valid && fileValid && !state.submitLoading && !state.tooManyPending

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
                modifier = Modifier.size(34.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                    .background(Color.White).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(8.dp, 14.dp)) {
                    val p = Path().apply { moveTo(size.width, 0f); lineTo(0f, size.height / 2); lineTo(size.width, size.height) }
                    drawPath(p, Color(0xFF18182A), style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (mode == PaymentMode.NO_ADS) "Remove Ads" else "Subscription Plans",
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF18182A), letterSpacing = (-0.3).sp,
                )
                Text(
                    if (mode == PaymentMode.NO_ADS) "One-time · Never see ads again" else "Monthly · Pay in Toman or USD",
                    fontSize = 10.sp, color = Color(0xFFA0AAB8), modifier = Modifier.padding(top = 1.dp),
                )
            }
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .clickable { showReceipts = true }.padding(horizontal = 11.dp, vertical = 7.dp),
            ) { Text("My Receipts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF279491)) }
        }

        // ---- Plan cards (subscription only) ----
        if (mode == PaymentMode.SUBSCRIPTION) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                listOf(Plan.PRO, Plan.PREMIUM).forEach { plan ->
                    PlanCard(
                        plan = plan,
                        selected = selectedPlan == plan,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedPlan = plan },
                    )
                }
            }
        } else {
            // NoAds info banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF5F0FF))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C3AED).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(18.dp)) {
                        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.8f)
                        drawCircle(color = Color(0xFF7C3AED), style = stroke)
                        drawLine(
                            color = Color(0xFF7C3AED),
                            start = Offset(size.width * 0.22f, size.height * 0.78f),
                            end = Offset(size.width * 0.78f, size.height * 0.22f),
                            strokeWidth = 2.8f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Remove Ads — One-time Payment", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B0764))
                    Text(
                        "Pay once and ads will be permanently removed from your account.",
                        fontSize = 10.sp, color = Color(0xFF6D28D9), lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        // ---- Currency selector ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Pay with", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6B7A99))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("tmn" to "Toman", "usd" to "USD").forEach { (code, label) ->
                    val active = selectedCurrency == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Color(0xFF279491) else Color(0xFFF6F7FA))
                            .border(1.5.dp, if (active) Color(0xFF279491) else Color(0xFFEAECF2), RoundedCornerShape(10.dp))
                            .clickable { selectedCurrency = code }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Color(0xFF6B7A99))
                    }
                }
            }
        }

        // ---- Amount badge ----
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
            Column(horizontalAlignment = Alignment.End) {
                val (primary, secondary) = if (mode == PaymentMode.NO_ADS) {
                    if (selectedCurrency == "tmn") "200,000 تومان" to "$1"
                    else "$1" to "200,000 تومان"
                } else {
                    if (selectedCurrency == "tmn") "${selectedPlan.tmnPrice} تومان" to selectedPlan.usdPrice
                    else selectedPlan.usdPrice to "${selectedPlan.tmnPrice} تومان"
                }
                Text(primary, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF18182A), letterSpacing = (-0.3).sp)
                Text(secondary, fontSize = 10.sp, color = Color(0xFFA0AAB8),
                    modifier = Modifier.padding(top = 1.dp))
            }
        }

        // ---- Bank card ----
        if (selectedCurrency == "usd") {
            RedotPayCardUI(card = card, copied = copied) {
                clipboard.setText(AnnotatedString(card.number.replace(" ", "")))
                copied = true
            }
        } else {
            SamanBankCardUI(card = card, copied = copied) {
                clipboard.setText(AnnotatedString(card.number.replace(" ", "")))
                copied = true
            }
        }

        // ---- Form ----
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

            // Upload receipt
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Upload Receipt", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8A96A8), letterSpacing = 0.3.sp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color(0xFFF6F7FA))
                        .border(1.5.dp,
                            if (fileErr != null) Color(0xFFEF4444) else Color(0xFFD0D6E2),
                            RoundedCornerShape(11.dp))
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
                        fontWeight = FontWeight.SemiBold, color = Color(0xFF8A96A8), maxLines = 1)
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
            val submitGradient = if (canSubmit) {
                if (mode == PaymentMode.NO_ADS)
                    Brush.linearGradient(listOf(Color(0xFF9F67F5), Color(0xFF7C3AED)))
                else
                    Brush.linearGradient(listOf(Color(0xFF4ECAC5), Color(0xFF279491)))
            } else {
                Brush.linearGradient(listOf(Color(0xFFE8EAF0), Color(0xFFE8EAF0)))
            }
            val submitShadowColor = if (mode == PaymentMode.NO_ADS) Color(0xFF7C3AED) else Color(0xFF279491)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (canSubmit) Modifier.shadow(12.dp, RoundedCornerShape(12.dp), spotColor = submitShadowColor) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(submitGradient)
                    .clickable(enabled = !state.submitLoading && !state.tooManyPending) {
                        triedSubmit = true
                        val p = picked
                        if (nameValid && last4Valid && p != null && fileErr == null) {
                            if (mode == PaymentMode.NO_ADS)
                                vm.submit(name, last4, 0, p, receiptType = "AdsRemoval")
                            else
                                vm.submit(name, last4, 30, p)
                        }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (state.submitLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Payment Info", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
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
            Text(
                if (r.receiptType.equals("AdsRemoval", ignoreCase = true)) "Remove Ads"
                else "${r.requestedDurationDays ?: 30} days",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18182A),
            )
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
private fun PlanCard(plan: Plan, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .shadow(if (selected) 6.dp else 2.dp, RoundedCornerShape(16.dp),
                spotColor = if (selected) Color(0x33279491) else Color(0x10000000))
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFFEFF9F9) else Color.White)
            .border(2.dp, if (selected) Color(0xFF279491) else Color(0xFFEAECF2), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(plan.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF18182A))
                if (selected) {
                    Box(
                        modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFF279491)),
                        contentAlignment = Alignment.Center
                    ) { Text("✓", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
            Text(plan.usdPrice, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF279491))
            Text("${plan.tmnPrice} تومان", fontSize = 11.sp, color = Color(0xFF6B7A99))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (plan == Plan.PRO) Color(0x1A279491) else Color(0x1AF59E0B))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    if (plan == Plan.PRO) "Popular" else "Best Value",
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = if (plan == Plan.PRO) Color(0xFF279491) else Color(0xFFF59E0B),
                )
            }
        }
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

// ---- Saman Bank Card ----
@Composable
private fun SamanBankCardUI(card: BankCard, copied: Boolean, onCopy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(440f / 277f)
            .shadow(20.dp, RoundedCornerShape(20.dp), spotColor = Color(0xBB200840))
            .clip(RoundedCornerShape(20.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val sc = w / 440f

            drawRect(Brush.linearGradient(
                listOf(Color(0xFF3C1A52), Color(0xFF27103E), Color(0xFF160826)),
                Offset.Zero, Offset(w, size.height)
            ))
            drawRect(Brush.radialGradient(
                listOf(Color(0x478040A0), Color(0x008040A0)),
                Offset(w * .72f, size.height * .20f), w * .50f
            ))
            drawRect(Brush.radialGradient(
                listOf(Color(0x00000000), Color(0x6B000000)),
                Offset(w / 2f, size.height / 2f), w * .70f
            ))
            // Simplified floral dots
            val fc = Color(0x20D2A220)
            for (col in 0..5) for (row in 0..3) {
                val px = (col * 80f + 40f) * sc; val py = (row * 80f + 40f) * sc
                if (py < size.height) {
                    drawCircle(fc, 7f * sc, Offset(px, py), style = Stroke(0.8f * sc))
                    drawCircle(fc.copy(alpha = .07f), 3.5f * sc, Offset(px, py))
                    drawCircle(fc.copy(alpha = .09f), 3f * sc, Offset(px, py - 12f * sc))
                    drawCircle(fc.copy(alpha = .09f), 3f * sc, Offset(px, py + 12f * sc))
                    drawCircle(fc.copy(alpha = .09f), 3f * sc, Offset(px - 12f * sc, py))
                    drawCircle(fc.copy(alpha = .09f), 3f * sc, Offset(px + 12f * sc, py))
                }
            }
            // Outer gold border
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFFF8E07A), Color(0xFFD6AA1E), Color(0xFFF4CE48), Color(0xFFBB8A08), Color(0xFFECC030)), Offset.Zero, Offset(w, size.height)),
                Offset(9f * sc, 9f * sc), androidx.compose.ui.geometry.Size(w - 18f * sc, size.height - 18f * sc),
                androidx.compose.ui.geometry.CornerRadius(14f * sc), style = Stroke(1.9f * sc)
            )
            drawRoundRect(
                Color(0x42D7AA20), Offset(13.5f * sc, 13.5f * sc),
                androidx.compose.ui.geometry.Size(w - 27f * sc, size.height - 27f * sc),
                androidx.compose.ui.geometry.CornerRadius(10f * sc), style = Stroke(.65f * sc)
            )
            // EMV Chip (gold)
            val cx = 32f * sc; val cy = 82f * sc; val cw = 52f * sc; val ch = 40f * sc
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFFF4D458), Color(0xFFBC8A00), Color(0xFFDEBC28)), Offset(cx, cy), Offset(cx + cw, cy + ch)),
                Offset(cx, cy), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(5.5f * sc)
            )
            drawRoundRect(Color(0xFFA07808), Offset(cx + 2.5f * sc, cy + 2.5f * sc), androidx.compose.ui.geometry.Size(cw - 5f * sc, ch - 5f * sc), androidx.compose.ui.geometry.CornerRadius(4f * sc))
            drawLine(Color(0xFFC09A10), Offset(cx + 2.5f * sc, cy + ch / 2), Offset(cx + cw - 2.5f * sc, cy + ch / 2), .9f * sc)
            drawLine(Color(0xFFC09A10), Offset(cx + cw / 2, cy + 2.5f * sc), Offset(cx + cw / 2, cy + ch - 2.5f * sc), .9f * sc)
            listOf(
                Offset(cx + 4.5f * sc, cy + 4.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 13f * sc),
                Offset(cx + 28.5f * sc, cy + 4.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 13f * sc),
                Offset(cx + 4.5f * sc, cy + 22.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 12f * sc),
                Offset(cx + 28.5f * sc, cy + 22.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 12f * sc),
            ).forEach { (o, s) -> drawRoundRect(Color(0x99D2A218), o, s, androidx.compose.ui.geometry.CornerRadius(2.5f * sc)) }
            // Contactless arcs (gold, opening right)
            val nfcX = 93f * sc; val nfcY = (90f + 12f) * sc
            val nfcC = Color(0xB3DEB02A)
            drawCircle(nfcC, 2.4f * sc, Offset(nfcX, nfcY))
            listOf(7f to 9f, 11f to 15f, 15f to 21f).forEach { (hh, bow) ->
                drawPath(Path().apply {
                    moveTo(nfcX, nfcY - hh * sc)
                    quadraticBezierTo(nfcX + bow * sc, nfcY, nfcX, nfcY + hh * sc)
                }, nfcC, style = Stroke(1.35f * sc, cap = StrokeCap.Round))
            }
        }
        // Proportional overlay: SVG card is 440×277, positions scaled to actual card size
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight; val w = maxWidth
            // SAMAN logo — SVG group at translate(282,20), text "SAMAN" baseline y=48
            Column(
                Modifier.align(Alignment.TopEnd).padding(top = h * 0.072f, end = w * 0.055f),
                horizontalAlignment = Alignment.End
            ) {
                Text("SAMAN", fontFamily = FontFamily.Serif, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, color = Color(0xF5E6BA2C), letterSpacing = 2.sp)
                Text("BANK", fontSize = 8.sp, color = Color(0x9ACDA222), letterSpacing = 4.sp)
            }
            // Card number — SVG baseline y=180 → top ≈ 163/277 = 58.8%
            Text(card.number,
                Modifier.padding(top = h * 0.588f, start = w * 0.073f),
                fontFamily = FontFamily.Monospace, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = Color(0xEEEAC034), letterSpacing = 2.sp
            )
            // Cardholder name — SVG baseline y=224 → top ≈ 214/277 = 77.3%
            Text(card.holder,
                Modifier.padding(top = h * 0.773f, start = w * 0.073f),
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xDCDCB02C), letterSpacing = 1.5.sp
            )
            // Valid THRU — SVG group at y=238 → top ≈ 232/277 = 83.8%
            Row(
                Modifier.padding(top = h * 0.838f, start = w * 0.073f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column {
                    Text("VALID", fontSize = 6.sp, color = Color(0x7AC8A222))
                    Text("THRU", fontSize = 6.sp, color = Color(0x7AC8A222))
                }
                Text("06/28", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xD5DEB02C))
            }
            // SHETAB — SVG text-anchor end at x=416, y=260 → bottom-right
            Text("SHETAB",
                Modifier.align(Alignment.BottomEnd).padding(bottom = 10.dp, end = 12.dp),
                fontSize = 8.sp, fontWeight = FontWeight.Bold,
                color = Color(0x8BC8A222), letterSpacing = 1.sp
            )
            // Copy button — right side, same vertical as cardholder name
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = h * 0.773f, end = w * 0.04f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x28DEB02C))
                    .border(1.dp, Color(0x3FDEB02C), RoundedCornerShape(6.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(if (copied) "✓ Copied" else "Copy", fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xCCDEB02C))
            }
        }
    }
}

// ---- RedotPay International Card ----
@Composable
private fun RedotPayCardUI(card: BankCard, copied: Boolean, onCopy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(440f / 277f)
            .shadow(18.dp, RoundedCornerShape(20.dp), spotColor = Color(0x66CC1828))
            .clip(RoundedCornerShape(20.dp))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val sc = w / 440f

            drawRect(Brush.linearGradient(
                listOf(Color(0xFF0C0D1E), Color(0xFF10112A), Color(0xFF07080E)),
                Offset.Zero, Offset(w, size.height)
            ))
            // Dot grid
            val dotC = Color(0x0BFFFFFF)
            for (col in 0..15) for (row in 0..10) {
                drawCircle(dotC, .9f * sc, Offset((col * 30f + 15f) * sc, (row * 30f + 15f) * sc))
                if (col < 15 && row < 10)
                    drawCircle(dotC, .9f * sc, Offset((col * 30f + 30f) * sc, (row * 30f + 30f) * sc))
            }
            drawRect(Brush.radialGradient(listOf(Color(0x3DCC1828), Color(0x00CC1828)), Offset(w * .10f, size.height * .90f), w * .44f))
            drawRect(Brush.radialGradient(listOf(Color(0x292535C8), Color(0x002535C8)), Offset(w * .85f, size.height * .15f), w * .40f))
            drawRoundRect(Color(0x12FFFFFF), Offset(1f, 1f),
                androidx.compose.ui.geometry.Size(w - 2f, size.height - 2f),
                androidx.compose.ui.geometry.CornerRadius(19f * sc), style = Stroke(1f))
            // EMV Chip (silver)
            val cx = 32f * sc; val cy = 82f * sc; val cw = 52f * sc; val ch = 40f * sc
            drawRoundRect(
                Brush.linearGradient(listOf(Color(0xFFD6D6DE), Color(0xFF959598), Color(0xFFBCBCC4)), Offset(cx, cy), Offset(cx + cw, cy + ch)),
                Offset(cx, cy), androidx.compose.ui.geometry.Size(cw, ch), androidx.compose.ui.geometry.CornerRadius(5.5f * sc)
            )
            drawRoundRect(Color(0xFF828292), Offset(cx + 2.5f * sc, cy + 2.5f * sc), androidx.compose.ui.geometry.Size(cw - 5f * sc, ch - 5f * sc), androidx.compose.ui.geometry.CornerRadius(4f * sc))
            drawLine(Color(0xFF9A9AAC), Offset(cx + 2.5f * sc, cy + ch / 2), Offset(cx + cw - 2.5f * sc, cy + ch / 2), .9f * sc)
            drawLine(Color(0xFF9A9AAC), Offset(cx + cw / 2, cy + 2.5f * sc), Offset(cx + cw / 2, cy + ch - 2.5f * sc), .9f * sc)
            listOf(
                Offset(cx + 4.5f * sc, cy + 4.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 13f * sc),
                Offset(cx + 28.5f * sc, cy + 4.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 13f * sc),
                Offset(cx + 4.5f * sc, cy + 22.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 12f * sc),
                Offset(cx + 28.5f * sc, cy + 22.5f * sc) to androidx.compose.ui.geometry.Size(19f * sc, 12f * sc),
            ).forEach { (o, s) -> drawRoundRect(Color(0x80B4B4C4), o, s, androidx.compose.ui.geometry.CornerRadius(2.5f * sc)) }
            // Contactless arcs (silver, opening right)
            val nfcX = 93f * sc; val nfcY = (90f + 12f) * sc
            val nfcC = Color(0x85B9B9CD)
            drawCircle(nfcC, 2.4f * sc, Offset(nfcX, nfcY))
            listOf(7f to 9f, 11f to 15f, 15f to 21f).forEach { (hh, bow) ->
                drawPath(Path().apply {
                    moveTo(nfcX, nfcY - hh * sc)
                    quadraticBezierTo(nfcX + bow * sc, nfcY, nfcX, nfcY + hh * sc)
                }, nfcC, style = Stroke(1.3f * sc, cap = StrokeCap.Round))
            }
        }
        // Proportional overlay: SVG card is 440×277
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val h = maxHeight; val w = maxWidth
            // RedotPay logo — SVG group at translate(30,24): circle r=15, text alongside
            Row(
                Modifier.padding(top = h * 0.087f, start = w * 0.068f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(w * 0.068f).clip(CircleShape).background(Color(0xFFC41828)),
                    contentAlignment = Alignment.Center
                ) { Text("R", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White) }
                Column {
                    Text("Redot", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xE0FFFFFF), letterSpacing = 0.3.sp)
                    Text("Pay", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xF2D72A3C), letterSpacing = 0.3.sp)
                }
            }
            // NFC icon — SVG at translate(400,28), 40 units from right edge
            Canvas(
                Modifier.align(Alignment.TopEnd).padding(top = h * 0.101f, end = w * 0.091f).size(22.dp, 24.dp)
            ) {
                val nfcC = Color(0x38FFFFFF)
                drawCircle(Color(0x1FFFFFFF), 3f, Offset(2f, size.height / 2f))
                listOf(7f to 9f, 11f to 15f).forEach { (hh, bow) ->
                    drawPath(Path().apply {
                        moveTo(4f, size.height / 2f - hh)
                        quadraticBezierTo(4f + bow, size.height / 2f, 4f, size.height / 2f + hh)
                    }, nfcC, style = Stroke(1.3f, cap = StrokeCap.Round))
                }
            }
            // Card number — SVG baseline y=180 → top ≈ 58.8%
            Text(card.number,
                Modifier.padding(top = h * 0.588f, start = w * 0.073f),
                fontFamily = FontFamily.Monospace, fontSize = 15.sp,
                fontWeight = FontWeight.Medium, color = Color(0xDCFFFFFF), letterSpacing = 2.sp
            )
            // Cardholder name — SVG baseline y=224 → top ≈ 77.3%
            Text(card.holder,
                Modifier.padding(top = h * 0.773f, start = w * 0.073f),
                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = Color(0x9EFFFFFF), letterSpacing = 1.5.sp
            )
            // Valid THRU — SVG group at y=238 → top ≈ 83.8%
            Row(
                Modifier.padding(top = h * 0.838f, start = w * 0.073f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column {
                    Text("VALID", fontSize = 6.sp, color = Color(0x4DFFFFFF))
                    Text("THRU", fontSize = 6.sp, color = Color(0x4DFFFFFF))
                }
                Text("09/29", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0x94FFFFFF))
            }
            // VISA — SVG text-anchor end at x=416, y=260 → bottom-right
            Text("VISA",
                Modifier.align(Alignment.BottomEnd).padding(bottom = 10.dp, end = 12.dp),
                fontSize = 22.sp, fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic,
                color = Color(0xD6FFFFFF), letterSpacing = (-0.5).sp
            )
            // Copy button — right side, same vertical as cardholder name
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = h * 0.773f, end = w * 0.04f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x22FFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(if (copied) "✓ Copied" else "Copy", fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold, color = Color(0xCCFFFFFF))
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
