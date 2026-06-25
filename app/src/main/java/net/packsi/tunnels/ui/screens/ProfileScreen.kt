package net.packsi.tunnels.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import net.packsi.tunnels.data.auth.AuthViewModel
import net.packsi.tunnels.data.auth.UserProfile
import net.packsi.tunnels.data.subscription.SubscriptionResponse

// ---- Color palette (Profile-screen specific) ----
private val TealStart   = Color(0xFF3DBFBA)
private val TealMid     = Color(0xFF279491)
private val TealEnd     = Color(0xFF195E5C)
private val GoldStart   = Color(0xFFFBCF42)
private val GoldMid     = Color(0xFFF5A620)
private val GoldEnd     = Color(0xFFE48808)
private val TextPrimary = Color(0xFF18182A)
private val TextMuted   = Color(0xFFA0AAB8)
private val TextHint    = Color(0xFFC2CAD6)
private val Divider     = Color(0xFFF2F4F8)
private val Chevron     = Color(0xFFD0D6E0)
private val BgScreen    = Color(0xFFF0F2F6)
private val CardWhite   = Color.White
private val Amber       = Color(0xFFFBBF24)
private val Red         = Color(0xFFEF4444)

// ---- Main screen ----
@Composable
fun ProfileScreen(vm: AuthViewModel = viewModel(), onSignOut: () -> Unit = {}, openPaymentOnLoad: Boolean = false, onPaymentOpened: () -> Unit = {}) {
    val state by vm.state.collectAsState()

    var showPayment by rememberSaveable { mutableStateOf(false) }
    var showEdit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadProfile()
        vm.loadSubscription()
    }

    LaunchedEffect(openPaymentOnLoad) {
        if (openPaymentOnLoad) { showPayment = true; onPaymentOpened() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgScreen)
    ) {
        ProfileView(
            profile = state.profile,
            subscription = state.subscription,
            subscriptionNotFound = state.subscriptionNotFound,
            email = state.email,
            fullName = state.fullName,
            onPay = { showPayment = true },
            onEditProfile = { showEdit = true },
            onExit = { vm.logout(); onSignOut() },
        )
        if (showPayment) {
            PaymentScreen(
                onBack = { showPayment = false },
                onApproved = { vm.loadSubscription() },
            )
        }
        if (showEdit) {
            EditProfileScreen(
                vm = vm,
                currentName = state.fullName,
                onDone = { showEdit = false },
                onBack = { showEdit = false },
            )
        }
    }
}

// ---- Profile view (logged in) ----
@Composable
private fun ProfileView(
    profile: UserProfile?,
    subscription: SubscriptionResponse?,
    subscriptionNotFound: Boolean,
    email: String,
    fullName: String,
    onPay: () -> Unit,
    onEditProfile: () -> Unit,
    onExit: () -> Unit,
) {
    // No embedded bottom nav here — the Profile tab reuses the same shared
    // tab bar (AppBottomNav) that Home and Apps already render in MainActivity.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // 1. Profile hero card
        ProfileHeroCard(
            profile = profile,
            subscription = subscription,
            email = email,
            fullName = fullName,
        )

        // Subscription state notices
        if (subscriptionNotFound) {
            SubscriptionNotice(
                text = "No subscription — upgrade for full access.",
                color = Amber,
            )
        } else if (subscription != null && (!subscription.isActive || (subscription.daysRemaining ?: 0) <= 7)) {
            val days = subscription.daysRemaining ?: 0
            SubscriptionNotice(
                text = if (!subscription.isActive) "Your subscription has expired — renew now."
                       else "Only $days days left — renew now.",
                color = Red,
            )
        }

        // 2. Mini stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(modifier = Modifier.weight(1f), label = "↓ Download", value = "28.1", unit = "GB", barPct = 0.72f,
                barBrush = Brush.horizontalGradient(listOf(Color(0xFF68D0CA), TealStart)))
            StatCard(modifier = Modifier.weight(1f), label = "↑ Upload", value = "17.1", unit = "GB", barPct = 0.43f,
                barBrush = Brush.horizontalGradient(listOf(Color(0xFF9DD8D6), Color(0xFF68D0CA))))
        }

        // 3. Renew card
        RenewCard(onPay = onPay)

        // 4. Menu list
        MenuCard()

        // 5. Edit profile / change password
        Button(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Edit profile", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TealMid)
        }

        // 6. Exit button
        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Exit Session", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Red)
        }

        Spacer(Modifier.height(2.dp))
    }
}

