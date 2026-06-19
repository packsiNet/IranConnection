package com.iranconnection.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Palette ───────────────────────────────────────────────────────────────────
private val ScBg       = Color(0xFFF0F2F6)
private val Teal       = Color(0xFF3DBFBA)
private val TealMid    = Color(0xFF279491)
private val TealDeep   = Color(0xFF195E5C)
private val TealLight  = Color(0xFF4ECAC5)
private val ErrorRed   = Color(0xFFE53935)
private val ErrorLight = Color(0xFFEF9A9A)
private val TextHigh   = Color(0xFF18182A)
private val TextLow    = Color(0xFFA0AAB8)
private val TextOff    = Color(0xFFC0C8D4)
private val TextGhost  = Color(0xFFD8DCE4)
private val IdleBg     = Color(0xFFEDEEF2)
private val IdleBdr    = Color(0xFFE0E3EA)
private val BarBg      = Color(0xFFE4E7EE)

// ── Step definitions (5 steps, must match TOTAL_STEPS in DeviceAuthViewModel) ─
private data class StepDef(val label: String, val sub: String, val badge: String)

private val STEPS = listOf(
    StepDef("Connecting to server",   "Establishing encrypted tunnel",  "OK"),
    StepDef("Authenticating session", "Verifying credentials & token",  "AUTH"),
    StepDef("Fetching configuration", "Receiving VPN server config",    "CFG"),
    StepDef("Detecting Persian apps", "Syncing supported app catalog",  "APPS"),
    StepDef("Applying routing rules", "Optimizing for your network",    "READY"),
)

private enum class SStep { PENDING, ACTIVE, DONE, FAILED }

// ── Public composable ─────────────────────────────────────────────────────────
/**
 * @param completedSteps driven by DeviceAuthViewModel (0–5)
 * @param failedStep     index of the step that failed, null = none
 * @param isError        true when a fatal error occurred
 * @param onRetry        called when user taps "Try Again"
 * @param onSkip         called when user taps "Continue anyway" (enter app despite error)
 */
@Composable
fun SplashScreen(
    message: String = "",
    isError: Boolean = false,
    onRetry: (() -> Unit)? = null,
    onSkip: (() -> Unit)? = null,
    completedSteps: Int = 0,
    failedStep: Int? = null,
) {
    val targetProgress = completedSteps.coerceAtMost(STEPS.size).toFloat() / STEPS.size
    val progress = remember { Animatable(0f) }

    LaunchedEffect(completedSteps) {
        progress.animateTo(targetProgress, tween(350, easing = LinearEasing))
    }

    val allDone = completedSteps >= STEPS.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        LogoSection()
        StepsList(completedSteps = completedSteps, failedStep = failedStep, allDone = allDone)
        ProgressSection(
            progress = progress.value,
            completedSteps = completedSteps,
            failedStep = failedStep,
            allDone = allDone,
            isError = isError,
            errorMessage = message.ifBlank { "Failed to connect to server. Please check your connection." },
            onRetry = onRetry,
            onSkip = onSkip,
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ── Logo + subtitle ───────────────────────────────────────────────────────────
@Composable
private fun LogoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LogoWithRipple()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("IranConnection", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextHigh, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(4.dp))
            Text("Secure · Fast · Reliable", fontSize = 11.sp, color = TextLow, letterSpacing = 0.5.sp)
        }
    }
}

// ── Logo + ripple rings ───────────────────────────────────────────────────────
@Composable
private fun LogoWithRipple() {
    val inf = rememberInfiniteTransition(label = "logo")
    val r1Scale by inf.animateFloat(0.8f, 2.2f, infiniteRepeatable(tween(2400, easing = LinearEasing)), "r1s")
    val r1Alpha by inf.animateFloat(0.50f, 0f, infiniteRepeatable(tween(2400, easing = LinearEasing)), "r1a")
    val r2Scale by inf.animateFloat(0.8f, 2.2f, infiniteRepeatable(tween(2400, delayMillis = 800, easing = LinearEasing)), "r2s")
    val r2Alpha by inf.animateFloat(0.35f, 0f, infiniteRepeatable(tween(2400, delayMillis = 800, easing = LinearEasing)), "r2a")
    val pulse   by inf.animateFloat(1f, 1.05f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), "p")

    Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val base = size.minDimension * 0.277f
            val sw = 1.5.dp.toPx()
            drawCircle(Teal.copy(alpha = r1Alpha), base * r1Scale, androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(sw))
            drawCircle(Teal.copy(alpha = r2Alpha), base * r2Scale, androidx.compose.ui.geometry.Offset(cx, cy), style = Stroke(sw))
        }
        Box(
            modifier = Modifier.size(72.dp).scale(pulse).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(TealLight, TealMid, TealDeep))),
            contentAlignment = Alignment.Center,
        ) { GlobeIcon() }
    }
}

