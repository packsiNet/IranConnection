package net.packsi.tunnels.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ─────────────────────────── palette (from design) ───────────────────────────
private val TealLight = Color(0xFF3DBFBA)
private val TealMid = Color(0xFF279491)
private val TealDark = Color(0xFF195E5C)
private val BtnTealTop = Color(0xFF4ECAC5)
private val InkDark = Color(0xFF18182A)
private val InkGray = Color(0xFF94A0B4)
private val LabelGray = Color(0xFFC0C8D4)
private val Divider = Color(0xFFF0F2F6)
private val RedBadgeBg = Color(0xFFFEF2F2)
private val Red = Color(0xFFEF4444)
private val GreenBadgeBg = Color(0xFFF0FDF4)
private val Green = Color(0xFF16A34A)
private val ChipTeal = Color(0xFFEAF8F8)
private val StoreBg = Color(0xFFF4F5F8)
private val StoreBorder = Color(0xFFE8EBF0)
private val StoreText = Color(0xFFB0BAC8)

private val heroGradient = Brush.linearGradient(listOf(TealLight, TealMid, TealDark))
private val ctaGradient = Brush.linearGradient(listOf(BtnTealTop, TealMid))

/** A changelog entry: a short title + supporting line + which glyph to draw. */
private data class Change(val title: String, val subtitle: String, val glyph: Glyph)

private val MAJOR_CHANGES = listOf(
    Change("New Protocol Engine", "Redesigned core with AES-256-GCM encryption standard", Glyph.SHIELD),
    Change("3× Faster Routing", "Smart routing reduces latency by up to 65% across all servers", Glyph.BOLT),
    Change("Multi-Hop Protection", "Route through 2 servers simultaneously for maximum privacy", Glyph.LOCK),
)

private val MINOR_CHANGES = listOf(
    Change("Better Battery Efficiency", "Background processes optimized — 30% less drain", Glyph.BATTERY),
    Change("Bug Fixes & Stability", "Fixed reconnection loop and server timeout issues", Glyph.CHECK),
    Change("UI Refinements", "Smoother animations and improved Persian font rendering", Glyph.SLIDERS),
)

/**
 * Update gate. [isMajor] = mandatory full-screen modal the user cannot dismiss; otherwise a
 * dismissible bottom sheet ("Remind Me Later"). Mirrors the two frames in the design doc.
 */
@Composable
fun UpdateDialog(
    newVersion: String,
    downloadUrl: String,
    isMajor: Boolean,
    onDismiss: () -> Unit,
) {
    if (isMajor) {
        MajorUpdateDialog(newVersion = newVersion, downloadUrl = downloadUrl)
    } else {
        MinorUpdateSheet(newVersion = newVersion, downloadUrl = downloadUrl, onDismiss = onDismiss)
    }
}

