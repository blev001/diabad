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
import com.diabad.domain.model.ThemeMode

/** Shared accent tokens (work in both themes). */
val ShGreen = Color(0xFF34C759)
val ShGreenSoft = Color(0xFF2EE66F)
val ShBlue = Color(0xFF5AC8FA)
val ShPurple = Color(0xFFBF5AF2)
val ShOrange = Color(0xFFFF9F0A)
val ShDanger = Color(0xFFFF453A)

// Legacy aliases
val DiaMint = ShBlue
val DiaDroplet = ShBlue
val DiaDanger = ShDanger
val DiaOk = ShGreen

private val DarkColors = darkColorScheme(
    primary = ShGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A3D28),
    onPrimaryContainer = ShGreenSoft,
    secondary = ShBlue,
    onSecondary = Color.Black,
    tertiary = ShPurple,
    background = Color(0xFF010101),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFAEAEB2),
    error = ShDanger,
    onError = Color.White,
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF38383A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF248A3D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8F5E1),
    onPrimaryContainer = Color(0xFF0B3D1C),
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    tertiary = Color(0xFFAF52DE),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF636366),
    error = Color(0xFFD70015),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFD1D1D6),
)

private val ShTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 76.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
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
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

@Composable
fun DiaBADTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = ShTypography,
        content = content,
    )
}
