package com.motorola.launcher.system

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.Window
import android.view.WindowManager

/**
 * Plumbing for Motorola Edge 50 Fusion / Pro 144Hz curved pOLED display.
 * Locks the window and surface render cadence to 144Hz (6.944ms VSYNC interval).
 */
class Display144HzManager(private val activity: Activity) {

    companion object {
        private const val TAG = "Display144HzManager"
        const val TARGET_REFRESH_RATE = 144.0f
        const val FRAME_BUDGET_MS = 6.944f
    }

    /**
     * Finds and applies the explicit 144Hz Display.Mode on Snapdragon 7s Gen 2 / 7 Gen 3.
     */
    fun enforce144HzMode() {
        val window = activity.window ?: return
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        } ?: return

        val supportedModes = display.supportedModes
        var bestMode144: Display.Mode? = null
        var maxHzFound = 60.0f

        for (mode in supportedModes) {
            val hz = mode.refreshRate
            if (hz > maxHzFound) {
                maxHzFound = hz
            }
            // Match 144Hz panel (accounting for float precision: 143.9f..144.1f)
            if (hz in 143.9f..144.2f) {
                if (bestMode144 == null || (mode.physicalWidth == display.mode.physicalWidth &&
                            mode.physicalHeight == display.mode.physicalHeight)) {
                    bestMode144 = mode
                }
            }
        }

        val layoutParams = window.attributes

        if (bestMode144 != null) {
            Log.i(TAG, "Binding Window to 144Hz Display.Mode id=${bestMode144.modeId} (${bestMode144.physicalWidth}x${bestMode144.physicalHeight}@${bestMode144.refreshRate}Hz)")
            layoutParams.preferredDisplayModeId = bestMode144.modeId
            layoutParams.preferredRefreshRate = TARGET_REFRESH_RATE
        } else {
            Log.w(TAG, "Exact 144Hz Mode not found. Highest available: ${maxHzFound}Hz. Requesting 144Hz preferredRefreshRate.")
            layoutParams.preferredRefreshRate = TARGET_REFRESH_RATE
        }

        window.attributes = layoutParams

        // Android 11+ (API 30..36) Surface Frame Rate Hint
        enforceSurfaceFrameRate(window)
    }

    private fun enforceSurfaceFrameRate(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Request exact 144Hz cadence with CHANGE_FRAME_RATE_ALWAYS to avoid throttling
                window.decorView.post {
                    try {
                        val surfaceControl = window.decorView.rootSurfaceControl
                        if (surfaceControl != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            // Enforce exact 144Hz frame rate via Surface
                            window.setFrameRate(
                                TARGET_REFRESH_RATE,
                                Surface.FRAME_RATE_COMPATIBILITY_EXACT,
                                Surface.CHANGE_FRAME_RATE_ALWAYS
                            )
                            Log.d(TAG, "Window Surface frame-rate locked to $TARGET_REFRESH_RATE Hz")
                        }
                    } catch (e: Throwable) {
                        Log.w(TAG, "Failed to apply Surface.setFrameRate directly: ${e.message}")
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Error configuring Surface frame-rate hints: ${t.message}")
            }
        }
    }
}