// ---- Profile hero card ----
@Composable
private fun SubscriptionNotice(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("⚠", fontSize = 13.sp, color = color)
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
private fun ProfileHeroCard(
    profile: UserProfile?,
    subscription: SubscriptionResponse?,
    email: String,
    fullName: String,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    // Prefer the dedicated /api/subscription response; fall back to the profile's embedded one.
    val profileSub = profile?.subscription
    val displayName = fullName.ifBlank { profile?.fullName ?: "" }.ifBlank { "—" }
    val displayEmail = email.ifBlank { profile?.email ?: "" }
    val planLabel = (subscription?.plan ?: profileSub?.plan)?.ifBlank { null } ?: "Free"
    val avatarLetter = displayName.firstOrNull()?.uppercaseChar()?.toString()
        ?: displayEmail.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val expireDate = subscription?.expireDate ?: profileSub?.expireDate
    val activeUntil = expireDate?.substringBefore('T')?.ifBlank { null } ?: "—"
    val daysRemaining = subscription?.daysRemaining ?: profileSub?.daysRemaining
    val remainingLabel = daysRemaining?.let { "$it days" } ?: "—"
    val progress = daysRemaining?.let { (it / 30f).coerceIn(0f, 1f) } ?: 0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = TealMid)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(
                colors = listOf(TealStart, TealMid, TealEnd),
                start = Offset(0f, 0f), end = Offset(600f, 500f)
            ))
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        RingDecor(size = 110.dp, borderWidth = 22.dp, alpha = 0.07f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-18).dp))
        RingDecor(size = 80.dp, borderWidth = 16.dp, alpha = 0.05f,
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = 36.dp))

        Column {
            // Avatar row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(avatarLetter, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(displayName,
                        fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White,
                        letterSpacing = (-0.2).sp)
                    Text(displayEmail,
                        fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp))
                }

                // Plan badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Amber.copy(alpha = pulse)))
                    Text(planLabel, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Stats box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .padding(horizontal = 13.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ACTIVE UNTIL",
                        fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.5f), letterSpacing = 0.9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(activeUntil,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.2).sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("REMAINING",
                        fontSize = 9.5.sp, color = Color.White.copy(alpha = 0.5f), letterSpacing = 0.9.sp)
                    Spacer(Modifier.height(2.dp))
                    Text(remainingLabel,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Days progress bar
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.18f))
            ) {
                Box(modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.78f)))
            }
        }
    }
}

// ---- Stat card ----
@Composable
private fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    barPct: Float,
    barBrush: Brush,
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(17.dp))
            .clip(RoundedCornerShape(17.dp))
            .background(CardWhite)
            .padding(horizontal = 13.dp, vertical = 13.dp)
    ) {
        Text(label.uppercase(),
            fontSize = 9.5.sp, color = TextMuted, letterSpacing = 0.8.sp,
            modifier = Modifier.padding(bottom = 5.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = (-0.5).sp)
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFEFF1F6))
        ) {
            Box(modifier = Modifier
                .fillMaxWidth(barPct)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(barBrush))
        }
    }
}

// ---- Renew card ----
@Composable
private fun RenewCard(onPay: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = GoldMid)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(
                colors = listOf(GoldStart, GoldMid, GoldEnd),
                start = Offset(0f, 0f), end = Offset(500f, 350f)
            ))
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        // Decorative rings
        RingDecor(size = 100.dp, borderWidth = 20.dp, alpha = 0.07f,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 14.dp, y = (-28).dp))
        RingDecor(size = 54.dp, borderWidth = 11.dp, alpha = 0.06f,
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (-14).dp, y = 14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(
                "MEMBERSHIP",
                fontSize = 9.sp, color = Color.White.copy(alpha = 0.50f), letterSpacing = 1.2.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                "Connect Without Limits",
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                letterSpacing = (-0.5).sp, lineHeight = 27.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Unlock full-speed access with PRO or Premium.\nPay in Toman or USD — your choice.",
                fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.70f),
                lineHeight = 17.sp, letterSpacing = 0.sp,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onPay() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Explore Plans",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB07210),
                    )
                    Text(
                        "→",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB07210),
                    )
                }
            }
        }
    }
}