// ── Globe icon ────────────────────────────────────────────────────────────────
@Composable
private fun GlobeIcon() {
    Canvas(Modifier.size(38.dp)) {
        val sc = size.width / 38f
        val c  = androidx.compose.ui.geometry.Offset(19f * sc, 19f * sc)
        val sw = Stroke(1.6f * sc, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(Color.White, 12.5f * sc, c, style = sw)

        drawPath(Path().apply {
            moveTo(19f*sc, 6.5f*sc)
            cubicTo(16.8f*sc, 10.4f*sc, 14.8f*sc, 14.5f*sc, 14.8f*sc, 19f*sc)
            cubicTo(14.8f*sc, 23.5f*sc, 16.8f*sc, 27.6f*sc, 19f*sc, 31.5f*sc)
        }, Color.White, style = sw)

        drawPath(Path().apply {
            moveTo(19f*sc, 6.5f*sc)
            cubicTo(21.2f*sc, 10.4f*sc, 23.2f*sc, 14.5f*sc, 23.2f*sc, 19f*sc)
            cubicTo(23.2f*sc, 23.5f*sc, 21.2f*sc, 27.6f*sc, 19f*sc, 31.5f*sc)
        }, Color.White, style = sw)

        drawLine(Color.White.copy(0.55f), androidx.compose.ui.geometry.Offset(6.5f*sc, 19f*sc), androidx.compose.ui.geometry.Offset(31.5f*sc, 19f*sc), 1.4f*sc, StrokeCap.Round)
        drawLine(Color.White.copy(0.30f), androidx.compose.ui.geometry.Offset(8f*sc, 13f*sc),   androidx.compose.ui.geometry.Offset(30f*sc, 13f*sc),   1.1f*sc, StrokeCap.Round)
        drawLine(Color.White.copy(0.30f), androidx.compose.ui.geometry.Offset(8f*sc, 25f*sc),   androidx.compose.ui.geometry.Offset(30f*sc, 25f*sc),   1.1f*sc, StrokeCap.Round)
    }
}

// ── Steps list ────────────────────────────────────────────────────────────────
@Composable
private fun StepsList(completedSteps: Int, failedStep: Int?, allDone: Boolean) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        STEPS.forEachIndexed { i, step ->
            val state = when {
                i == failedStep                    -> SStep.FAILED
                i < completedSteps                 -> SStep.DONE
                i == completedSteps && !allDone    -> SStep.ACTIVE
                else                               -> SStep.PENDING
            }
            StepRow(step, state)
        }
    }
}

