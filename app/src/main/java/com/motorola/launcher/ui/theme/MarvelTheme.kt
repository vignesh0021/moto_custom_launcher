package com.motorola.launcher.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.ThemeColorPreset

/**
 * Per-hero visual identity: core glow, accents, HUD borders, dock styling.
 */
data class MarvelThemeColors(
    val coreGlow: Color,
    val accent: Color,
    val bright: Color,
    val glow: Color,
    val hudBorder: Color,
    val hudBorderBright: Color,
    val glassSurface: Color
)

data class MarvelDockStyle(
    val glassColor: Color,
    val cutCornerSize: androidx.compose.ui.unit.Dp,
    val pillText: String
)

data class MarvelTheme(
    val hero: MarvelHeroProtocol,
    val colors: MarvelThemeColors,
    val dockStyle: MarvelDockStyle
)

fun marvelThemeFor(hero: MarvelHeroProtocol, preset: ThemeColorPreset): MarvelTheme {
    return MarvelTheme(
        hero = hero,
        colors = MarvelThemeColors(
            coreGlow = hero.coreGlow,
            accent = hero.accent,
            bright = preset.primary,
            glow = preset.glow,
            hudBorder = hero.hudBorder,
            hudBorderBright = hero.hudBorder,
            glassSurface = StarkGlassSurface
        ),
        dockStyle = MarvelDockStyle(
            glassColor = DockGlass,
            cutCornerSize = androidx.compose.ui.unit.dp(10),
            pillText = hero.displayName.uppercase()
        )
    )
}

val LocalMarvelTheme = staticCompositionLocalOf {
    marvelThemeFor(MarvelHeroProtocol.IRON_MAN, ThemeColorPreset.CYAN)
}
