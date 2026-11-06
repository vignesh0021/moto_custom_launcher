package com.motorola.launcher.system

import android.app.Activity
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.Window
import android.view.WindowManager

/**
 * Plumbing for Motorola Edge 50 Fusion / Pro 144Hz curved pOLED display.
 *
 * Battery-safe: 144Hz is applied ONLY while the user is actively gesturing
 * (drawer swipe, pinch, rotation). After INTERACTION_TIMEOUT_MS of inactivity
 * the display falls back to 60Hz to save power. No hardcoded always-on lock.
 */
class Display144HzManager(private val activity: Activity) {

    companion object {
        private const val TAG = "Display144HzManager"
        const val TARGET_REFRESH_RATE = 144.0f
        const val FRAME_BUDGET_MS = 6.944f
        private const val INTERACTION_TIMEOUT_MS = 2_000L
        private const val IDLE_REFRESH_RATE = 60.0f
    }

    private val handler = Handler(Looper.getMainLooper())
    private val idleFallback = object : Runnable {
        override fun run() {
            applyRefreshRate(IDLE_REFRESH_RATE)
        }
    }

    private var highestSupportedHz = 60.0f
    private var mode144Id: Int? = null

    init {
        cacheDisplayModes()
    }

    private fun cacheDisplayModes() {
        try {
            val display = currentDisplay() ?: return
            val supportedModes = display.supportedModes ?: emptyArray()
            for (mode in supportedModes) {
                val hz = mode.refreshRate
                if (hz > highestSupportedHz) highestSupportedHz = hz
                if (hz in 143.9f..144.2f) {
                    mode144Id = mode.modeId
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed caching display modes: ${t.message}")
        }
    }

    private fun currentDisplay(): Display? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                activity.display
            } catch (e: Throwable) {
                @Suppress("DEPRECATION")
                activity.windowManager?.defaultDisplay
            }
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager?.defaultDisplay
        }
    }

    /**
     * Call this when a gesture starts (drawer swipe, pinch, rotation).
     * Immediately bumps to 144Hz and arms the idle fallback timer.
     */
    fun onInteractionStart() {
        handler.removeCallbacks(idleFallback)
        applyRefreshRate(TARGET_REFRESH_RATE)
        handler.postDelayed(idleFallback, INTERACTION_TIMEOUT_MS)
    }

    /**
     * Call on gesture end to start the countdown to 60Hz fallback.
     */
    fun onInteractionEnd() {
        handler.removeCallbacks(idleFallback)
        handler.postDelayed(idleFallback, INTERACTION_TIMEOUT_MS)
    }

    private fun applyRefreshRate(rate: Float) {
        try {
            val window = activity.window ?: return
            val layoutParams = window.attributes
            if (rate >= 143.9f && mode144Id != null) {
                layoutParams.preferredDisplayModeId = mode144Id!!
                layoutParams.preferredRefreshRate = TARGET_REFRESH_RATE
            } else {
                layoutParams.preferredDisplayModeId = 0
                layoutParams.preferredRefreshRate = rate.coerceAtMost(highestSupportedHz).coerceAtLeast(60.0f)
            }
            window.attributes = layoutParams
        } catch (t: Throwable) {
            Log.w(TAG, "Failed applying refresh rate ${rate}Hz: ${t.message}")
        }
    }

    fun release() {
        handler.removeCallbacks(idleFallback)
    }
}
