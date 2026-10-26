package com.motorola.launcher.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometric type of the central Marvel Hero Core.
 */
enum class CoreGeometry {
    ARC_REACTOR,      // Iron Man / Tony Stark: Triangular confinement core & magnetic coils
    VIBRANIUM_WEAVE,  // Black Panther / Wakanda: Kinetic tribal tooth ring & energy wave
    STAR_SHIELD,      // Captain America / Steve Rogers: Vibranium shield rings & 5-point Star
    MJOLNIR_RUNIC,    // Thor / Asgard: Asgardian runic compass with lightning crackles
    EYE_OF_AGAMOTTO,  // Doctor Strange: Mystic Tao spell mandala with Time Stone center
    SPIDER_MATRIX     // Spider-Man: Radial web targeting matrix & nano spider reticle
}

/**
 * Full Marvel Superhero Protocol presets for the launcher.
 */
enum class MarvelHeroProtocol(
    val id: String,
    val heroName: String,
    val protocolTitle: String,
    val primary: Color,
    val secondary: Color,
    val bright: Color,
    val glow: Color,
    val subtle: Color,
    val coreGeometry: CoreGeometry
) {
    IRON_MAN(
        id = "iron_man",
        heroName = "Iron Man",
        protocolTitle = "STARK OS // JARVIS PROTOCOL",
        primary = Color(0xFF00E5FF),
        secondary = Color(0xFFFF2A42),
        bright = Color(0xFF80F7FF),
        glow = Color(0x6600E5FF),
        subtle = Color(0x1F00E5FF),
        coreGeometry = CoreGeometry.ARC_REACTOR
    ),
    BLACK_PANTHER(
        id = "black_panther",
        heroName = "Black Panther",
        protocolTitle = "WAKANDA OS // KINETIC PROTOCOL",
        primary = Color(0xFFB388FF),
        secondary = Color(0xFFE0AAFF),
        bright = Color(0xFFD1B3FF),
        glow = Color(0x66B388FF),
        subtle = Color(0x1FB388FF),
        coreGeometry = CoreGeometry.VIBRANIUM_WEAVE
    ),
    CAPTAIN_AMERICA(
        id = "captain_america",
        heroName = "Captain America",
        protocolTitle = "SHIELD OS // FIRST AVENGER",
        primary = Color(0xFF2979FF),
        secondary = Color(0xFFFF1744),
        bright = Color(0xFF82B1FF),
        glow = Color(0x662979FF),
        subtle = Color(0x1F2979FF),
        coreGeometry = CoreGeometry.STAR_SHIELD
    ),
    THOR(
        id = "thor",
        heroName = "Thor",
        protocolTitle = "ASGARD OS // BIFROST PROTOCOL",
        primary = Color(0xFFFFD700),
        secondary = Color(0xFF00E5FF),
        bright = Color(0xFFFFE57F),
        glow = Color(0x66FFD700),
        subtle = Color(0x1FFFD700),
        coreGeometry = CoreGeometry.MJOLNIR_RUNIC
    ),
    DOCTOR_STRANGE(
        id = "doctor_strange",
        heroName = "Doctor Strange",
        protocolTitle = "MYSTIC OS // AGAMOTTO PROTOCOL",
        primary = Color(0xFFFF6D00),
        secondary = Color(0xFF00E676),
        bright = Color(0xFFFFAB40),
        glow = Color(0x66FF6D00),
        subtle = Color(0x1FFF6D00),
        coreGeometry = CoreGeometry.EYE_OF_AGAMOTTO
    ),
    SPIDER_MAN(
        id = "spider_man",
        heroName = "Spider-Man",
        protocolTitle = "STARK NANO // IRON SPIDER",
        primary = Color(0xFFFF1744),
        secondary = Color(0xFF00B0FF),
        bright = Color(0xFFFF5252),
        glow = Color(0x66FF1744),
        subtle = Color(0x1FFF1744),
        coreGeometry = CoreGeometry.SPIDER_MATRIX
    );

    companion object {
        fun fromId(id: String?): MarvelHeroProtocol {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IRON_MAN
        }
    }
}

/**
 * Backward-compatible ThemeColorPreset mapped to Marvel Protocols.
 */
