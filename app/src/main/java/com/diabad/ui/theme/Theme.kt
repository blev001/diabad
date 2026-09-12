package com.diabad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Samsung Health–inspired One UI dark palette. */
val ShBg = Color(0xFF010101)
val ShCard = Color(0xFF1C1C1E)
val ShCardElevated = Color(0xFF2C2C2E)
val ShCardGlass = Color(0xCC252528)
val ShTextPrimary = Color(0xFFFFFFFF)
val ShTextSecondary = Color(0xFFAEAEB2)
val ShTextTertiary = Color(0xFF8E8E93)
val ShGreen = Color(0xFF34C759)
val ShGreenSoft = Color(0xFF2EE66F)
val ShBlue = Color(0xFF5AC8FA)
val ShPurple = Color(0xFFBF5AF2)
val ShOrange = Color(0xFFFF9F0A)
val ShDanger = Color(0xFFFF453A)
val ShDangerContainer = Color(0xFF3A1515)
val ShDivider = Color(0xFF38383A)
val ShGlow = Color(0x33FF8A50)

// Kept for call sites that still reference brand tokens.
val DiaMint = ShBlue
val DiaDroplet = ShBlue
val DiaNavy = ShTextPrimary
val DiaSoftBg = ShBg
val DiaSurface = ShCard
val DiaDanger = ShDanger
val DiaDangerContainer = ShDangerContainer
val DiaOk = ShGreen

private val ShColors = darkColorScheme(
    primary = ShGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A3D28),
    onPrimaryContainer = ShGreenSoft,
    secondary = ShBlue,
    onSecondary = Color.Black,
    tertiary = ShPurple,
    background = ShBg,
    onBackground = ShTextPrimary,
    surface = ShCard,
    onSurface = ShTextPrimary,
    surfaceVariant = ShCardElevated,
    onSurfaceVariant = ShTextSecondary,
    error = ShDanger,
    onError = Color.White,
    errorContainer = ShDangerContainer,
    onErrorContainer = Color(0xFFFFB4AB),
    outline = ShDivider,
    outlineVariant = ShDivider,
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
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ShColors,
        typography = ShTypography,
        content = content,
    )
}
