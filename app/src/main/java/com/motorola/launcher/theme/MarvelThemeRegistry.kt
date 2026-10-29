package com.motorola.launcher.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motorola.launcher.ui.model.CoreGeometry
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.WallpaperPreset

/**
 * Centralized, extensible Theme Engine Registry for the Multi-Marvel Universe Launcher.
 * Contains definitions for initial superhero themes, supports dynamic registration,
 * and guarantees a safe fallback if any theme or asset fails to load.
 */
object MarvelThemeRegistry {

    private val registeredThemes = mutableMapOf<String, MarvelTheme>()

    // ========================================================================
    // 1. IRON MAN — Arc reactor, red/gold armor, cyan telemetry
    // ========================================================================
    val IRON_MAN = MarvelTheme(
        id = "iron_man",
        name = "Iron Man",
        heroName = "Iron Man",
        protocolTitle = "Tony Stark // Mark VII",
        heroProtocol = MarvelHeroProtocol.IRON_MAN,
        coreGeometry = CoreGeometry.ARC_REACTOR,
        colors = ThemeColors(
            primary = Color(0xFF00E5FF),
            secondary = Color(0xFFFF2A42),
            bright = Color(0xFF80F7FF),
            glow = Color(0x6600E5FF),
            subtle = Color(0x1F00E5FF),
            surface = Color(0xEE060C16),
            surfaceDark = Color(0xFF03070E),
            surfaceCard = Color(0xD90A1222),
            hudBorder = Color(0x4000E5FF),
            hudBorderBright = Color(0x9900E5FF),
            textAccent = Color(0xFF00E5FF),
            tickColor = Color(0x6600E5FF),
            glassTint = Color(0x1400E5FF)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "STARK OS // MARK VII",
            statusBadge = "JARVIS PROTOCOL ONLINE",
            searchHint = "Stark Archives",
            dashboardTitle = "STARK OS // SYSTEM DIAGNOSTICS"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF041A22),
            bottomColor = Color(0xFF010508),
            ambientEdgeGlow = Color(0x6600E5FF)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "iron_man"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 20f,
            borderAlpha = 0.45f,
            pillText = "▲ ALL APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Stark Archive or speak...",
            categoryAccent = Color(0xFF00E5FF),
            activeTabIndicator = Color(0xFF00E5FF)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "STARK OS // JARVIS DIAGNOSTICS",
            subtitle = "MARK VII HARDWARE TELEMETRY",
            blueprintLabel = "MARK VII AVIONICS"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.0f,
            pulseDurationMs = 2000,
            burstDurationMs = 240,
            particleDensity = 0.6f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 2. CAPTAIN AMERICA — Shield geometry, blue/red/silver
    // ========================================================================
    val CAPTAIN_AMERICA = MarvelTheme(
        id = "captain_america",
        name = "Captain America",
        heroName = "Captain America",
        protocolTitle = "Steve Rogers // Star Shield",
        heroProtocol = MarvelHeroProtocol.CAPTAIN_AMERICA,
        coreGeometry = CoreGeometry.STAR_SHIELD,
        colors = ThemeColors(
            primary = Color(0xFF2979FF),
            secondary = Color(0xFFFF1744),
            bright = Color(0xFF82B1FF),
            glow = Color(0x662979FF),
            subtle = Color(0x1F2979FF),
            surface = Color(0xEE060D1A),
            surfaceDark = Color(0xFF03050B),
            surfaceCard = Color(0xD9081226),
            hudBorder = Color(0x402979FF),
            hudBorderBright = Color(0x992979FF),
            textAccent = Color(0xFF82B1FF),
            tickColor = Color(0x662979FF),
            glassTint = Color(0x142979FF)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "SHIELD OS // FIRST AVENGER",
            statusBadge = "DEFENSE GRID SECURE",
            searchHint = "Strategic Archives",
            dashboardTitle = "SHIELD // TACTICAL COMMAND"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF0B172E),
            bottomColor = Color(0xFF020409),
            ambientEdgeGlow = Color(0x662979FF)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "captain_america"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 18f,
            borderAlpha = 0.45f,
            pillText = "▲ STRATEGIC APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Strategic Index or speak...",
            categoryAccent = Color(0xFF2979FF),
            activeTabIndicator = Color(0xFF82B1FF)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "SHIELD // HELI-CARRIER MONITOR",
            subtitle = "TACTICAL DEFENSE TELEMETRY",
            blueprintLabel = "VIBRANIUM SHIELD MATRIX"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.0f,
            pulseDurationMs = 2400,
            burstDurationMs = 260,
            particleDensity = 0.4f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 3. THOR — Lightning, cosmic energy and Asgardian gold
    // ========================================================================
    val THOR = MarvelTheme(
        id = "thor",
        name = "Thor",
        heroName = "Thor",
        protocolTitle = "Asgard // Mjolnir Thunder",
        heroProtocol = MarvelHeroProtocol.THOR,
        coreGeometry = CoreGeometry.MJOLNIR_RUNIC,
        colors = ThemeColors(
            primary = Color(0xFFFFD700),
            secondary = Color(0xFF00E5FF),
            bright = Color(0xFFFFE57F),
            glow = Color(0x66FFD700),
            subtle = Color(0x1FFFD700),
            surface = Color(0xEE141006),
            surfaceDark = Color(0xFF070501),
            surfaceCard = Color(0xD91E1809),
            hudBorder = Color(0x40FFD700),
            hudBorderBright = Color(0x99FFD700),
            textAccent = Color(0xFFFFE57F),
            tickColor = Color(0x66FFD700),
            glassTint = Color(0x14FFD700)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "ASGARD OS // MJOLNIR THUNDER",
            statusBadge = "BIFROST PORTAL STABLE",
            searchHint = "Realm Grimoire",
            dashboardTitle = "HEIMDALL // OBSERVATORY READOUT"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF1F1603),
            bottomColor = Color(0xFF04060E),
            ambientEdgeGlow = Color(0x66FFD700)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "thor"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 22f,
            borderAlpha = 0.45f,
            pillText = "▲ REALM APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Realm Archives or speak...",
            categoryAccent = Color(0xFFFFD700),
            activeTabIndicator = Color(0xFFFFE57F)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "ASGARD // HEIMDALL OBSERVATORY",
            subtitle = "NINE REALMS TELEMETRY",
            blueprintLabel = "MJOLNIR RUNIC RESONANCE"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.1f,
            pulseDurationMs = 1800,
            burstDurationMs = 220,
            particleDensity = 0.7f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 4. BLACK PANTHER — Wakandan tech and purple vibranium effects
    // ========================================================================
    val BLACK_PANTHER = MarvelTheme(
        id = "black_panther",
        name = "Black Panther",
        heroName = "Black Panther",
        protocolTitle = "Wakanda // Kinetic Panther",
        heroProtocol = MarvelHeroProtocol.BLACK_PANTHER,
        coreGeometry = CoreGeometry.VIBRANIUM_WEAVE,
        colors = ThemeColors(
            primary = Color(0xFFB388FF),
            secondary = Color(0xFFE0AAFF),
            bright = Color(0xFFD1B3FF),
            glow = Color(0x66B388FF),
            subtle = Color(0x1FB388FF),
            surface = Color(0xEE0E0818),
            surfaceDark = Color(0xFF05020A),
            surfaceCard = Color(0xD9160C26),
            hudBorder = Color(0x40B388FF),
            hudBorderBright = Color(0x99B388FF),
            textAccent = Color(0xFFD1B3FF),
            tickColor = Color(0x66B388FF),
            glassTint = Color(0x14B388FF)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "WAKANDA OS // KINETIC PANTHER",
            statusBadge = "VIBRANIUM WEAVE ACTIVE",
            searchHint = "Bast Knowledge Base",
            dashboardTitle = "WAKANDA // DESIGN GROUP TELEMETRY"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF180A2B),
            bottomColor = Color(0xFF04010A),
            ambientEdgeGlow = Color(0x66B388FF)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "black_panther"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 20f,
            borderAlpha = 0.45f,
            pillText = "▲ WAKANDA APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Wakandan Archives or speak...",
            categoryAccent = Color(0xFFB388FF),
            activeTabIndicator = Color(0xFFD1B3FF)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "WAKANDA // SHURI DESIGN GROUP",
            subtitle = "KINETIC VIBRANIUM GRID",
            blueprintLabel = "NANO-KINETIC SUIT HARNESS"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.0f,
            pulseDurationMs = 2100,
            burstDurationMs = 250,
            particleDensity = 0.5f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 5. SPIDER-MAN — Web geometry, red/blue motion effects
    // ========================================================================
    val SPIDER_MAN = MarvelTheme(
        id = "spider_man",
        name = "Spider-Man",
        heroName = "Spider-Man",
        protocolTitle = "Peter Parker // Iron Spider",
        heroProtocol = MarvelHeroProtocol.SPIDER_MAN,
        coreGeometry = CoreGeometry.SPIDER_MATRIX,
        colors = ThemeColors(
            primary = Color(0xFFFF1744),
            secondary = Color(0xFF00B0FF),
            bright = Color(0xFFFF5252),
            glow = Color(0x66FF1744),
            subtle = Color(0x1FFF1744),
            surface = Color(0xEE14080B),
            surfaceDark = Color(0xFF080204),
            surfaceCard = Color(0xD9220C12),
            hudBorder = Color(0x40FF1744),
            hudBorderBright = Color(0x99FF1744),
            textAccent = Color(0xFFFF5252),
            tickColor = Color(0x66FF1744),
            glassTint = Color(0x14FF1744)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "WEB MATRIX // IRON SPIDER",
            statusBadge = "NEURAL WEB SYNCD",
            searchHint = "City Mesh Network",
            dashboardTitle = "KAREN OS // SUIT DIAGNOSTICS"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF28070F),
            bottomColor = Color(0xFF040106),
            ambientEdgeGlow = Color(0x66FF1744)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "spider_man"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 16f,
            borderAlpha = 0.45f,
            pillText = "▲ WEB APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Web Network or speak...",
            categoryAccent = Color(0xFFFF1744),
            activeTabIndicator = Color(0xFFFF5252)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "KAREN OS // NANO SUIT TELEMETRY",
            subtitle = "RADIAL WEB TRAFFICKING",
            blueprintLabel = "WEB SHOOTER PRESSURE"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.15f,
            pulseDurationMs = 1700,
            burstDurationMs = 200,
            particleDensity = 0.6f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 6. DOCTOR STRANGE — Mystic rings, portals and orange energy
    // ========================================================================
    val DOCTOR_STRANGE = MarvelTheme(
        id = "doctor_strange",
        name = "Doctor Strange",
        heroName = "Doctor Strange",
        protocolTitle = "Sanctum // Eye of Agamotto",
        heroProtocol = MarvelHeroProtocol.DOCTOR_STRANGE,
        coreGeometry = CoreGeometry.EYE_OF_AGAMOTTO,
        colors = ThemeColors(
            primary = Color(0xFFFF6D00),
            secondary = Color(0xFF00E676),
            bright = Color(0xFFFFAB40),
            glow = Color(0x66FF6D00),
            subtle = Color(0x1FFF6D00),
            surface = Color(0xEE160C04),
            surfaceDark = Color(0xFF070401),
            surfaceCard = Color(0xD9241407),
            hudBorder = Color(0x40FF6D00),
            hudBorderBright = Color(0x99FF6D00),
            textAccent = Color(0xFFFFAB40),
            tickColor = Color(0x66FF6D00),
            glassTint = Color(0x14FF6D00)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "SANCTUM OS // AGAMOTTO",
            statusBadge = "DIMENSIONAL BARRIER SECURE",
            searchHint = "Sanctum Grimoire",
            dashboardTitle = "KAMAR-TAJ // ASTRAL TELEMETRY"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF241002),
            bottomColor = Color(0xFF030704),
            ambientEdgeGlow = Color(0x66FF6D00)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "doctor_strange"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 24f,
            borderAlpha = 0.45f,
            pillText = "▲ MYSTIC APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Sanctum Grimoire or speak...",
            categoryAccent = Color(0xFFFF6D00),
            activeTabIndicator = Color(0xFFFFAB40)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "KAMAR-TAJ // ASTRAL SENSORS",
            subtitle = "TAO MANDALA DIMENSIONAL FLUX",
            blueprintLabel = "EYE OF AGAMOTTO TIME MATRIX"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 0.95f,
            pulseDurationMs = 2600,
            burstDurationMs = 280,
            particleDensity = 0.65f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 7. HULK — Gamma bio-radiation rings, titan kinetic compression
    // ========================================================================
    val HULK = MarvelTheme(
        id = "hulk",
        name = "Hulk",
        heroName = "Hulk",
        protocolTitle = "Bruce Banner // Gamma Titan",
        heroProtocol = MarvelHeroProtocol.HULK,
        coreGeometry = CoreGeometry.GAMMA_FIST,
        colors = ThemeColors(
            primary = Color(0xFF16A34A),
            secondary = Color(0xFF9333EA),
            bright = Color(0xFF4ADE80),
            glow = Color(0x6616A34A),
            subtle = Color(0x1F16A34A),
            surface = Color(0xEE061209),
            surfaceDark = Color(0xFF020803),
            surfaceCard = Color(0xD90A1F10),
            hudBorder = Color(0x4016A34A),
            hudBorderBright = Color(0x9916A34A),
            textAccent = Color(0xFF4ADE80),
            tickColor = Color(0x6616A34A),
            glassTint = Color(0x1416A34A)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "GAMMA OS // TITAN PROTOCOL",
            statusBadge = "BIO-CONTAINMENT 100%",
            searchHint = "Banner Lab Research",
            dashboardTitle = "CULVER INSTITUTE // BIO READOUT"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF052110),
            bottomColor = Color(0xFF020904),
            ambientEdgeGlow = Color(0x6616A34A)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "hulk"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 16f,
            borderAlpha = 0.45f,
            pillText = "▲ TITAN APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Lab Archives or speak...",
            categoryAccent = Color(0xFF16A34A),
            activeTabIndicator = Color(0xFF4ADE80)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "CULVER LAB // GAMMA METRICS",
            subtitle = "BIO-CONTAINMENT AND KINETIC OUTPUT",
            blueprintLabel = "CELLULAR GAMMA COMPRESSION"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.0f,
            pulseDurationMs = 2300,
            burstDurationMs = 250,
            particleDensity = 0.45f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 8. BLACK WIDOW — Red Room tactical telemetry, hourglass precision
    // ========================================================================
    val BLACK_WIDOW = MarvelTheme(
        id = "black_widow",
        name = "Black Widow",
        heroName = "Black Widow",
        protocolTitle = "Natasha Romanoff // Red Room",
        heroProtocol = MarvelHeroProtocol.BLACK_WIDOW,
        coreGeometry = CoreGeometry.BLACK_WIDOW_HOURGLASS,
        colors = ThemeColors(
            primary = Color(0xFFFF2E4C),
            secondary = Color(0xFFF59E0B),
            bright = Color(0xFFFF6677),
            glow = Color(0x66FF2E4C),
            subtle = Color(0x1FFF2E4C),
            surface = Color(0xEE120608),
            surfaceDark = Color(0xFF080203),
            surfaceCard = Color(0xD91E0A0E),
            hudBorder = Color(0x40FF2E4C),
            hudBorderBright = Color(0x99FF2E4C),
            textAccent = Color(0xFFFF6677),
            tickColor = Color(0x66FF2E4C),
            glassTint = Color(0x14FF2E4C)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "RED ROOM // TACTICAL SPEC",
            statusBadge = "COVERT LINK SECURE",
            searchHint = "Classified Dossiers",
            dashboardTitle = "TACTICAL // INTEL COMMAND"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF22050B),
            bottomColor = Color(0xFF050102),
            ambientEdgeGlow = Color(0x66FF2E4C)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "black_widow"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 18f,
            borderAlpha = 0.45f,
            pillText = "▲ COVERT APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Classified Dossiers...",
            categoryAccent = Color(0xFFFF2E4C),
            activeTabIndicator = Color(0xFFFF6677)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "TACTICAL // INTEL COMMAND",
            subtitle = "COVERT COMMUNICATIONS MATRIX",
            blueprintLabel = "PRECISION ELECTROSHOCK GAUNTLET"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.1f,
            pulseDurationMs = 1900,
            burstDurationMs = 210,
            particleDensity = 0.4f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    // ========================================================================
    // 9. AVENGERS UNIFIED — Unified Multi-Hero Theme (Clutter-Free, Balanced)
    // ========================================================================
    val AVENGERS_UNIFIED = MarvelTheme(
        id = "avengers_unified",
        name = "Avengers Unified",
        heroName = "Avengers Assemble",
        protocolTitle = "Avengers // Initiative Protocol",
        heroProtocol = MarvelHeroProtocol.AVENGERS_UNIFIED,
        coreGeometry = CoreGeometry.AVENGERS_CHEVRON,
        colors = ThemeColors(
            primary = Color(0xFF1E88E5),      // Initiative Navy / Shield Blue
            secondary = Color(0xFFFFC107),    // Asgardian / Stark Gold Accent
            bright = Color(0xFF64B5F6),       // Holographic Radiant Cyan-Blue
            glow = Color(0x661E88E5),         // Unified Radiant Glow
            subtle = Color(0x1F1E88E5),       // Subtle Foundation Tint
            surface = Color(0xEE080F1A),      // Clean void navy surface
            surfaceDark = Color(0xFF03060B),  // Deep space void
            surfaceCard = Color(0xD90E1B2D),  // Command card elevation
            hudBorder = Color(0x401E88E5),    // Precision border
            hudBorderBright = Color(0x9964B5F6),
            textAccent = Color(0xFF64B5F6),
            tickColor = Color(0x661E88E5),
            glassTint = Color(0x141E88E5)
        ),
        hudConfig = ThemeHudConfig(
            headerPrefix = "AVENGERS PROTOCOL // ASSEMBLE",
            statusBadge = "GLOBAL INITIATIVE ONLINE",
            searchHint = "Global Initiative Index",
            dashboardTitle = "AVENGERS // COMMAND HEADQUARTERS"
        ),
        wallpaperConfig = ThemeWallpaperConfig(
            defaultPreset = WallpaperPreset.MARVEL_HERO,
            topColor = Color(0xFF0B1728),
            bottomColor = Color(0xFF02050A),
            ambientEdgeGlow = Color(0x661E88E5)
        ),
        iconTreatment = ThemeIconTreatment(
            cornerRadius = 14.dp,
            borderAlpha = 0.85f,
            hasGlow = true,
            insigniaType = "avengers_unified"
        ),
        dockStyle = ThemeDockStyle(
            cutCornerSize = 20f,
            borderAlpha = 0.45f,
            pillText = "▲ AVENGERS APPS"
        ),
        drawerStyle = ThemeDrawerStyle(
            searchPlaceholder = "Search Global Index or speak...",
            categoryAccent = Color(0xFF1E88E5),
            activeTabIndicator = Color(0xFF64B5F6)
        ),
        dashboardStyle = ThemeDashboardStyle(
            title = "AVENGERS // INITIATIVE COMMAND",
            subtitle = "UNITED SUPERHERO TELEMETRY",
            blueprintLabel = "COMPOUND DEFENSE GRID"
        ),
        animationConfig = ThemeAnimationConfig(
            speedMultiplier = 1.0f,
            pulseDurationMs = 2100,
            burstDurationMs = 240,
            particleDensity = 0.5f
        ),
        soundHapticConfig = ThemeSoundHapticConfig(
            soundEnabledDefault = false,
            hapticEnabledDefault = true
        )
    )

    init {
        registerTheme(IRON_MAN)
        registerTheme(CAPTAIN_AMERICA)
        registerTheme(THOR)
        registerTheme(BLACK_PANTHER)
        registerTheme(SPIDER_MAN)
        registerTheme(DOCTOR_STRANGE)
        registerTheme(HULK)
        registerTheme(BLACK_WIDOW)
        registerTheme(AVENGERS_UNIFIED)
    }

    /**
     * Extensible theme registration method.
     */
    fun registerTheme(theme: MarvelTheme) {
        registeredThemes[theme.id.lowercase()] = theme
    }

    /**
     * Default theme for the launcher (Iron Man / Stark OS).
     */
    val defaultTheme: MarvelTheme
        get() = IRON_MAN

    /**
     * Safe fallback theme if any active theme or asset fails.
     */
    val fallbackTheme: MarvelTheme
        get() = IRON_MAN

    /**
     * Retrieves a theme by its unique ID.
     * Guaranteed safe fallback: if the ID is unknown or null, returns [defaultTheme].
     */
    fun getTheme(id: String?): MarvelTheme {
        if (id.isNullOrBlank()) return defaultTheme
        return registeredThemes[id.lowercase()] ?: defaultTheme
    }

    /**
     * Retrieves the corresponding theme for a given [MarvelHeroProtocol].
     */
    fun getThemeForProtocol(protocol: MarvelHeroProtocol): MarvelTheme {
        return getTheme(protocol.id)
    }

    /**
     * Returns all registered Marvel themes in display order.
     */
    fun getAllThemes(): List<MarvelTheme> {
        return listOf(
            IRON_MAN,
            CAPTAIN_AMERICA,
            THOR,
            BLACK_PANTHER,
            SPIDER_MAN,
            DOCTOR_STRANGE,
            HULK,
            BLACK_WIDOW,
            AVENGERS_UNIFIED
        )
    }
}