// ─────────────────────────── MAJOR (forced, full screen) ───────────────────────────
@Composable
private fun MajorUpdateDialog(newVersion: String, downloadUrl: String) {
    Dialog(
        onDismissRequest = { /* mandatory — cannot dismiss */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            // Teal hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(heroGradient)
                    .padding(vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(36.dp)) { drawAppGlyph() }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IranConnection", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Version $newVersion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                // Required badge
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(RedBadgeBg)
                            .padding(horizontal = 13.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Canvas(Modifier.size(11.dp)) { drawGlyph(Glyph.LOCK, Red) }
                        Text("UPDATE REQUIRED", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Red)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "A Major Update Is Ready",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkDark,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    "This critical update includes security patches and core architectural changes. You must update to continue using the app.",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 12.sp,
                    color = InkGray,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                Spacer(Modifier.height(14.dp))
                SectionLabel()
                Spacer(Modifier.height(10.dp))
                MAJOR_CHANGES.forEach { c ->
                    ChangelogRow(c, major = true)
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Fixed CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 1.dp),
            ) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    DownloadButton(downloadUrl)
                    Spacer(Modifier.height(8.dp))
                    StoreButtonsRow()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mandatory · App will restart after installation",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 10.sp,
                        color = LabelGray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ─────────────────────────── MINOR (optional bottom sheet) ───────────────────────────
@Composable
private fun MinorUpdateSheet(newVersion: String, downloadUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x85080A14))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visibleState = remember { androidx.compose.animation.core.MutableTransitionState(false).apply { targetState = true } },
                enter = slideInVertically(animationSpec = tween(420)) { it } + fadeIn(tween(420)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        // swallow taps so they don't fall through to the scrim
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {},
                ) {
                    // Handle
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), horizontalArrangement = Arrangement.Center) {
                        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFDDE1EA)))
                    }
                    Column(Modifier.padding(horizontal = 20.dp).padding(top = 4.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(heroGradient),
                                contentAlignment = Alignment.Center,
                            ) {
                                Canvas(Modifier.size(24.dp)) { drawAppGlyph() }
                            }
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    Text("New Update Available", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = InkDark)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GreenBadgeBg)
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text("v $newVersion", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Green)
                                    }
                                }
                                Text("IranConnection", fontSize = 11.sp, color = InkGray, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(13.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
                        Spacer(Modifier.height(13.dp))
                        SectionLabel()
                        Spacer(Modifier.height(10.dp))
                        MINOR_CHANGES.forEach { c ->
                            ChangelogRow(c, major = false)
                            Spacer(Modifier.height(9.dp))
                        }
                        Spacer(Modifier.height(7.dp))
                        DownloadButton(downloadUrl)
                        Spacer(Modifier.height(7.dp))
                        StoreButtonsRow()
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDismiss() }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Remind Me Later", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = InkGray)
                        }
                    }
                    // Home indicator
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.Center) {
                        Box(Modifier.size(width = 110.dp, height = 4.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFC4CCD8)))
                    }
                }
            }
        }
    }
}

// ─────────────────────────── shared pieces ───────────────────────────
@Composable
private fun SectionLabel() {
    Text("WHAT'S NEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LabelGray)
}

@Composable
private fun ChangelogRow(change: Change, major: Boolean) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(if (major) 10.dp else 9.dp)) {
        val boxSize = if (major) 34.dp else 30.dp
        Box(
            modifier = Modifier
                .size(boxSize)
                .clip(RoundedCornerShape(if (major) 10.dp else 9.dp))
                .background(if (major) ctaGradient else androidx.compose.ui.graphics.SolidColor(ChipTeal)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(if (major) 16.dp else 14.dp)) {
                drawGlyph(change.glyph, if (major) Color.White else TealLight)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(change.title, fontSize = if (major) 12.5.sp else 12.sp, fontWeight = FontWeight.Bold, color = InkDark)
            Text(
                change.subtitle,
                fontSize = 10.5.sp,
                color = InkGray,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

@Composable
private fun DownloadButton(downloadUrl: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ctaGradient)
            .clickable(enabled = downloadUrl.isNotEmpty()) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
            }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(15.dp)) { drawGlyph(Glyph.DOWNLOAD, Color.White) }
            Text("Direct Download", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
private fun StoreButtonsRow() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        StoreButton("Google Play", Modifier.weight(1f))
        StoreButton("App Store", Modifier.weight(1f))
    }
}

@Composable
private fun StoreButton(label: String, modifier: Modifier) {
    // Disabled by design — direct download is the only active path.
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(StoreBg)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = StoreText)
    }
}

// ─────────────────────────── Canvas glyphs (no icon dep) ───────────────────────────
private enum class Glyph { LOCK, BOLT, SHIELD, BATTERY, CHECK, SLIDERS, DOWNLOAD }

/** App logo mark — globe + meridians, white strokes. Used on hero/header (draws in given size). */
private fun DrawScope.drawAppGlyph() {
    val w = size.width
    val s = Stroke(width = w * 0.045f, cap = StrokeCap.Round)
    val white = Color.White
    drawCircle(white, radius = w * 0.33f, center = center, style = s)
    // vertical meridian
    val p1 = Path().apply {
        moveTo(w * 0.5f, w * 0.17f)
        cubicTo(w * 0.30f, w * 0.38f, w * 0.30f, w * 0.62f, w * 0.5f, w * 0.83f)
    }
    val p2 = Path().apply {
        moveTo(w * 0.5f, w * 0.17f)
        cubicTo(w * 0.70f, w * 0.38f, w * 0.70f, w * 0.62f, w * 0.5f, w * 0.83f)
    }
    drawPath(p1, white, style = s)
    drawPath(p2, white, style = s)
    drawLine(white.copy(alpha = 0.55f), Offset(w * 0.17f, w * 0.5f), Offset(w * 0.83f, w * 0.5f), strokeWidth = w * 0.04f, cap = StrokeCap.Round)
}