// ---- Menu card ----
@Composable
private fun MenuCard() {
    val items = listOf(
        MenuRowData(
            iconBg = Color(0xFFEAF8F8), iconColor = TealStart,
            title = "Support Tickets", subtitle = "2 open requests",
            badge = "2", badgeBg = TealStart,
            isLast = true,
            icon = { c: DrawScope -> c.drawSupportIcon() }
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(CardWhite)
    ) {
        items.forEach { item ->
            MenuRow(item = item)
            if (!item.isLast) {
                HorizontalDivider(color = Divider, thickness = 1.dp)
            }
        }
    }
}

private data class MenuRowData(
    val iconBg: Color,
    val iconColor: Color,
    val title: String,
    val subtitle: String,
    val badge: String? = null,
    val badgeBg: Color = Color.Transparent,
    val isLast: Boolean = false,
    val icon: (DrawScope) -> Unit,
)

@Composable
private fun MenuRow(item: MenuRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(item.iconBg),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) { item.icon(this) }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(1.dp))
            Text(item.subtitle, fontSize = 10.5.sp, color = TextMuted)
        }

        // Optional badge
        if (item.badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(item.badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(item.badge, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Chevron
        Canvas(modifier = Modifier.size(5.dp, 10.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f); lineTo(size.width, size.height / 2); lineTo(0f, size.height)
            }
            drawPath(path, color = Chevron, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}


// ---- Decorative ring ----
@Composable
private fun RingDecor(size: Dp, borderWidth: Dp, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, Color.White.copy(alpha = alpha), CircleShape)
    )
}

// ---- Canvas drawing helpers ----
private fun DrawScope.drawGlobeIcon() {
    val cx = size.width / 2; val cy = size.height / 2; val r = size.minDimension * 0.34f
    val stroke = Stroke(width = 2f, cap = StrokeCap.Round)
    drawCircle(color = Color.White, radius = r, center = Offset(cx, cy), style = stroke)
    drawPath(Path().apply {
        moveTo(cx, cy - r); cubicTo(cx - r * 0.7f, cy - r * 0.4f, cx - r * 0.7f, cy + r * 0.4f, cx, cy + r)
    }, Color.White, style = stroke)
    drawPath(Path().apply {
        moveTo(cx, cy - r); cubicTo(cx + r * 0.7f, cy - r * 0.4f, cx + r * 0.7f, cy + r * 0.4f, cx, cy + r)
    }, Color.White, style = stroke)
    drawLine(Color.White.copy(alpha = 0.55f), Offset(cx - r, cy), Offset(cx + r, cy), strokeWidth = 1.8f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.35f), Offset(cx - r * 0.8f, cy - r * 0.5f), Offset(cx + r * 0.8f, cy - r * 0.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.35f), Offset(cx - r * 0.8f, cy + r * 0.5f), Offset(cx + r * 0.8f, cy + r * 0.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
}

private fun DrawScope.drawMailIcon() {
    val s = Stroke(width = 1.8f, cap = StrokeCap.Round)
    drawRoundRect(TextHint, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f), style = s)
    drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, TextHint, style = s)
}

private fun DrawScope.drawLockIcon() {
    val w = size.width; val h = size.height
    drawRoundRect(TextHint, topLeft = Offset(0f, h * 0.43f), size = androidx.compose.ui.geometry.Size(w, h * 0.53f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f), style = Stroke(1.8f))
    drawPath(Path().apply {
        moveTo(w * 0.25f, h * 0.43f); lineTo(w * 0.25f, h * 0.32f)
        cubicTo(w * 0.25f, h * 0.12f, w * 0.75f, h * 0.12f, w * 0.75f, h * 0.32f); lineTo(w * 0.75f, h * 0.43f)
    }, TextHint, style = Stroke(1.8f, cap = StrokeCap.Round))
    drawCircle(TextHint, radius = w * 0.14f, center = Offset(w / 2, h * 0.69f))
}

