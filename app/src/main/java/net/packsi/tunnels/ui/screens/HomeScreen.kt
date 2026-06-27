package net.packsi.tunnels.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import net.packsi.tunnels.ConfigFetchStatus
import net.packsi.tunnels.data.auth.AuthViewModel
import net.packsi.tunnels.ui.components.CountryFlag
import net.packsi.tunnels.ui.components.SignalBars
import net.packsi.tunnels.ui.components.WorldMap
import net.packsi.tunnels.ui.theme.AppColors

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
    onGoToPayment: () -> Unit = {},
    configStatus: ConfigFetchStatus = ConfigFetchStatus.Success,
    buttonEnabled: Boolean = true,
    errorMessage: String? = null,
    browserVpnEnabled: Boolean = false,
    onBrowserVpnChange: (Boolean) -> Unit = {},
    reconnecting: Boolean = false,
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
        Column(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(connected, accent)
                Spacer(Modifier.size(7.dp))
                Text(
                    statusLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    // Amber while reconnecting to flag the transient churn without the red/disconnect look.
                    color = if (reconnecting) AppColors.Gold else accent,
                )
            }
            if (errorMessage != null) {
                Text(
                    errorMessage,
                    fontSize = 11.sp,
                    color = AppColors.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 3.dp),
                )
            }
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

        BrowserToggleRow(
            enabled = browserVpnEnabled,
            connected = connected,
            premium = isPremium,
            onChange = onBrowserVpnChange,
            onLockedClick = { showPremiumModal = true },
        )
        ServerCard(connected = connected, serverIp = serverIp, onClick = onServerCardClick)
        Spacer(Modifier.height(12.dp))
    }

    if (showPremiumModal) {
        PremiumModal(
            onDismiss = { showPremiumModal = false },
            onGoToPayment = { showPremiumModal = false; onGoToPayment() },
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
private fun BrowserToggleRow(
    enabled: Boolean,
    connected: Boolean,
    premium: Boolean,
    onChange: (Boolean) -> Unit,
    onLockedClick: () -> Unit,
) {
    // The toggle is a premium feature and only meaningful while the tunnel is up.
    val interactive = connected && premium
    val trackColor by animateColorAsState(
        targetValue = if (enabled) AppColors.Teal else Color(0xFF9494A8),
        animationSpec = tween(220),
        label = "track",
    )
    val thumbX by animateDpAsState(
        targetValue = if (enabled) 24.dp else 2.dp,
        animationSpec = tween(220),
        label = "thumb",
    )
    val iconColor = if (enabled) AppColors.Teal else Color(0xFF8888A0)

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 10.dp)
            .alpha(if (interactive) 1f else 0.45f)
            .shadow(6.dp, RoundedCornerShape(16.dp))
            .background(AppColors.CardBg, RoundedCornerShape(16.dp))
            .then(
                when {
                    interactive -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onChange(!enabled) }
                    // Connected but not premium → tapping nudges the user to upgrade.
                    connected && !premium -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onLockedClick() }
                    else -> Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) AppColors.Teal.copy(alpha = 0.14f) else Color(0xFF2A2A3C)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val er = size.width * 0.35f
                    val sw = 1.7.dp.toPx()
                    // "e" body arc
                    drawArc(
                        color = iconColor,
                        startAngle = 35f,
                        sweepAngle = 290f,
                        useCenter = false,
                        topLeft = Offset(cx - er, cy - er),
                        size = Size(er * 2f, er * 2f),
                        style = Stroke(width = sw, cap = StrokeCap.Round),
                    )
                    // "e" horizontal bar
                    drawLine(iconColor, Offset(cx - er * 0.9f, cy), Offset(cx + er * 0.7f, cy), sw, StrokeCap.Round)
                    // orbital ring (rotated ellipse)
                    rotate(-35f) {
                        drawOval(
                            color = iconColor,
                            topLeft = Offset(cx - er * 1.38f, cy - er * 0.45f),
                            size = Size(er * 2.76f, er * 0.9f),
                            style = Stroke(width = sw * 0.75f),
                        )
                    }
                }
            }
            Column {
                Text(
                    "Should browsers use tunneling?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                )
                Text(
                    if (premium) "Chrome · Firefox · Edge · Brave" else "Premium feature — upgrade to enable",
                    fontSize = 11.sp,
                    color = if (premium) AppColors.TextMuted else AppColors.Gold,
                )
            }
        }
        Box(
            Modifier
                .width(52.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(trackColor),
        ) {
            Box(
                Modifier
                    .offset(x = thumbX, y = 2.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
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
            painter = androidx.compose.ui.res.painterResource(net.packsi.tunnels.R.drawable.ic_iran_flag),
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
@Composable
private fun PremiumModal(onDismiss: () -> Unit, onGoToPayment: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 400.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF4F6FA)),
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFBCF42), Color(0xFFF5A620), Color(0xFFE48808)),
                            start = Offset(0f, 0f), end = Offset(600f, 200f),
                        ),
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White) }

                Column {
                    Text("⭐ Upgrade to Premium", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Full access to all servers and apps.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PlanPill(modifier = Modifier.weight(1f), name = "PRO", usd = "$3", tmn = "500,000", color = Color(0xFF279491))
                    PlanPill(modifier = Modifier.weight(1f), name = "Premium", usd = "$5", tmn = "700,000", color = Color(0xFFF59E0B))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF279491).copy(alpha = 0.09f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("ℹ", fontSize = 13.sp, color = Color(0xFF279491))
                    Text(
                        "Log in, then go to Profile → Payment to submit your receipt and activate.",
                        fontSize = 11.sp, color = Color(0xFF1B6B68), lineHeight = 17.sp, fontWeight = FontWeight.Medium,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF4ECAC5), Color(0xFF279491))))
                        .clickable { onGoToPayment() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Go to Payment", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }

                Text(
                    "Later",
                    fontSize = 12.sp, color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onDismiss() }.padding(vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanPill(modifier: Modifier, name: String, usd: String, tmn: String, color: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(usd, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF18182A))
        Text("$tmn تومان", fontSize = 10.sp, color = Color(0xFF6B7A99))
        Text("/ month", fontSize = 9.sp, color = Color(0xFF9AA3B2))
    }
}
