package com.motorola.launcher.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motorola.launcher.ui.model.CoreGeometry
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.WallpaperPreset

/**
 * Color tokens defining a superhero theme's visual identity.
 */
@Immutable
data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val bright: Color,
    val glow: Color,
    val subtle: Color,
    val surface: Color = Color(0xEE060C16),
    val surfaceDark: Color = Color(0xFF03070E),
    val surfaceCard: Color = Color(0xD90A1222),
    val hudBorder: Color = primary.copy(alpha = 0.25f),
    val hudBorderBright: Color = primary.copy(alpha = 0.60f),
    val textPrimary: Color = Color(0xFFFFFFFF),
    val textSecondary: Color = Color(0xB3FFFFFF),
    val textAccent: Color = primary,
    val tickColor: Color = primary.copy(alpha = 0.40f),
    val glassTint: Color = primary.copy(alpha = 0.08f)
)

/**
 * HUD & Header configuration tokens.
 */
@Immutable
data class ThemeHudConfig(
    val headerPrefix: String,
    val statusBadge: String,
    val searchHint: String,
    val dashboardTitle: String
)

/**
 * Procedural wallpaper and background tokens.
 */
@Immutable
data class ThemeWallpaperConfig(
    val defaultPreset: WallpaperPreset,
    val topColor: Color,
    val bottomColor: Color,
    val ambientEdgeGlow: Color
)

/**
 * Launcher-specific app icon frame and insignia treatment.
 */
@Immutable
data class ThemeIconTreatment(
    val cornerRadius: Dp = 14.dp,
    val borderAlpha: Float = 0.85f,
    val hasGlow: Boolean = true,
    val insigniaType: String
)

/**
 * Dock styling and action pill tokens.
 */
@Immutable
data class ThemeDockStyle(
    val cutCornerSize: Float = 20f,
    val borderAlpha: Float = 0.45f,
    val glassColor: Color = Color(0x9903070E),
    val pillText: String = "▲ ALL APPS"
)

/**
 * App drawer and search overlay tokens.
 */
@Immutable
data class ThemeDrawerStyle(
    val searchPlaceholder: String,
    val categoryAccent: Color,
    val activeTabIndicator: Color
)

/**
 * System diagnostic dashboard tokens.
 */
@Immutable
data class ThemeDashboardStyle(
    val title: String,
    val subtitle: String,
    val blueprintLabel: String
)

/**
 * Animation and transition configuration tokens.
 */
@Immutable
data class ThemeAnimationConfig(
    val speedMultiplier: Float = 1.0f,
    val pulseDurationMs: Int = 2200,
    val burstDurationMs: Int = 240,
    val particleDensity: Float = 0.5f
)

/**
 * Sound and haptic feedback options (sound disabled by default).
 */
@Immutable
data class ThemeSoundHapticConfig(
    val soundEnabledDefault: Boolean = false,
    val hapticEnabledDefault: Boolean = true,
    val soundProfileId: String = "subtle_click"
)

/**
 * Comprehensive shared theme model for the Multi-Marvel Universe Launcher.
 * Decouples theme configuration from launcher core implementations.
 */
@Immutable
data class MarvelTheme(
    val id: String,
    val name: String,
    val heroName: String,
    val protocolTitle: String,
    val heroProtocol: MarvelHeroProtocol,
    val coreGeometry: CoreGeometry,
    val colors: ThemeColors,
    val hudConfig: ThemeHudConfig,
    val wallpaperConfig: ThemeWallpaperConfig,
    val iconTreatment: ThemeIconTreatment,
    val dockStyle: ThemeDockStyle = ThemeDockStyle(),
    val drawerStyle: ThemeDrawerStyle,
    val dashboardStyle: ThemeDashboardStyle,
    val animationConfig: ThemeAnimationConfig = ThemeAnimationConfig(),
    val soundHapticConfig: ThemeSoundHapticConfig = ThemeSoundHapticConfig()
)