private fun DrawScope.drawEyeIcon(visible: Boolean) {
    val color = if (visible) TealStart else TextHint
    val s = Stroke(1.8f, cap = StrokeCap.Round)
    val cx = size.width / 2; val cy = size.height / 2
    drawPath(Path().apply {
        moveTo(size.width * 0.075f, cy)
        cubicTo(size.width * 0.35f, cy - size.height * 0.3f, size.width * 0.65f, cy - size.height * 0.3f, size.width * 0.925f, cy)
        cubicTo(size.width * 0.65f, cy + size.height * 0.3f, size.width * 0.35f, cy + size.height * 0.3f, size.width * 0.075f, cy)
    }, color, style = s)
    drawCircle(color, radius = size.minDimension * 0.12f, center = Offset(cx, cy), style = s)
    if (!visible) drawLine(TextHint, Offset(size.width * 0.125f, size.height * 0.125f),
        Offset(size.width * 0.875f, size.height * 0.875f), strokeWidth = 1.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawSupportIcon() {
    val s = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawPath(Path().apply {
        moveTo(size.width / 2, size.height * 0.063f)
        cubicTo(size.width * 0.26f, size.height * 0.063f, size.height * 0.063f, size.width / 2,
            size.height * 0.063f, size.width / 2)
        lineTo(size.height * 0.063f, size.width * 0.6f)
        cubicTo(size.width * 0.32f, size.height * 0.78f, size.width * 0.41f, size.height * 0.95f, size.width / 2, size.height * 0.95f)
        cubicTo(size.width * 0.74f, size.height * 0.95f, size.width * 0.94f, size.height * 0.75f, size.width * 0.94f, size.width * 0.51f)
        cubicTo(size.width * 0.94f, size.width * 0.27f, size.width * 0.74f, size.height * 0.063f, size.width / 2, size.height * 0.063f)
    }, TealStart, style = s)
    drawLine(TealStart, Offset(size.width * 0.31f, size.height * 0.47f), Offset(size.width * 0.53f, size.height * 0.47f), 1.8f, cap = StrokeCap.Round)
    drawLine(TealStart, Offset(size.width * 0.31f, size.height * 0.66f), Offset(size.width * 0.69f, size.height * 0.66f), 1.8f, cap = StrokeCap.Round)
}



// ---- Edit profile ----
@Composable
private fun EditProfileScreen(
    vm: AuthViewModel,
    currentName: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var fullName by rememberSaveable { mutableStateOf(currentName) }
    var err by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) { kotlinx.coroutines.delay(900); vm.clearUpdateState(); onDone() }
    }
    BackHandler(onBack = onBack)

    val submit = {
        val nameChanged = fullName.trim() != currentName.trim()
        err = if (!nameChanged) "No changes made" else null
        if (nameChanged) {
            vm.clearUpdateState()
            vm.updateProfile(fullName = fullName.trim(), currentPassword = null, newPassword = null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgScreen)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
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
                    drawPath(p, TextPrimary, style = Stroke(3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            Text("Edit Profile", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        }

        Text("Full name", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; if (err != null) err = null },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Full name", color = TextHint, fontSize = 14.sp) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealMid,
                unfocusedBorderColor = Divider,
                cursorColor = TealMid,
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
            ),
        )

        err?.let { Text(it, fontSize = 11.sp, color = Red, fontWeight = FontWeight.Medium) }
        state.updateError?.let { Text(it, fontSize = 11.sp, color = Red, fontWeight = FontWeight.Medium) }
        if (state.updateSuccess) Text("Changes saved", fontSize = 11.sp, color = TealMid, fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4ECAC5), TealMid)))
                .clickable(enabled = !state.updateLoading) { submit() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.updateLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Save changes", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