enum class ThemeColorPreset(
    val id: String,
    val displayName: String,
    val primary: Color,
    val bright: Color,
    val glow: Color,
    val subtle: Color
) {
    CYAN(
        id = "cyan",
        displayName = "Iron Man Cyan",
        primary = Color(0xFF00E5FF),
        bright = Color(0xFF80F7FF),
        glow = Color(0x6600E5FF),
        subtle = Color(0x1F00E5FF)
    ),
    RED(
        id = "red",
        displayName = "Hot Rod Red",
        primary = Color(0xFFFF3344),
        bright = Color(0xFFFF7788),
        glow = Color(0x66FF3344),
        subtle = Color(0x1FFF3344)
    ),
    PURPLE(
        id = "purple",
        displayName = "Wakanda Violet",
        primary = Color(0xFFB388FF),
        bright = Color(0xFFD1B3FF),
        glow = Color(0x66B388FF),
        subtle = Color(0x1FB388FF)
    ),
    AMBER(
        id = "amber",
        displayName = "Asgard Gold",
        primary = Color(0xFFFFD700),
        bright = Color(0xFFFFE57F),
        glow = Color(0x66FFD700),
        subtle = Color(0x1FFFD700)
    );

    companion object {
        fun fromId(id: String?): ThemeColorPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CYAN
        }
    }
}

/**
 * Wallpaper presets for STARK OS command center.
 * SYSTEM shows the user's own Android wallpaper (with a readability scrim);
 * the others are procedurally drawn HUD backgrounds.
 */
enum class WallpaperPreset(
    val id: String,
    val displayName: String,
    val top: Color,
    val bottom: Color
) {
    SYSTEM("system", "My Photo", Color(0xFF1A2230), Color(0xFF0A0E14)),
    EARTH("earth", "Earth Hologram", Color(0xFF001B33), Color(0xFF02060C)),
    CYBER_GRID("cyber_grid", "Cyber Grid", Color(0xFF0A0F1C), Color(0xFF020308)),
    DEEP_SPACE("deep_space", "Deep Space", Color(0xFF12051F), Color(0xFF020107)),
    STARK_TOWER("stark_tower", "Stark Tower", Color(0xFF041A22), Color(0xFF010508));

    companion object {
        fun fromId(id: String?): WallpaperPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SYSTEM
        }
    }
}

/**
 * Icon visual presentation styles.
 */
enum class IconStylePreset(
    val id: String,
    val displayName: String,
    val cornerRadius: Dp,
    val hasGlow: Boolean,
    val borderAlpha: Float
) {
    CLEAN("clean", "Clean", 14.dp, false, 0.0f),       // Pure native crisp icon without double boxes
    MINIMAL("minimal", "Minimal", 12.dp, false, 0.15f), // Whisper of border
    TECH("tech", "Tech Brackets", 10.dp, false, 0.40f), // Corner bracket reticles
    GLOW("glow", "Neon Glow", 14.dp, true, 0.70f);      // Cyberpunk diffuse glow

    companion object {
        fun fromId(id: String?): IconStylePreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: CLEAN
        }
    }
}

/**
 * Animation speed / battery optimization level.
 */
enum class AnimationLevelPreset(val id: String, val displayName: String, val speedMultiplier: Float) {
    SMOOTH("smooth", "Smooth", 1.0f),
    BALANCED("balanced", "Balanced", 1.4f),
    PERFORMANCE("performance", "Performance", 2.0f);

    companion object {
        fun fromId(id: String?): AnimationLevelPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SMOOTH
        }
    }
}

/**
 * Complete immutable customization state for STARK OS JARVIS Edition.
 */
@Immutable
data class CustomizationSettings(
    val heroProtocol: MarvelHeroProtocol = MarvelHeroProtocol.IRON_MAN,
    val themeColor: ThemeColorPreset = ThemeColorPreset.CYAN,
    val wallpaper: WallpaperPreset = WallpaperPreset.SYSTEM,
    val iconStyle: IconStylePreset = IconStylePreset.CLEAN,
    val animationLevel: AnimationLevelPreset = AnimationLevelPreset.SMOOTH,
    val showSystemInfo: Boolean = true,
    val enableQuickAccessShelf: Boolean = true,
    val dynamicWallpaper: Boolean = true,
    val hapticFeedback: Boolean = true
)
