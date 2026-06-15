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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onHamburgerClick: () -> Unit = {},
    buttonEnabled: Boolean = true,
) {
    val accent by animateColorAsState(if (connected) AppColors.Teal else AppColors.Red, label = "accent")

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.ScreenBg),
    ) {
        HomeHeader(onHamburgerClick = onHamburgerClick)

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
}

@Composable
private fun HomeHeader(onHamburgerClick: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Hamburger
        Box(
            Modifier
                .size(44.dp)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .background(AppColors.CardBg, RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHamburgerClick,
                ),
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
        // Premium badge
        Row(
            Modifier
                .shadow(4.dp, RoundedCornerShape(24.dp))
                .background(AppColors.CardBg, RoundedCornerShape(24.dp))
                .padding(start = 12.dp, end = 16.dp, top = 9.dp, bottom = 9.dp),
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
        CountryFlag("ir", size = 42.dp)
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
