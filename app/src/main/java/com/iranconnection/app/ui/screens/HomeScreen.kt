package com.iranconnection.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iranconnection.app.ConfigFetchStatus
import com.iranconnection.app.data.auth.AuthViewModel
import com.iranconnection.app.ui.components.CountryFlag
import com.iranconnection.app.ui.components.SignalBars
import com.iranconnection.app.ui.components.WorldMap
import com.iranconnection.app.ui.theme.AppColors

@Composable
fun HomeScreen(
    connected: Boolean,
    statusLabel: String,
    seconds: Long,
    serverIp: String?,
    onToggle: () -> Unit,
    onServerCardClick: () -> Unit,
    onShowLogs: () -> Unit = {},
    onGoToLogin: () -> Unit = {},
    configStatus: ConfigFetchStatus = ConfigFetchStatus.Success,
    buttonEnabled: Boolean = true,
) {
    val accent by animateColorAsState(if (connected) AppColors.Teal else AppColors.Red, label = "accent")

    val authVm: AuthViewModel = viewModel()
    val authState by authVm.state.collectAsState()
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) authVm.loadSubscription()
    }
    val isPremium = authState.subscription?.plan?.let { it == "Premium" || it == "Admin" } ?: false
    var showPremiumModal by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg),
    ) {
        HomeHeader(
            configStatus = configStatus,
            onShowLogs = onShowLogs,
            isPremium = isPremium,
            onPremiumClick = { if (!isPremium) showPremiumModal = true },
        )

        // Connection status
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(connected, accent)
            Spacer(Modifier.size(7.dp))
            Text(
                statusLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
        }

        // Timer
        Text(
            text = formatTime(seconds),
            modifier = Modifier.fillMaxWidth(),
            fontSize = 58.sp,
            fontWeight = FontWeight.Light,
            color = AppColors.TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            letterSpacing = 2.sp,
        )

        // Speed stats
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            SpeedStat(if (connected) "20,3" else "0,0", "Download")
            Spacer(Modifier.size(52.dp))
            SpeedStat(if (connected) "18,3" else "0,0", "Upload")
        }

        // Map + rings + power button
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            WorldMap(Modifier.fillMaxSize())
            PulseRings(connected)
            PowerButton(connected, accent, buttonEnabled, onToggle)
        }

        ServerCard(connected = connected, serverIp = serverIp, onClick = onServerCardClick)
        Spacer(Modifier.height(12.dp))
    }

    if (showPremiumModal) {
        PremiumModal(
            onDismiss = { showPremiumModal = false },
            onGoToLogin = { showPremiumModal = false; onGoToLogin() },
        )
    }
}

@Composable
private fun HomeHeader(
    configStatus: ConfigFetchStatus,
    onShowLogs: () -> Unit,
    isPremium: Boolean,
    onPremiumClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HamburgerMenu(onShowLogs = onShowLogs)
            ConfigIndicator(configStatus)
        }
        // Premium badge — a tappable upsell button when the user isn't premium.
        Row(
            Modifier
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isPremium) Brush.horizontalGradient(listOf(AppColors.CardBg, AppColors.CardBg))
                    else Brush.horizontalGradient(listOf(Color(0xFFFFF4D6), Color(0xFFFFE9B0))),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !isPremium,
                ) { onPremiumClick() }
                .padding(start = 12.dp, end = if (isPremium) 16.dp else 13.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Canvas(Modifier.size(20.dp, 16.dp)) {
                val w = size.width / 20f
                val h = size.height / 16f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(1.5f * w, 14f * h); lineTo(4.5f * w, 5.5f * h); lineTo(8.5f * w, 10.5f * h)
                    lineTo(10f * w, 1.5f * h); lineTo(11.5f * w, 10.5f * h); lineTo(15.5f * w, 5.5f * h)
                    lineTo(18.5f * w, 14f * h); close()
                }
                drawPath(path, AppColors.Gold)
            }
            Text("Premium", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
            if (!isPremium) {
                Text("↑", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD9920A))
            }
        }
    }
}

/** Hamburger button that opens a dropdown menu. Currently exposes the connection-log viewer. */
@Composable
private fun HamburgerMenu(onShowLogs: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(44.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .background(AppColors.CardBg, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { open = true },
            contentAlignment = Alignment.CenterStart,
        ) {
            Column(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(Modifier.size(20.dp, 2.dp).background(Color(0xFF3A3A4C), RoundedCornerShape(2.dp)))
                Box(Modifier.size(12.dp, 2.dp).background(Color(0xFF3A3A4C), RoundedCornerShape(2.dp)))
                Box(Modifier.size(20.dp, 2.dp).background(Color(0xFF3A3A4C), RoundedCornerShape(2.dp)))
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Connection Logs", fontSize = 14.sp, color = AppColors.TextPrimary) },
                onClick = {
                    open = false
                    onShowLogs()
                },
            )
        }
    }
}

