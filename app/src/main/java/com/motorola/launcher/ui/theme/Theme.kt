package com.motorola.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.motorola.launcher.ui.model.CustomizationSettings

val LocalCustomization = staticCompositionLocalOf { CustomizationSettings() }

@Composable
fun MotoLauncherTheme(
    customization: CustomizationSettings = CustomizationSettings(),
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(
        primary = customization.themeColor.primary,
        secondary = customization.themeColor.bright,
        tertiary = customization.themeColor.glow,
        surface = MotoDarkSurface,
        onPrimary = MotoBlack,
        onSurface = TextPrimary,
        background = Color(0x00000000) // Transparent for wallpaper pass-through
    )

    CompositionLocalProvider(LocalCustomization provides customization) {
        MaterialTheme(
            colorScheme = darkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
