package com.motorola.launcher.ui.model

import androidx.compose.ui.graphics.Color
import com.motorola.launcher.ui.theme.StarkAmberGlow
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanGlow
import com.motorola.launcher.ui.theme.StarkElectricBlue
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkGold
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.StarkIceBlue
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.StarkRedGlow
import com.motorola.launcher.ui.theme.StarkVoidBlack

/**
 * Marvel hero protocols available in STARK OS.
 * Each maps to a distinct visual identity: core glow, accent, and transition style.
 */
enum class MarvelHeroProtocol(
    val displayName: String,
    val coreGlow: Color,
    val accent: Color,
    val hudBorder: Color
) {
    IRON_MAN("Iron Man", StarkCyanGlow, StarkGold, StarkHudBorderBright),
    CAPTAIN_AMERICA("Captain America", StarkElectricBlue, StarkIceBlue, StarkHudBorder),
    THOR("Thor", StarkAmberGlow, StarkGold, StarkHudBorderBright),
    HULK("Hulk", StarkEmerald, StarkEmerald, StarkHudBorder),
    BLACK_PANTHER("Black Panther", Color(0xFF9D4EDD), Color(0xFF7B2CBF), StarkHudBorder),
    BLACK_WIDOW("Black Widow", StarkRedGlow, StarkReactorRed, StarkHudBorderBright),
    DOCTOR_STRANGE("Doctor Strange", Color(0x66FFB800), StarkGold, StarkHudBorderBright),
    SPIDER_MAN("Spider-Man", StarkRedGlow, StarkReactorRed, StarkHudBorderBright)
}

/**
 * Theme color presets for contextual and manual theme switching.
 */
enum class ThemeColorPreset(val primary: Color, val secondary: Color, val glow: Color) {
    CYAN(StarkCyan, StarkCyanBright, StarkCyanGlow),
    AMBER(StarkGold, StarkAmberGlow, StarkAmberGlow),
    PURPLE(Color(0xFF9D4EDD), Color(0xFF7B2CBF), Color(0x669D4EDD)),
    EMERALD(StarkEmerald, StarkEmerald, Color(0x6600FF9D)),
    CRIMSON(StarkReactorRed, StarkRedGlow, StarkRedGlow)
}

/**
 * Wallpaper preset categories for AIWallpaperEngine and manual selection.
 */
enum class WallpaperPreset {
    MARVEL_HERO,
    MITHRA_FAMILY,
    ABSTRACT,
    NATURE,
    CUSTOM
}
