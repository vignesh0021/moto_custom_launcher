package com.motorola.launcher.system

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
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
        try {
            val window = activity.window ?: return
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
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

            if (display == null) {
                Log.w(TAG, "Display not yet attached; skipping 144Hz mode lock.")
                return
            }

            val supportedModes = display.supportedModes ?: emptyArray()
            var bestMode144: Display.Mode? = null
            var maxHzFound = 60.0f
            val currentMode = try { display.mode } catch (e: Throwable) { null }

            for (mode in supportedModes) {
                val hz = mode.refreshRate
                if (hz > maxHzFound) {
                    maxHzFound = hz
                }
                // Match 144Hz panel (accounting for float precision: 143.9f..144.2f)
                if (hz in 143.9f..144.2f) {
                    if (bestMode144 == null || (currentMode != null &&
                                mode.physicalWidth == currentMode.physicalWidth &&
                                mode.physicalHeight == currentMode.physicalHeight)) {
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
                Log.w(TAG, "Exact 144Hz Mode not found. Highest available: ${maxHzFound}Hz. Requesting ${maxHzFound}Hz preferredRefreshRate.")
                layoutParams.preferredRefreshRate = maxHzFound.coerceAtLeast(60.0f)
            }

            window.attributes = layoutParams
            Log.i(TAG, "Window preferred display attributes successfully applied.")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed configuring display refresh rate: ${t.message}")
        }
    }
}
