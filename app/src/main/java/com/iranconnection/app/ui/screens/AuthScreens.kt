package com.iranconnection.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iranconnection.app.data.auth.AuthViewModel
import kotlinx.coroutines.delay

// ---- Palette (matches Profile screen) ----
private val ATealStart   = Color(0xFF3DBFBA)
private val ATealMid     = Color(0xFF279491)
private val ATealEnd     = Color(0xFF195E5C)
private val ATextPrimary = Color(0xFF18182A)
private val ATextMuted   = Color(0xFFA0AAB8)
private val ATextHint    = Color(0xFFC2CAD6)
private val ACardWhite   = Color.White
private val ARed         = Color(0xFFEF4444)

// ---- Validation ----
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun validateEmail(email: String): String? = when {
    email.isBlank() -> "ایمیل الزامی است"
    email.length > 256 -> "ایمیل حداکثر ۲۵۶ کاراکتر"
    !EMAIL_REGEX.matches(email) -> "فرمت ایمیل معتبر نیست"
    else -> null
}

private fun validatePassword(pw: String): String? = when {
    pw.isBlank() -> "رمز عبور الزامی است"
    pw.length < 8 -> "رمز عبور حداقل ۸ کاراکتر"
    !pw.any { it in 'A'..'Z' } -> "حداقل یک حرف بزرگ (A-Z) لازم است"
    !pw.any { it in '0'..'9' } -> "حداقل یک عدد (0-9) لازم است"
    else -> null
}

private fun validateFullName(name: String): String? =
    if (name.length > 128) "نام حداکثر ۱۲۸ کاراکتر" else null

// =====================================================================
// Login
// =====================================================================
@Composable
fun LoginScreen(vm: AuthViewModel, onRegisterClick: () -> Unit) {
    val state by vm.state.collectAsState()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwVisible by rememberSaveable { mutableStateOf(false) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    val submit = {
        val e = validateEmail(email)
        val p = if (password.isBlank()) "رمز عبور الزامی است" else null
        emailErr = e; pwErr = p
        if (e == null && p == null) {
            vm.clearLoginError()
            vm.login(email, password)
        }
    }

    AuthScaffold(subtitle = "Secure global access · IR") {
        Text(
            "Welcome back",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ATextPrimary,
            letterSpacing = (-0.3).sp, modifier = Modifier.padding(bottom = 3.dp),
        )

        AuthField(
            value = email,
            onValueChange = { email = it; if (emailErr != null) emailErr = null },
            placeholder = "Email address",
            keyboardType = KeyboardType.Email,
            leadingIcon = { Canvas(Modifier.size(15.dp, 12.dp)) { drawMailIcon() } },
        )
        FieldError(emailErr)

        AuthField(
            value = password,
            onValueChange = { password = it; if (pwErr != null) pwErr = null },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Canvas(Modifier.size(14.dp, 17.dp)) { drawLockIcon() } },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }, modifier = Modifier.size(32.dp)) {
                    Canvas(Modifier.size(16.dp)) { drawEyeIcon(pwVisible) }
                }
            },
        )
        FieldError(pwErr)

        state.loginError?.let { Banner(it) }

        GradientButton(
            label = "Sign In",
            loading = state.loginLoading,
            onClick = submit,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("New here? ", fontSize = 11.5.sp, color = ATextMuted)
            Text(
                "Create account",
                fontSize = 11.5.sp, color = ATealStart, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onRegisterClick() },
            )
        }
    }
}

// =====================================================================
// Register
// =====================================================================
@Composable
fun RegisterScreen(vm: AuthViewModel, onLoginClick: () -> Unit) {
    val state by vm.state.collectAsState()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwVisible by rememberSaveable { mutableStateOf(false) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    // On success, briefly show the message then return to login.
    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) {
            delay(1400)
            vm.clearRegisterState()
            onLoginClick()
        }
    }

    val submit = {
        val n = validateFullName(fullName)
        val e = validateEmail(email)
        val p = validatePassword(password)
        nameErr = n; emailErr = e; pwErr = p
        if (n == null && e == null && p == null) {
            vm.clearRegisterState()
            vm.register(email, password, fullName)
        }
    }

    AuthScaffold(subtitle = "Create your account · IR") {
        Text(
            "Create account",
            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ATextPrimary,
            letterSpacing = (-0.3).sp, modifier = Modifier.padding(bottom = 3.dp),
        )

        AuthField(
            value = fullName,
            onValueChange = { fullName = it; if (nameErr != null) nameErr = null },
            placeholder = "Full name (optional)",
            keyboardType = KeyboardType.Text,
            leadingIcon = { Canvas(Modifier.size(15.dp)) { drawUserIcon() } },
        )
        FieldError(nameErr)

        AuthField(
            value = email,
            onValueChange = { email = it; if (emailErr != null) emailErr = null },
            placeholder = "Email address",
            keyboardType = KeyboardType.Email,
            leadingIcon = { Canvas(Modifier.size(15.dp, 12.dp)) { drawMailIcon() } },
        )
        FieldError(emailErr)

        AuthField(
            value = password,
            onValueChange = { password = it; if (pwErr != null) pwErr = null },
            placeholder = "Password",
            keyboardType = KeyboardType.Password,
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Canvas(Modifier.size(14.dp, 17.dp)) { drawLockIcon() } },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }, modifier = Modifier.size(32.dp)) {
                    Canvas(Modifier.size(16.dp)) { drawEyeIcon(pwVisible) }
                }
            },
        )
        FieldError(pwErr ?: "حداقل ۸ کاراکتر، یک حرف بزرگ و یک عدد", isHint = pwErr == null)

        state.registerError?.let { Banner(it) }
        if (state.registerSuccess) {
            Banner(state.registerMessage ?: "ثبت‌نام موفق بود", success = true)
        }

        GradientButton(
            label = "Create account",
            loading = state.registerLoading,
            onClick = submit,
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("Already have an account? ", fontSize = 11.5.sp, color = ATextMuted)
            Text(
                "Sign in",
                fontSize = 11.5.sp, color = ATealStart, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onLoginClick() },
            )
        }
    }
}

