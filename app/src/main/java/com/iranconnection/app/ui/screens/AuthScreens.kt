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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
private val ATextHint    = Color(0xFFBAC3D0)
private val ABorder      = Color(0xFFE2E6EE)
private val ACardWhite   = Color.White
private val ARed         = Color(0xFFEF4444)

// ---- Validation ----
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private fun validateEmail(email: String): String? = when {
    email.isBlank() -> "Email is required"
    email.length > 256 -> "Email must be at most 256 characters"
    !EMAIL_REGEX.matches(email) -> "Invalid email format"
    else -> null
}

private fun validatePassword(pw: String): String? = when {
    pw.isBlank() -> "Password is required"
    pw.length < 8 -> "Password must be at least 8 characters"
    !pw.any { it in 'A'..'Z' } -> "At least one uppercase letter (A-Z) required"
    !pw.any { it in '0'..'9' } -> "At least one digit (0-9) required"
    else -> null
}

private fun validateFullName(name: String): String? =
    if (name.length > 128) "Name must be at most 128 characters" else null

// =====================================================================
// Login
// =====================================================================
@Composable
fun LoginScreen(vm: AuthViewModel, onRegisterClick: () -> Unit, onForgotClick: () -> Unit = {}) {
    val state by vm.state.collectAsState()
    val focus = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwVisible by rememberSaveable { mutableStateOf(false) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    val submit = {
        focus.clearFocus()
        val e = validateEmail(email)
        val p = if (password.isBlank()) "Password is required" else null
        emailErr = e; pwErr = p
        if (e == null && p == null) {
            vm.clearLoginError()
            vm.login(email, password)
        }
    }

    AuthScaffold(title = "Welcome back", subtitle = "Sign in to continue") {
        AuthField(
            value = email,
            onValueChange = { email = it; if (emailErr != null) emailErr = null },
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = emailErr != null,
            leadingIcon = { Canvas(Modifier.size(20.dp, 16.dp)) { drawMailIcon() } },
        )
        FieldError(emailErr)

        AuthField(
            value = password,
            onValueChange = { password = it; if (pwErr != null) pwErr = null },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = submit,
            isError = pwErr != null,
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Canvas(Modifier.size(18.dp, 21.dp)) { drawLockIcon() } },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }) {
                    Canvas(Modifier.size(20.dp)) { drawEyeIcon(pwVisible) }
                }
            },
        )
        FieldError(pwErr)

        state.loginError?.let { Banner(it) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Forgot password?",
                fontSize = 12.sp, color = ATealStart, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onForgotClick() },
            )
        }

        Spacer(Modifier.height(4.dp))
        GradientButton(label = "Sign In", loading = state.loginLoading, onClick = submit)

        FooterLink(
            prompt = "New here? ",
            action = "Create account",
            onClick = onRegisterClick,
        )
    }
}

// =====================================================================
// Register
// =====================================================================
@Composable
fun RegisterScreen(
    vm: AuthViewModel,
    onLoginClick: () -> Unit,
    onRegistered: (email: String) -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val focus = LocalFocusManager.current

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pwVisible by rememberSaveable { mutableStateOf(false) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var emailErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    // On success the server auto-sends a 6-digit code → go to the verify-email screen.
    LaunchedEffect(state.registerSuccess) {
        if (state.registerSuccess) {
            val registeredEmail = email.trim()
            delay(900)
            vm.clearRegisterState()
            onRegistered(registeredEmail)
        }
    }

    val submit = {
        focus.clearFocus()
        val n = validateFullName(fullName)
        val e = validateEmail(email)
        val p = validatePassword(password)
        nameErr = n; emailErr = e; pwErr = p
        if (n == null && e == null && p == null) {
            vm.clearRegisterState()
            vm.register(email, password, fullName)
        }
    }

    AuthScaffold(title = "Create account", subtitle = "Join IranConnection") {
        AuthField(
            value = fullName,
            onValueChange = { fullName = it; if (nameErr != null) nameErr = null },
            label = "Full name (optional)",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            isError = nameErr != null,
            leadingIcon = { Canvas(Modifier.size(20.dp)) { drawUserIcon() } },
        )
        FieldError(nameErr)

        AuthField(
            value = email,
            onValueChange = { email = it; if (emailErr != null) emailErr = null },
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            isError = emailErr != null,
            leadingIcon = { Canvas(Modifier.size(20.dp, 16.dp)) { drawMailIcon() } },
        )
        FieldError(emailErr)

        AuthField(
            value = password,
            onValueChange = { password = it; if (pwErr != null) pwErr = null },
            label = "Password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = submit,
            isError = pwErr != null,
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Canvas(Modifier.size(18.dp, 21.dp)) { drawLockIcon() } },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }) {
                    Canvas(Modifier.size(20.dp)) { drawEyeIcon(pwVisible) }
                }
            },
        )
        FieldError(pwErr ?: "At least 8 characters, one uppercase and one digit", isHint = pwErr == null)

        state.registerError?.let { Banner(it) }
        if (state.registerSuccess) {
            Banner(state.registerMessage ?: "Registration successful", success = true)
        }

        Spacer(Modifier.height(4.dp))
        GradientButton(label = "Create account", loading = state.registerLoading, onClick = submit)

        FooterLink(
            prompt = "Already have an account? ",
            action = "Sign in",
            onClick = onLoginClick,
        )
    }
}

