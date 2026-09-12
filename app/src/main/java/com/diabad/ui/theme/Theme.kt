package com.diabad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// DiaBAD brand (icon): mint teal, droplet blue, navy outlines.
val DiaMint = Color(0xFF7EE8E5)
val DiaDroplet = Color(0xFF5BB8E0)
val DiaNavy = Color(0xFF1A3A5F)
val DiaSoftBg = Color(0xFFEFFAF9)
val DiaSurface = Color(0xFFFFFFFF)
val DiaDanger = Color(0xFFE53935)
val DiaDangerContainer = Color(0xFFFFEBEE)
val DiaOk = Color(0xFF2E7D32)

private val LightColors = lightColorScheme(
    primary = DiaDroplet,
    onPrimary = Color.White,
    primaryContainer = DiaMint,
    onPrimaryContainer = DiaNavy,
    secondary = DiaMint,
    onSecondary = DiaNavy,
    background = DiaSoftBg,
    onBackground = DiaNavy,
    surface = DiaSurface,
    onSurface = DiaNavy,
    surfaceVariant = Color(0xFFD9F3F1),
    onSurfaceVariant = Color(0xFF3D5A6C),
    error = DiaDanger,
    onError = Color.White,
    errorContainer = DiaDangerContainer,
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFF9BB8C0),
)

private val DarkColors = darkColorScheme(
    primary = DiaMint,
    onPrimary = DiaNavy,
    primaryContainer = Color(0xFF1F4E5A),
    onPrimaryContainer = DiaMint,
    secondary = DiaDroplet,
    onSecondary = DiaNavy,
    background = Color(0xFF0F1C22),
    onBackground = Color(0xFFE6F4F3),
    surface = Color(0xFF15262E),
    onSurface = Color(0xFFE6F4F3),
    error = Color(0xFFFF8A80),
)

private val DiaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun DiaBADTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DiaTypography,
        content = content,
    )
}
