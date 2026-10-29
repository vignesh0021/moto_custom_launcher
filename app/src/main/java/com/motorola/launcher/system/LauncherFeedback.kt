package com.motorola.launcher.system

import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import com.motorola.launcher.ui.model.AnimationLevelPreset
import com.motorola.launcher.ui.model.CustomizationSettings

/**
 * Centralized tactile haptics, auditory feedback, and accessibility reduced-motion manager
 * for the Multi-Marvel Launcher.
 */
object LauncherFeedback {

    /**
     * Executes tactile haptic feedback and optional auditory sound click effects.
     * Respects user settings and sound/haptic toggles (sound disabled by default).
     */
    fun performFeedback(
        view: View,
        hapticEnabled: Boolean,
        soundEnabled: Boolean,
        isLongPress: Boolean = false
    ) {
        if (hapticEnabled) {
            val hapticConstant = if (isLongPress) {
                HapticFeedbackConstants.LONG_PRESS
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(hapticConstant)
        }

        if (soundEnabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    /**
     * Returns true if either the user selected the "Low Motion" performance preset
     * or the Android OS accessibility "Remove animations" setting is active.
     */
    fun isReducedMotion(context: Context, customization: CustomizationSettings): Boolean {
        if (customization.animationLevel == AnimationLevelPreset.PERFORMANCE) {
            return true
        }

        return try {
            val resolver = context.contentResolver
            val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)
            val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f)
            animatorScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }
}
