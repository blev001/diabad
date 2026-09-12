package com.diabad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette from DiaBAD icon (mint / droplet blue). Full One UI theme in UI iteration.
private val Mint = Color(0xFF7EE8E5)
private val DropletBlue = Color(0xFF5BB8E0)
private val Navy = Color(0xFF1A3A5F)
private val SoftBg = Color(0xFFF2FBFA)

private val DiaBADLightColors = lightColorScheme(
    primary = DropletBlue,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = Navy,
    background = SoftBg,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
)

@Composable
fun DiaBADTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DiaBADLightColors,
        content = content,
    )
}
