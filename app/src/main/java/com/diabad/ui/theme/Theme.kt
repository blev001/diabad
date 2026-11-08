package com.diabad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
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
val ShGreen = Color(0xFF7EE5BF)
val ShGreenSoft = Color(0xFF9DEECD)
val ShBlue = Color(0xFF80BAFF)
val ShPurple = Color(0xFFC3ACFF)
val ShOrange = Color(0xFFFFB86B)
val ShDanger = Color(0xFFFF453A)

// Legacy aliases
val DiaMint = ShBlue
val DiaDroplet = ShBlue
val DiaDanger = ShDanger
val DiaOk = ShGreen

private val DarkColors = darkColorScheme(
    primary = ShGreen,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF153C35),
    onPrimaryContainer = ShGreenSoft,
    secondary = ShBlue,
    onSecondary = Color.Black,
    tertiary = ShPurple,
    background = Color(0xFF0A1019),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF131E2B),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF203043),
    onSurfaceVariant = Color(0xFFA9B9CB),
    error = ShDanger,
    onError = Color.White,
    errorContainer = Color(0xFF3A1515),
    onErrorContainer = Color(0xFFFFB4AB),
    outline = Color(0xFF2B3D50),
    outlineVariant = Color(0xFF2B3D50),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF08745A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F7EB),
    onPrimaryContainer = Color(0xFF083E30),
    secondary = Color(0xFF285EA8),
    onSecondary = Color.White,
    tertiary = Color(0xFF7251A8),
    background = Color(0xFFF2F6F8),
    onBackground = Color(0xFF131E2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF131E2B),
    surfaceVariant = Color(0xFFE7EEF3),
    onSurfaceVariant = Color(0xFF526476),
    error = Color(0xFFD70015),
    onError = Color.White,
    errorContainer = Color(0xFFFFE5E5),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFB6C6D2),
    outlineVariant = Color(0xFFD8E3EA),
)

private val ShTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 80.sp,
        fontFeatureSettings = "tnum",
        lineHeight = 86.sp,
        letterSpacing = (-3).sp,
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
        shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(30.dp)),
        content = content,
    )
}
