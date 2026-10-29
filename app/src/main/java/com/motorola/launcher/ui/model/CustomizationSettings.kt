package com.motorola.launcher.ui.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Geometric type of the central Marvel Hero Core.
 */
enum class CoreGeometry {
    ARC_REACTOR,           // Iron Man / Tony Stark: Triangular confinement core & magnetic coils
    VIBRANIUM_WEAVE,       // Black Panther / Wakanda: Kinetic tribal tooth ring & energy wave
    STAR_SHIELD,           // Captain America / Steve Rogers: Vibranium shield rings & 5-point Star
    MJOLNIR_RUNIC,         // Thor / Asgard: Asgardian runic compass with lightning crackles
    EYE_OF_AGAMOTTO,       // Doctor Strange: Mystic Tao spell mandala with Time Stone center
    SPIDER_MATRIX,         // Spider-Man: Radial web targeting matrix & nano spider reticle
    GAMMA_FIST,            // Hulk: Gamma bio-radiation rings & kinetic compression core
    BLACK_WIDOW_HOURGLASS, // Black Widow: Tactical dual crimson hourglass reticle & precision crosshairs
    AVENGERS_CHEVRON       // Avengers Unified: Stylized Avengers chevron emblem with orbital hero rings
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
        protocolTitle = "Tony Stark // Mark VII",
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
        protocolTitle = "Wakanda // Kinetic Panther",
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
        protocolTitle = "Steve Rogers // Star Shield",
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
        protocolTitle = "Asgard // Mjolnir Thunder",
        primary = Color(0xFFFFD700),
        secondary = Color(0xFF00E5FF),
        bright = Color(0xFFFFE57F),
        glow = Color(0x66FFD700),
        subtle = Color(0x1FFFD700),
        coreGeometry = CoreGeometry.MJOLNIR_RUNIC
    ),
    HULK(
        id = "hulk",
        heroName = "Hulk",
        protocolTitle = "Bruce Banner // Gamma Titan",
        primary = Color(0xFF16A34A),
        secondary = Color(0xFF9333EA),
        bright = Color(0xFF4ADE80),
        glow = Color(0x6616A34A),
        subtle = Color(0x1F16A34A),
        coreGeometry = CoreGeometry.GAMMA_FIST
    ),
    DOCTOR_STRANGE(
        id = "doctor_strange",
        heroName = "Doctor Strange",
        protocolTitle = "Sanctum // Eye of Agamotto",
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
        protocolTitle = "Peter Parker // Iron Spider",
        primary = Color(0xFFFF1744),
        secondary = Color(0xFF00B0FF),
        bright = Color(0xFFFF5252),
        glow = Color(0x66FF1744),
        subtle = Color(0x1FFF1744),
        coreGeometry = CoreGeometry.SPIDER_MATRIX
    ),
    BLACK_WIDOW(
        id = "black_widow",
        heroName = "Black Widow",
        protocolTitle = "Natasha Romanoff // Red Room",
        primary = Color(0xFFFF2E4C),
        secondary = Color(0xFFF59E0B),
        bright = Color(0xFFFF6677),
        glow = Color(0x66FF2E4C),
        subtle = Color(0x1FFF2E4C),
        coreGeometry = CoreGeometry.BLACK_WIDOW_HOURGLASS
    ),
    AVENGERS_UNIFIED(
        id = "avengers_unified",
        heroName = "Avengers Unified",
        protocolTitle = "Avengers // Initiative Protocol",
        primary = Color(0xFF1E88E5),
        secondary = Color(0xFFFFC107),
        bright = Color(0xFF64B5F6),
        glow = Color(0x661E88E5),
        subtle = Color(0x1F1E88E5),
        coreGeometry = CoreGeometry.AVENGERS_CHEVRON
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
    ),
    GREEN(
        id = "green",
        displayName = "Gamma Emerald",
        primary = Color(0xFF16A34A),
        bright = Color(0xFF4ADE80),
        glow = Color(0x6616A34A),
        subtle = Color(0x1F16A34A)
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
    MARVEL_HERO("marvel_hero", "Marvel Theme", Color(0xFF140D26), Color(0xFF040208)),
    AVENGERS_TOWER("stark_tower", "Avengers Tower", Color(0xFF041A22), Color(0xFF010508)),
    WAKANDA("wakanda", "Wakanda Weave", Color(0xFF170928), Color(0xFF06020D)),
    BIFROST("bifrost", "Bifrost Realm", Color(0xFF1C1300), Color(0xFF040710)),
    GAMMA_CORE("gamma_core", "Gamma Biosphere", Color(0xFF032110), Color(0xFF020A05)),
    SPIDER_MATRIX("spider_matrix", "Spider Matrix", Color(0xFF26050C), Color(0xFF050108)),
    SANCTUM("sanctum", "Sanctum Mystic", Color(0xFF241002), Color(0xFF030A05)),
    CYBER_GRID("cyber_grid", "Cyber Grid", Color(0xFF0A0F1C), Color(0xFF020308)),
    EARTH("earth", "Earth Hologram", Color(0xFF001B33), Color(0xFF02060C)),
    DEEP_SPACE("deep_space", "Deep Space", Color(0xFF12051F), Color(0xFF020107)),
    SYSTEM("system", "My Photo", Color(0xFF1A2230), Color(0xFF0A0E14));

    companion object {
        fun fromId(id: String?): WallpaperPreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: MARVEL_HERO
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
    HERO_THEMED("hero_themed", "Marvel Hero", 14.dp, true, 0.85f), // Unique hero-specific design
    CLEAN("clean", "Clean", 14.dp, false, 0.0f),       // Pure native crisp icon
    MINIMAL("minimal", "Minimal", 12.dp, false, 0.15f), // Whisper of border
    TECH("tech", "Tech Brackets", 10.dp, false, 0.40f), // Corner bracket reticles
    GLOW("glow", "Neon Glow", 14.dp, true, 0.70f);      // Cyberpunk diffuse glow

    companion object {
        fun fromId(id: String?): IconStylePreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HERO_THEMED
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
 * PixelFriend Companion character style (inspired by aakashcreates.com).
 */
enum class CompanionStyle(val id: String, val displayName: String) {
    HERO_SUIT("hero_suit", "Marvel Hero"),
    TAILORED_JACKET("tailored", "Brown Jacket"),
    SOUTH_INDIAN("south_indian", "South Indian");

    companion object {
        fun fromId(id: String?): CompanionStyle {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: HERO_SUIT
        }
    }
}

/**
 * Complete immutable customization state for the launcher.
 */
@Immutable
data class CustomizationSettings(
    val activeThemeId: String = "iron_man",
    val heroProtocol: MarvelHeroProtocol = MarvelHeroProtocol.IRON_MAN,
    val themeColor: ThemeColorPreset = ThemeColorPreset.CYAN,
    val wallpaper: WallpaperPreset = WallpaperPreset.MARVEL_HERO,
    val iconStyle: IconStylePreset = IconStylePreset.HERO_THEMED,
    val animationLevel: AnimationLevelPreset = AnimationLevelPreset.SMOOTH,
    val companionStyle: CompanionStyle = CompanionStyle.HERO_SUIT,
    val showSystemInfo: Boolean = true,
    val enableQuickAccessShelf: Boolean = true,
    val dynamicWallpaper: Boolean = true,
    val hapticFeedback: Boolean = true,
    val soundEffectsEnabled: Boolean = false,
    val hydrationReminderEnabled: Boolean = true,
    val hydrationIntervalMinutes: Int = 60,
    val companionVoiceEnabled: Boolean = true,
    val waterGlassesCount: Int = 0,
    val lastHydrationHour: Long = 0L
)