/** Compact config-load status chip next to the hamburger: spinner while loading, green dot online, red dot offline. */
@Composable
private fun ConfigIndicator(status: ConfigFetchStatus) {
    val color = when (status) {
        ConfigFetchStatus.Loading -> AppColors.Gold
        ConfigFetchStatus.Success -> AppColors.Teal
        ConfigFetchStatus.Error   -> AppColors.Red
    }
    Box(
        Modifier
            .size(44.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .background(AppColors.CardBg, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (status == ConfigFetchStatus.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = color,
                strokeWidth = 2.dp,
            )
        } else {
            // Filled dot with a soft outer halo.
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun StatusDot(connected: Boolean, accent: Color) {
    Box(
        Modifier.size(19.dp).clip(CircleShape).background(accent),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(10.dp)) {
            val w = size.width
            if (connected) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0.12f * w, 0.5f * w); lineTo(0.38f * w, 0.85f * w); lineTo(0.88f * w, 0.15f * w)
                }
                drawPath(p, Color.White, style = Stroke(width = 0.16f * w, cap = StrokeCap.Round))
            } else {
                drawLine(Color.White, Offset(0.15f * w, 0.15f * w), Offset(0.85f * w, 0.85f * w), strokeWidth = 1.6f.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color.White, Offset(0.85f * w, 0.15f * w), Offset(0.15f * w, 0.85f * w), strokeWidth = 1.6f.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun SpeedStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            Spacer(Modifier.size(3.dp))
            Text("Mbps", fontSize = 14.sp, color = AppColors.TextMuted, modifier = Modifier.padding(bottom = 1.dp))
        }
        Text(label, fontSize = 12.sp, color = AppColors.TextMuted, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun PulseRings(connected: Boolean) {
    val t = rememberInfiniteTransition(label = "rings")
    val base = if (connected) AppColors.Teal else AppColors.Red

    @Composable
    fun ring(sizeDp: Int, alphaFill: Float, durationMs: Int, maxScale: Float, minAlpha: Float, delay: Int) {
        val scale by t.animateFloat(
            1f, maxScale,
            infiniteRepeatable(tween(durationMs, delay, FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scale$sizeDp",
        )
        val alpha by t.animateFloat(
            1f, minAlpha,
            infiniteRepeatable(tween(durationMs, delay, FastOutSlowInEasing), RepeatMode.Reverse),
            label = "alpha$sizeDp",
        )
        Box(
            Modifier
                .size(sizeDp.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(base.copy(alpha = alphaFill * alpha)),
        )
    }

    ring(305, 0.05f, 3500, 1.12f, 0.5f, 500)
    ring(252, 0.09f, 3000, 1.12f, 0.5f, 0)
    ring(200, 0.16f, 2500, 1.07f, 0.65f, 0)
}

@Composable
private fun PowerButton(connected: Boolean, accent: Color, enabled: Boolean, onToggle: () -> Unit) {
    val t = rememberInfiniteTransition(label = "glow")
    val glow by t.animateFloat(
        0f, 0.12f,
        infiniteRepeatable(tween(1250, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    val gradient = if (connected) {
        Brush.linearGradient(listOf(AppColors.TealLight, AppColors.TealDark))
    } else {
        Brush.linearGradient(listOf(AppColors.RedLight, AppColors.Red))
    }
    Box(
        Modifier
            .size(142.dp)
            .shadow(20.dp, CircleShape, ambientColor = accent, spotColor = accent)
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
            ) { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = glow)))
        Canvas(Modifier.size(54.dp)) {
            val u = size.width / 54f
            val white = Color.White.copy(alpha = 0.95f)
            drawLine(white, Offset(27 * u, 11 * u), Offset(27 * u, 28 * u), strokeWidth = 3.6f * u, cap = StrokeCap.Round)
            val arc = androidx.compose.ui.graphics.Path().apply {
                moveTo(17.5f * u, 18 * u)
                cubicTo(13.8f * u, 21.2f * u, 11.5f * u, 25.5f * u, 11.5f * u, 30.3f * u)
                cubicTo(11.5f * u, 39.3f * u, 18.4f * u, 46.5f * u, 27 * u, 46.5f * u)
                cubicTo(35.6f * u, 46.5f * u, 42.5f * u, 39.3f * u, 42.5f * u, 30.3f * u)
                cubicTo(42.5f * u, 25.5f * u, 40.2f * u, 21.2f * u, 36.5f * u, 18 * u)
            }
            drawPath(arc, white, style = Stroke(width = 3.6f * u, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ServerCard(connected: Boolean, serverIp: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .alpha(if (connected) 1f else 0.42f)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(AppColors.CardBg, RoundedCornerShape(16.dp))
            .then(if (connected) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(com.iranconnection.app.R.drawable.ic_iran_flag),
            contentDescription = "Iran",
            modifier = Modifier.size(42.dp).clip(CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Text("Iran", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
            if (connected && serverIp != null) {
                Row(
                    Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text("IP $serverIp", fontSize = 12.sp, color = AppColors.TextMuted)
                    Text("•", fontSize = 12.sp, color = AppColors.Chevron)
                    Text("164ms", fontSize = 12.sp, color = AppColors.TextMuted)
                    SignalBars(3)
                }
            } else {
                RedactedInfoRow()
            }
        }
        if (connected) {
            Canvas(Modifier.size(8.dp, 14.dp)) {
                val w = size.width / 8f
                val h = size.height / 14f
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(1.5f * w, 1.5f * h); lineTo(6.5f * w, 7f * h); lineTo(1.5f * w, 12.5f * h)
                }
                drawPath(p, AppColors.Chevron, style = Stroke(width = 1.8f.dp.toPx(), cap = StrokeCap.Round))
            }
        } else {
            LockIcon()
        }
    }
}

/** Animated shimmer placeholder bars shown instead of IP info when disconnected. */
@Composable
private fun RedactedInfoRow() {
    val t = rememberInfiniteTransition(label = "shimmer")
    val shimmer by t.animateFloat(
        0.25f, 0.5f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmerAlpha",
    )
    val barColor = Color(0xFF3A3A4C).copy(alpha = shimmer)
    Row(
        Modifier.padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(46.dp, 7.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
        Box(Modifier.size(5.dp).clip(CircleShape).background(barColor.copy(alpha = shimmer * 0.6f)))
        Box(Modifier.size(34.dp, 7.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
        Box(Modifier.size(5.dp).clip(CircleShape).background(barColor.copy(alpha = shimmer * 0.6f)))
        Box(Modifier.size(22.dp, 7.dp).clip(RoundedCornerShape(4.dp)).background(barColor))
    }
}

/** Lock icon drawn with Canvas, shown in the server card when disconnected. */
@Composable
private fun LockIcon() {
    Canvas(Modifier.size(10.dp, 13.dp)) {
        val w = size.width
        val h = size.height
        val lockColor = Color(0xFF4A4A5C)
        // Shackle (arc)
        drawArc(
            color = lockColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, 0f),
            size = Size(w * 0.68f, h * 0.52f),
            style = Stroke(width = w * 0.22f, cap = StrokeCap.Round),
        )
        // Body (rounded rect)
        drawRoundRect(
            color = lockColor,
            topLeft = Offset(0f, h * 0.44f),
            size = Size(w, h * 0.56f),
            cornerRadius = CornerRadius(w * 0.2f),
        )
    }
}

private fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

// =====================================================================
// Premium upsell modal (opened from the Home "Premium" badge)
// =====================================================================
private data class PremiumCard(
    val bank: String,
    val number: String,
    val holder: String,
    val amount: String,
    val gradient: List<Color>,
    val shadow: Color,
)

private val PREMIUM_USD = PremiumCard(
    bank = "USD Bank",
    number = "4937 2420 2574 6817",
    holder = "LALEH MANSOURI",
    amount = "$2.50 USD",
    gradient = listOf(Color(0xFFE63950), Color(0xFFB01030), Color(0xFF7A0C24)),
    shadow = Color(0x5CC81838),
)
private val PREMIUM_TMN = PremiumCard(
    bank = "Iranian Bank",
    number = "6219 8618 0150 9695",
    holder = "SHAHRAM OVEISI",
    amount = "300,000 TMN",
    gradient = listOf(Color(0xFF2D8FD8), Color(0xFF1868B2), Color(0xFF0C3D78)),
    shadow = Color(0x5C1878C8),
)

@Composable
private fun PremiumModal(onDismiss: () -> Unit, onGoToLogin: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var currency by remember { mutableStateOf("usd") }
    var copied by remember { mutableStateOf(false) }
    val card = if (currency == "usd") PREMIUM_USD else PREMIUM_TMN

    LaunchedEffect(copied) {
        if (copied) { kotlinx.coroutines.delay(2000); copied = false }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFFF4F6FA))
                .verticalScroll(rememberScrollState()),
        ) {
            // Gradient hero header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFBCF42), Color(0xFFF5A620), Color(0xFFE48808)),
                            start = Offset(0f, 0f), end = Offset(600f, 300f),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            ) {
                // close
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Canvas(Modifier.size(24.dp, 19.dp)) {
                            val w = size.width / 20f; val h = size.height / 16f
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(1.5f * w, 14f * h); lineTo(4.5f * w, 5.5f * h); lineTo(8.5f * w, 10.5f * h)
                                lineTo(10f * w, 1.5f * h); lineTo(11.5f * w, 10.5f * h); lineTo(15.5f * w, 5.5f * h)
                                lineTo(18.5f * w, 14f * h); close()
                            }
                            drawPath(p, Color.White)
                        }
                        Text("Premium Subscription", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sign up and log in to purchase a Premium subscription and get full access to all servers and apps.",
                        fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.95f), lineHeight = 19.sp,
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Currency tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    CurrencyTab(
                        modifier = Modifier.weight(1f),
                        active = currency == "usd",
                        top = "USD · Dollar",
                        amount = "$2.50",
                        bottom = "per month",
                        onClick = { currency = "usd" },
                    )
                    CurrencyTab(
                        modifier = Modifier.weight(1f),
                        active = currency == "tmn",
                        top = "TMN · Toman",
                        amount = "300,000",
                        bottom = "per month",
                        onClick = { currency = "tmn" },
                    )
                }

                // Bank card (switches with the selected currency)
                PremiumBankCard(
                    card = card,
                    copied = copied,
                    onCopy = { clipboard.setText(AnnotatedString(card.number.replace(" ", ""))); copied = true },
                )

                // Closing instructions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF279491).copy(alpha = 0.10f))
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("ℹ", fontSize = 14.sp, color = Color(0xFF279491))
                    Text(
                        "After transferring the amount to the card above, log in and submit your payment receipt in the Profile section to activate your subscription.",
                        fontSize = 11.5.sp, color = Color(0xFF1B6B68), lineHeight = 18.sp, fontWeight = FontWeight.Medium,
                    )
                }

                // Login CTA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF4ECAC5), Color(0xFF279491))))
                        .clickable { onGoToLogin() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Sign in / Sign up to continue", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                Text(
                    "Continue later",
                    fontSize = 12.sp, color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(vertical = 2.dp),
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun CurrencyTab(
    modifier: Modifier,
    active: Boolean,
    top: String,
    amount: String,
    bottom: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (active) Color.White else Color(0xFFEAEDF2))
            .border(
                width = 1.5.dp,
                color = if (active) Color(0xFFF5A620) else Color(0xFFEAEDF2),
                shape = RoundedCornerShape(13.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(top, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = if (active) Color(0xFF9A9A9A) else Color(0xFF8A93A5))
        Text(amount, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = if (active) Color(0xFFB87209) else Color(0xFF6B7A99))
        Text(bottom, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = if (active) Color(0xFFC8A060) else Color(0xFFA0AAB8))
    }
}

@Composable
private fun PremiumBankCard(card: PremiumCard, copied: Boolean, onCopy: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(20.dp), spotColor = card.shadow)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(card.gradient, start = Offset(0f, 0f), end = Offset(500f, 400f)))
            .padding(18.dp),
    ) {
        Box(
            modifier = Modifier.size(120.dp).align(Alignment.TopEnd).offset(x = 30.dp, y = (-30).dp)
                .clip(CircleShape).border(24.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        )
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text("TRANSFER TO", fontSize = 8.5.sp, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp)
                    Text(card.bank, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                        modifier = Modifier.padding(top = 2.dp))
                }
                Text(card.amount, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.height(18.dp))
            Text(card.number, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.White,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.5.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("CARD HOLDER", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.8.sp, modifier = Modifier.padding(bottom = 3.dp))
                    Text(card.holder, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.26f), RoundedCornerShape(8.dp))
                        .clickable { onCopy() }
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                ) {
                    Text(if (copied) "✓ Copied" else "Copy number", fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}