// =====================================================================
// Verify email (D + E)
// =====================================================================
@Composable
fun VerifyEmailScreen(
    vm: AuthViewModel,
    email: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    var codeErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.verifySuccess) {
        if (state.verifySuccess) { delay(1000); vm.clearVerifyState(); onVerified() }
    }

    val submit = {
        val c = if (code.length == 6) null else "Code must be exactly 6 digits"
        codeErr = c
        if (c == null) { vm.clearVerifyState(); vm.verifyEmail(email, code) }
    }

    AuthScaffold(title = "Verify Email", subtitle = "Verify your email") {
        Text("Enter the 6-digit code sent to $email ", fontSize = 12.5.sp, color = ATextMuted)

        CodeInputField(code = code, onCodeChange = { code = it; if (codeErr != null) codeErr = null }, isError = codeErr != null)
        FieldError(codeErr)

        state.verifyError?.let { Banner(it) }
        if (state.verifySuccess) Banner(state.verifyMessage ?: "Email verified successfully", success = true)

        Spacer(Modifier.height(4.dp))
        GradientButton(label = "Verify", loading = state.verifyLoading, onClick = submit)

        ResendRow(loading = state.resendLoading, onResend = { vm.resendVerification() })
        state.resendMessage?.let { Banner(it, success = true) }
        state.resendError?.let { Banner(it) }

        FooterLink(prompt = "Back to ", action = "Sign in", onClick = onBack)
    }
}

// =====================================================================
// Forgot password (F)
// =====================================================================
@Composable
fun ForgotPasswordScreen(
    vm: AuthViewModel,
    onCodeSent: (email: String) -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val focus = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf("") }
    var emailErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.forgotSuccess) {
        if (state.forgotSuccess) {
            val e = email.trim()
            delay(900); vm.clearForgotState(); onCodeSent(e)
        }
    }

    val submit = {
        focus.clearFocus()
        val e = validateEmail(email)
        emailErr = e
        if (e == null) { vm.clearForgotState(); vm.forgotPassword(email) }
    }

    AuthScaffold(title = "Forgot Password", subtitle = "Reset your password") {
        Text("Enter your account email to receive a recovery code.", fontSize = 12.5.sp, color = ATextMuted)

        AuthField(
            value = email,
            onValueChange = { email = it; if (emailErr != null) emailErr = null },
            label = "Email address",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            onImeAction = submit,
            isError = emailErr != null,
            leadingIcon = { Canvas(Modifier.size(20.dp, 16.dp)) { drawMailIcon() } },
        )
        FieldError(emailErr)

        state.forgotError?.let { Banner(it) }
        if (state.forgotSuccess) Banner(state.forgotMessage ?: "Code sent", success = true)

        Spacer(Modifier.height(4.dp))
        GradientButton(label = "Send Code", loading = state.forgotLoading, onClick = submit)

        FooterLink(prompt = "Back to ", action = "Sign in", onClick = onBack)
    }
}

