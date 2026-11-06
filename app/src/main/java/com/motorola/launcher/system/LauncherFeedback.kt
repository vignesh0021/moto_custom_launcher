package com.motorola.launcher.system

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Centralized haptic + sound feedback for launcher interactions.
 * Respects user customization toggles; zero-cost no-op when both disabled.
 */
object LauncherFeedback {

    private var toneGenerator: ToneGenerator? = null

    fun performFeedback(
        view: View,
        hapticEnabled: Boolean,
        soundEnabled: Boolean,
        isLongPress: Boolean = false
    ) {
        if (hapticEnabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.performHapticFeedback(
                    if (isLongPress) HapticFeedbackConstants.LONG_PRESS
                    else HapticFeedbackConstants.CONFIRM
                )
            } else {
                @Suppress("DEPRECATION")
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
        if (soundEnabled) {
            try {
                if (toneGenerator == null) {
                    toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 60)
                }
                toneGenerator?.startTone(
                    if (isLongPress) ToneGenerator.TONE_PROP_BEEP else ToneGenerator.TONE_PROP_ACK,
                    80
                )
            } catch (_: Exception) {
                // tone gen unavailable on some devices
            }
        }
    }
}