// =====================================================================
// Shared building blocks
// =====================================================================
@Composable
private fun AuthScaffold(subtitle: String, form: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Hero gradient card
        Box(
            modifier = Modifier
                .padding(start = 12.dp, end = 12.dp, top = 10.dp)
                .fillMaxWidth()
                .shadow(elevation = 20.dp, shape = RoundedCornerShape(28.dp), spotColor = ATealMid)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ATealStart, ATealMid, ATealEnd),
                        start = Offset(0f, 0f), end = Offset(600f, 600f),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            ARing(130.dp, 26.dp, 0.06f, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-28).dp))
            ARing(96.dp, 20.dp, 0.05f, Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = 44.dp))
            ARing(70.dp, 14.dp, 0.04f, Modifier.align(Alignment.BottomStart).offset(x = (-18).dp, y = (-20).dp))

            Column {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(17.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(28.dp)) { drawGlobeIcon() }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "IranConnection",
                    fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    letterSpacing = (-0.6).sp, lineHeight = 24.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    subtitle,
                    fontSize = 11.5.sp, color = Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Normal,
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            content = form,
        )
    }
}

@Composable
private fun GradientButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .shadow(12.dp, RoundedCornerShape(13.dp), spotColor = ATealMid),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF4ECAC5), ATealMid)),
                    RoundedCornerShape(13.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun FieldError(message: String?, isHint: Boolean = false) {
    if (message == null) return
    Text(
        message,
        fontSize = 10.5.sp,
        color = if (isHint) ATextMuted else ARed,
        modifier = Modifier.padding(start = 4.dp, top = (-4).dp),
    )
}

@Composable
private fun Banner(message: String, success: Boolean = false) {
    val color = if (success) ATealMid else ARed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text(message, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(13.dp), spotColor = Color(0xFF000000).copy(alpha = 0.06f))
            .clip(RoundedCornerShape(13.dp))
            .background(ACardWhite)
            .border(1.dp, Color(0xFFEAECF2), RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leadingIcon()
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            textStyle = TextStyle(fontSize = 12.5.sp, color = ATextPrimary, fontFamily = FontFamily.Default),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, fontSize = 12.5.sp, color = ATextHint)
                }
                inner()
            },
        )
        trailingIcon?.invoke()
    }
}

@Composable
private fun ARing(size: Dp, borderWidth: Dp, alpha: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, Color.White.copy(alpha = alpha), CircleShape),
    )
}

// ---- Icons ----
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
}

private fun DrawScope.drawMailIcon() {
    val s = Stroke(width = 1.8f, cap = StrokeCap.Round)
    drawRoundRect(ATextHint, size = size, cornerRadius = CornerRadius(4f), style = s)
    drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, ATextHint, style = s)
}

private fun DrawScope.drawLockIcon() {
    val w = size.width; val h = size.height
    drawRoundRect(ATextHint, topLeft = Offset(0f, h * 0.43f), size = Size(w, h * 0.53f),
        cornerRadius = CornerRadius(6f), style = Stroke(1.8f))
    drawPath(Path().apply {
        moveTo(w * 0.25f, h * 0.43f); lineTo(w * 0.25f, h * 0.32f)
        cubicTo(w * 0.25f, h * 0.12f, w * 0.75f, h * 0.12f, w * 0.75f, h * 0.32f); lineTo(w * 0.75f, h * 0.43f)
    }, ATextHint, style = Stroke(1.8f, cap = StrokeCap.Round))
    drawCircle(ATextHint, radius = w * 0.14f, center = Offset(w / 2, h * 0.69f))
}

private fun DrawScope.drawEyeIcon(visible: Boolean) {
    val color = if (visible) ATealStart else ATextHint
    val s = Stroke(1.8f, cap = StrokeCap.Round)
    val cx = size.width / 2; val cy = size.height / 2
    drawPath(Path().apply {
        moveTo(size.width * 0.075f, cy)
        cubicTo(size.width * 0.35f, cy - size.height * 0.3f, size.width * 0.65f, cy - size.height * 0.3f, size.width * 0.925f, cy)
        cubicTo(size.width * 0.65f, cy + size.height * 0.3f, size.width * 0.35f, cy + size.height * 0.3f, size.width * 0.075f, cy)
    }, color, style = s)
    drawCircle(color, radius = size.minDimension * 0.12f, center = Offset(cx, cy), style = s)
    if (!visible) drawLine(ATextHint, Offset(size.width * 0.125f, size.height * 0.125f),
        Offset(size.width * 0.875f, size.height * 0.875f), strokeWidth = 1.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawUserIcon() {
    val cx = size.width / 2
    val s = Stroke(1.8f, cap = StrokeCap.Round)
    drawCircle(ATextHint, radius = size.minDimension * 0.18f, center = Offset(cx, size.height * 0.3f), style = s)
    drawPath(Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.92f)
        cubicTo(size.width * 0.18f, size.height * 0.58f, size.width * 0.82f, size.height * 0.58f, size.width * 0.82f, size.height * 0.92f)
    }, ATextHint, style = s)
}
