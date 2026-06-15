package com.iranconnection.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = AppColors.Teal,
    onPrimary = AppColors.CardBg,
    background = AppColors.ScreenBg,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.CardBg,
    onSurface = AppColors.TextPrimary,
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
    )
)

@Composable
fun IranConnectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Design is light-only; keep a single light scheme regardless of system setting.
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
