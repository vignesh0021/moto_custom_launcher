package com.motorola.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MotoDarkColorScheme = darkColorScheme(
    primary = MotoCyanAccent,
    surface = MotoDarkSurface,
    onPrimary = MotoBlack,
    onSurface = TextPrimary,
    background = Color(0x00000000) // Transparent for wallpaper pass-through
)

@Composable
fun MotoLauncherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MotoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