private fun DrawScope.drawGlyph(glyph: Glyph, tint: Color) {
    val w = size.width
    val sw = w * 0.09f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when (glyph) {
        Glyph.LOCK -> {
            drawRoundRect(
                tint, topLeft = Offset(w * 0.22f, w * 0.45f), size = Size(w * 0.56f, w * 0.42f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f), style = stroke,
            )
            val sh = Path().apply {
                moveTo(w * 0.32f, w * 0.45f)
                lineTo(w * 0.32f, w * 0.32f)
                cubicTo(w * 0.32f, w * 0.14f, w * 0.68f, w * 0.14f, w * 0.68f, w * 0.32f)
                lineTo(w * 0.68f, w * 0.45f)
            }
            drawPath(sh, tint, style = stroke)
            drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.5f, w * 0.64f))
        }
        Glyph.BOLT -> {
            val p = Path().apply {
                moveTo(w * 0.56f, w * 0.1f)
                lineTo(w * 0.28f, w * 0.55f)
                lineTo(w * 0.48f, w * 0.55f)
                lineTo(w * 0.44f, w * 0.9f)
                lineTo(w * 0.72f, w * 0.42f)
                lineTo(w * 0.52f, w * 0.42f)
                close()
            }
            drawPath(p, tint, style = stroke)
        }
        Glyph.SHIELD -> {
            val p = Path().apply {
                moveTo(w * 0.5f, w * 0.12f)
                lineTo(w * 0.82f, w * 0.28f)
                lineTo(w * 0.82f, w * 0.55f)
                cubicTo(w * 0.82f, w * 0.75f, w * 0.5f, w * 0.9f, w * 0.5f, w * 0.9f)
                cubicTo(w * 0.5f, w * 0.9f, w * 0.18f, w * 0.75f, w * 0.18f, w * 0.55f)
                lineTo(w * 0.18f, w * 0.28f)
                close()
            }
            drawPath(p, tint, style = stroke)
            drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.5f, w * 0.5f), style = Stroke(width = sw * 0.85f))
        }
        Glyph.BATTERY -> {
            drawRoundRect(
                tint, topLeft = Offset(w * 0.15f, w * 0.3f), size = Size(w * 0.6f, w * 0.4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f), style = stroke,
            )
            drawLine(tint, Offset(w * 0.82f, w * 0.42f), Offset(w * 0.82f, w * 0.58f), strokeWidth = sw, cap = StrokeCap.Round)
        }
        Glyph.CHECK -> {
            drawCircle(tint, radius = w * 0.36f, center = center, style = Stroke(width = sw))
            val p = Path().apply {
                moveTo(w * 0.34f, w * 0.52f)
                lineTo(w * 0.46f, w * 0.64f)
                lineTo(w * 0.68f, w * 0.4f)
            }
            drawPath(p, tint, style = stroke)
        }
        Glyph.SLIDERS -> {
            drawLine(tint, Offset(w * 0.2f, w * 0.36f), Offset(w * 0.8f, w * 0.36f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(tint, Offset(w * 0.2f, w * 0.64f), Offset(w * 0.8f, w * 0.64f), strokeWidth = sw, cap = StrokeCap.Round)
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.62f, w * 0.36f))
            drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.38f, w * 0.64f))
        }
        Glyph.DOWNLOAD -> {
            drawLine(tint, Offset(w * 0.5f, w * 0.14f), Offset(w * 0.5f, w * 0.64f), strokeWidth = sw * 1.2f, cap = StrokeCap.Round)
            val a = Path().apply {
                moveTo(w * 0.34f, w * 0.48f)
                lineTo(w * 0.5f, w * 0.64f)
                lineTo(w * 0.66f, w * 0.48f)
            }
            drawPath(a, tint, style = Stroke(width = sw * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(tint, Offset(w * 0.18f, w * 0.86f), Offset(w * 0.82f, w * 0.86f), strokeWidth = sw * 1.2f, cap = StrokeCap.Round)
        }
    }
}