// =====================================================================
// Reset password with code (G)
// =====================================================================
@Composable
fun ResetPasswordScreen(
    vm: AuthViewModel,
    email: String,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    var code by rememberSaveable { mutableStateOf("") }
    var newPw by rememberSaveable { mutableStateOf("") }
    var pwVisible by rememberSaveable { mutableStateOf(false) }
    var codeErr by remember { mutableStateOf<String?>(null) }
    var pwErr by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.resetSuccess) {
        if (state.resetSuccess) { delay(1100); vm.clearResetState(); onReset() }
    }

    val submit = {
        val c = if (code.length == 6) null else "Code must be exactly 6 digits"
        val p = validatePassword(newPw)
        codeErr = c; pwErr = p
        if (c == null && p == null) { vm.clearResetState(); vm.resetPassword(email, code, newPw) }
    }

    AuthScaffold(title = "Reset Password", subtitle = "Enter code & new password") {
        Text("Enter the code sent to $email and your new password.", fontSize = 12.5.sp, color = ATextMuted)

        CodeInputField(code = code, onCodeChange = { code = it; if (codeErr != null) codeErr = null }, isError = codeErr != null)
        FieldError(codeErr)

        AuthField(
            value = newPw,
            onValueChange = { newPw = it; if (pwErr != null) pwErr = null },
            label = "New password",
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            onImeAction = submit,
            isError = pwErr != null,
            visualTransformation = if (pwVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Canvas(Modifier.size(18.dp, 21.dp)) { drawLockIcon() } },
            trailingIcon = {
                IconButton(onClick = { pwVisible = !pwVisible }) {
                    Canvas(Modifier.size(20.dp)) { drawEyeIcon(pwVisible) }
                }
            },
        )
        FieldError(pwErr ?: "At least 8 characters, one uppercase and one digit", isHint = pwErr == null)

        state.resetError?.let { Banner(it) }
        if (state.resetSuccess) Banner(state.resetMessage ?: "Password changed", success = true)

        Spacer(Modifier.height(4.dp))
        GradientButton(label = "Reset Password", loading = state.resetLoading, onClick = submit)

        FooterLink(prompt = "Back to ", action = "Sign in", onClick = onBack)
    }
}

// =====================================================================
// Code input (OTP-style, paste-friendly) + resend timer
// =====================================================================
@Composable
private fun CodeInputField(code: String, onCodeChange: (String) -> Unit, isError: Boolean = false) {
    val requester = remember { FocusRequester() }
    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(6)) },
        modifier = Modifier.fillMaxWidth().focusRequester(requester),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i ->
                    val ch = code.getOrNull(i)?.toString() ?: ""
                    val active = i == code.length
                    val border = when { isError -> ARed; active -> ATealMid; else -> ABorder }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ACardWhite)
                            .border(1.5.dp, border, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ch, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                            color = ATextPrimary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        },
    )
    LaunchedEffect(Unit) { runCatching { requester.requestFocus() } }
}

@Composable
private fun ResendRow(loading: Boolean, onResend: () -> Unit) {
    var seconds by rememberSaveable { mutableStateOf(60) }
    LaunchedEffect(seconds) { if (seconds > 0) { delay(1000); seconds-- } }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        if (seconds > 0) {
            Text("Resend code in $seconds s", fontSize = 12.sp, color = ATextMuted)
        } else {
            Text(
                if (loading) "Sending..." else "Resend code",
                fontSize = 12.sp, color = ATealStart, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = !loading) { onResend(); seconds = 60 },
            )
        }
    }
}

// =====================================================================
// Shared building blocks
// =====================================================================
@Composable
private fun AuthScaffold(
    title: String,
    subtitle: String,
    form: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        // Hero gradient card
        Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(30.dp), spotColor = ATealMid)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(ATealStart, ATealMid, ATealEnd),
                        start = Offset(0f, 0f), end = Offset(700f, 700f),
                    ),
                )
                .padding(horizontal = 26.dp, vertical = 36.dp),
        ) {
            ARing(150.dp, 30.dp, 0.06f, Modifier.align(Alignment.TopEnd).offset(x = 34.dp, y = (-34).dp))
            ARing(110.dp, 22.dp, 0.05f, Modifier.align(Alignment.BottomEnd).offset(x = (-34).dp, y = 50.dp))
            ARing(78.dp, 16.dp, 0.04f, Modifier.align(Alignment.BottomStart).offset(x = (-20).dp, y = (-22).dp))

            Column {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(19.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.size(32.dp)) { drawGlobeIcon() }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "IranConnection",
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    letterSpacing = (-0.6).sp, lineHeight = 28.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Normal,
                )
            }
        }

        // Form section
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                fontSize = 19.sp, fontWeight = FontWeight.Bold, color = ATextPrimary,
                letterSpacing = (-0.4).sp, modifier = Modifier.padding(bottom = 2.dp),
            )
            form()
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable () -> Unit,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        singleLine = true,
        isError = isError,
        textStyle = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Default),
        placeholder = { Text(label, fontSize = 14.5.sp, color = ATextHint) },
        leadingIcon = {
            Box(Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) { leadingIcon() }
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focus.moveFocus(FocusDirection.Down) },
            onDone = { focus.clearFocus(); onImeAction() },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ATextPrimary,
            unfocusedTextColor = ATextPrimary,
            focusedContainerColor = ACardWhite,
            unfocusedContainerColor = ACardWhite,
            errorContainerColor = ACardWhite,
            cursorColor = ATealMid,
            focusedBorderColor = ATealMid,
            unfocusedBorderColor = ABorder,
            errorBorderColor = ARed,
        ),
    )
}

