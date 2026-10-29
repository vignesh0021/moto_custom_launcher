package com.motorola.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.motorola.launcher.theme.MarvelTheme
import com.motorola.launcher.theme.MarvelThemeRegistry
import com.motorola.launcher.ui.model.CustomizationSettings

val LocalCustomization = staticCompositionLocalOf { CustomizationSettings() }
val LocalMarvelTheme = staticCompositionLocalOf<MarvelTheme> { MarvelThemeRegistry.defaultTheme }

@Composable
fun MotoLauncherTheme(
    customization: CustomizationSettings = CustomizationSettings(),
    content: @Composable () -> Unit
) {
    // Resolve active theme with safe fallback
    val currentTheme = MarvelThemeRegistry.getTheme(customization.activeThemeId.ifBlank { customization.heroProtocol.id })

    val darkColorScheme = darkColorScheme(
        primary = currentTheme.colors.primary,
        secondary = currentTheme.colors.bright,
        tertiary = currentTheme.colors.glow,
        surface = currentTheme.colors.surface,
        onPrimary = currentTheme.colors.surfaceDark,
        onSurface = currentTheme.colors.textPrimary,
        background = Color(0x00000000) // Transparent for wallpaper pass-through
    )

    CompositionLocalProvider(
        LocalCustomization provides customization,
        LocalMarvelTheme provides currentTheme
    ) {
        MaterialTheme(
            colorScheme = darkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
