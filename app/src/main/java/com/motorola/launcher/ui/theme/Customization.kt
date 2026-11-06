package com.motorola.launcher.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * User customization toggles surfaced through LocalCustomization.
 */
data class LauncherCustomization(
    val hapticFeedback: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val showAppLabels: Boolean = true,
    val iconSizeDp: Int = 46,
    val dockGlassOpacity: Float = 0.2f
)

val LocalCustomization = staticCompositionLocalOf { LauncherCustomization() }