@Composable
private fun GradientButton(label: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(14.dp, RoundedCornerShape(16.dp), spotColor = ATealMid),
        shape = RoundedCornerShape(16.dp),
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
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                )
            } else {
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.2.sp)
            }
        }
    }
}

@Composable
private fun FooterLink(prompt: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(prompt, fontSize = 13.sp, color = ATextMuted)
        Text(
            action,
            fontSize = 13.sp, color = ATealStart, fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onClick() },
        )
    }
}

@Composable
private fun FieldError(message: String?, isHint: Boolean = false) {
    if (message == null) return
    Text(
        message,
        fontSize = 11.5.sp,
        color = if (isHint) ATextMuted else ARed,
        modifier = Modifier.offset(y = (-6).dp).padding(start = 6.dp),
    )
}

@Composable
private fun Banner(message: String, success: Boolean = false) {
    val color = if (success) ATealMid else ARed
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(message, fontSize = 12.5.sp, color = color, fontWeight = FontWeight.Medium)
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
    drawRoundRect(ATextMuted, size = size, cornerRadius = CornerRadius(4f), style = s)
    drawPath(Path().apply { moveTo(0f, 0f); lineTo(size.width / 2, size.height * 0.6f); lineTo(size.width, 0f) }, ATextMuted, style = s)
}

private fun DrawScope.drawLockIcon() {
    val w = size.width; val h = size.height
    drawRoundRect(ATextMuted, topLeft = Offset(0f, h * 0.43f), size = Size(w, h * 0.53f),
        cornerRadius = CornerRadius(6f), style = Stroke(1.8f))
    drawPath(Path().apply {
        moveTo(w * 0.25f, h * 0.43f); lineTo(w * 0.25f, h * 0.32f)
        cubicTo(w * 0.25f, h * 0.12f, w * 0.75f, h * 0.12f, w * 0.75f, h * 0.32f); lineTo(w * 0.75f, h * 0.43f)
    }, ATextMuted, style = Stroke(1.8f, cap = StrokeCap.Round))
    drawCircle(ATextMuted, radius = w * 0.14f, center = Offset(w / 2, h * 0.69f))
}

private fun DrawScope.drawEyeIcon(visible: Boolean) {
    val color = if (visible) ATealStart else ATextMuted
    val s = Stroke(1.8f, cap = StrokeCap.Round)
    val cx = size.width / 2; val cy = size.height / 2
    drawPath(Path().apply {
        moveTo(size.width * 0.075f, cy)
        cubicTo(size.width * 0.35f, cy - size.height * 0.3f, size.width * 0.65f, cy - size.height * 0.3f, size.width * 0.925f, cy)
        cubicTo(size.width * 0.65f, cy + size.height * 0.3f, size.width * 0.35f, cy + size.height * 0.3f, size.width * 0.075f, cy)
    }, color, style = s)
    drawCircle(color, radius = size.minDimension * 0.12f, center = Offset(cx, cy), style = s)
    if (!visible) drawLine(ATextMuted, Offset(size.width * 0.125f, size.height * 0.125f),
        Offset(size.width * 0.875f, size.height * 0.875f), strokeWidth = 1.8f, cap = StrokeCap.Round)
}

private fun DrawScope.drawUserIcon() {
    val cx = size.width / 2
    val s = Stroke(1.8f, cap = StrokeCap.Round)
    drawCircle(ATextMuted, radius = size.minDimension * 0.18f, center = Offset(cx, size.height * 0.3f), style = s)
    drawPath(Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.92f)
        cubicTo(size.width * 0.18f, size.height * 0.58f, size.width * 0.82f, size.height * 0.58f, size.width * 0.82f, size.height * 0.92f)
    }, ATextMuted, style = s)
}