@Composable
private fun StepRow(step: StepDef, state: SStep) {
    val rowBg = when (state) {
        SStep.ACTIVE -> Teal.copy(alpha = 0.06f)
        SStep.FAILED -> ErrorRed.copy(alpha = 0.04f)
        else         -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(36.dp).then(when (state) {
                SStep.DONE    -> Modifier.background(Brush.linearGradient(listOf(Teal, TealMid)), RoundedCornerShape(11.dp))
                SStep.FAILED  -> Modifier.background(Brush.linearGradient(listOf(ErrorRed, Color(0xFFC62828))), RoundedCornerShape(11.dp))
                SStep.ACTIVE  -> Modifier.background(Teal.copy(0.12f), RoundedCornerShape(11.dp)).border(1.5.dp, Teal.copy(0.30f), RoundedCornerShape(11.dp))
                SStep.PENDING -> Modifier.background(IdleBg, RoundedCornerShape(11.dp)).border(1.5.dp, IdleBdr, RoundedCornerShape(11.dp))
            }),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                SStep.DONE    -> CheckIcon()
                SStep.FAILED  -> CrossIcon()
                SStep.ACTIVE  -> SpinnerIcon()
                SStep.PENDING -> Box(Modifier.size(6.dp).background(Color.White.copy(0.28f), CircleShape))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                step.label,
                fontSize = 12.5.sp,
                fontWeight = when (state) { SStep.ACTIVE -> FontWeight.Bold; SStep.DONE, SStep.FAILED -> FontWeight.SemiBold; else -> FontWeight.Medium },
                color = when (state) { SStep.FAILED -> ErrorRed; SStep.PENDING -> TextOff; else -> TextHigh },
                lineHeight = 16.sp,
            )
            Text(
                step.sub,
                fontSize = 9.5.sp,
                color = when (state) { SStep.DONE -> TextLow; SStep.ACTIVE -> Teal; SStep.FAILED -> ErrorRed.copy(0.7f); else -> TextGhost },
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        when (state) {
            SStep.DONE   -> Text(step.badge, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Teal)
            SStep.FAILED -> Text("ERR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
            else         -> {}
        }
    }
}

// ── Progress bar + button ─────────────────────────────────────────────────────
@Composable
private fun ProgressSection(
    progress: Float,
    completedSteps: Int,
    failedStep: Int?,
    allDone: Boolean,
    isError: Boolean,
    errorMessage: String,
    onRetry: (() -> Unit)?,
    onSkip: (() -> Unit)?,
) {
    val hasNonFatalFail = failedStep != null && !isError
    val buttonEnabled   = allDone || isError

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Progress bar
        Box(
            Modifier.fillMaxWidth().height(4.dp)
                .clip(RoundedCornerShape(3.dp)).background(BarBg)
        ) {
            val barBrush = when {
                isError         -> Brush.horizontalGradient(listOf(ErrorRed, ErrorLight))
                hasNonFatalFail -> Brush.horizontalGradient(listOf(Teal, Color(0xFFFFB74D)))
                else            -> Brush.horizontalGradient(listOf(Teal, Color(0xFF68D0CA)))
            }
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(3.dp)).background(barBrush))
        }

        // Status row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(progress * 100).toInt()}%", fontSize = 10.sp, color = TextLow)
            Text(
                text = when {
                    isError                         -> "Error"
                    hasNonFatalFail                 -> "Warning"
                    allDone                         -> "Ready"
                    completedSteps < STEPS.size     -> STEPS[completedSteps].label
                    else                            -> "Initializing…"
                },
                fontSize = 10.sp,
                color = when { isError -> ErrorRed; hasNonFatalFail -> Color(0xFFFF8F00); allDone -> Teal; else -> TextLow },
                fontWeight = if (buttonEnabled || hasNonFatalFail) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        // Error message (fatal only)
        if (isError) {
            Text(
                errorMessage,
                fontSize = 11.sp,
                color = ErrorRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        // Primary button — always visible, disabled during loading
        Button(
            onClick = { if (isError) onRetry?.invoke() else {} },
            enabled = buttonEnabled,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isError) Color(0xFFE53935) else Teal,
                disabledContainerColor = Color(0xFFD0D5DD),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(0.6f),
            ),
        ) {
            Text(
                text = when {
                    !buttonEnabled -> "Loading…"
                    isError        -> "Try Again"
                    else           -> "Enter App  →"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Skip link — small text below button, only on error
        if (isError && onSkip != null) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    "Continue to app anyway",
                    fontSize = 11.sp,
                    color = TextLow,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

// ── Small icons ───────────────────────────────────────────────────────────────
@Composable
private fun CheckIcon() {
    Canvas(Modifier.size(16.dp)) {
        drawPath(Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.53f)
            lineTo(size.width * 0.375f, size.height * 0.75f)
            lineTo(size.width * 0.84f,  size.height * 0.25f)
        }, Color.White, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CrossIcon() {
    Canvas(Modifier.size(14.dp)) {
        val p = 0.2f
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width*p, size.height*p),     androidx.compose.ui.geometry.Offset(size.width*(1-p), size.height*(1-p)), 2.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(size.width*(1-p), size.height*p), androidx.compose.ui.geometry.Offset(size.width*p, size.height*(1-p)),     2.dp.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun SpinnerIcon() {
    val rot by rememberInfiniteTransition(label = "spin").animateFloat(
        0f, 360f, infiniteRepeatable(tween(800, easing = LinearEasing)), "rot",
    )
    Canvas(Modifier.size(16.dp).rotate(rot)) {
        val sw = 2.dp.toPx()
        drawArc(Color.White.copy(0.25f), 0f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round))
        drawArc(Color.White, -90f, 90f, false, style = Stroke(sw, cap = StrokeCap.Round))
    }
}
