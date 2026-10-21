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

            // Android 11+ (API 30..36) Surface Frame Rate Hint
            enforceSurfaceFrameRate(window)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed configuring display refresh rate: ${t.message}")
        }
    }

    private fun enforceSurfaceFrameRate(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                // Ensure Window LayoutParams preferredRefreshRate is explicitly locked to 144Hz
                val layoutParams = window.attributes
                layoutParams.preferredRefreshRate = TARGET_REFRESH_RATE
                window.attributes = layoutParams

                // Android 11+ (API 30..36) Surface Frame Rate Hint via ViewRootImpl Surface
                window.decorView.post {
                    try {
                        val viewRootImpl = window.decorView.javaClass.getMethod("getViewRootImpl").invoke(window.decorView)
                        if (viewRootImpl != null) {
                            val surfaceMethod = viewRootImpl.javaClass.getMethod("getSurface")
                            val surface = surfaceMethod.invoke(viewRootImpl) as? Surface
                            if (surface != null && surface.isValid) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    surface.setFrameRate(
                                        TARGET_REFRESH_RATE,
                                        Surface.FRAME_RATE_COMPATIBILITY_DEFAULT,
                                        Surface.CHANGE_FRAME_RATE_ALWAYS
                                    )
                                } else {
                                    surface.setFrameRate(
                                        TARGET_REFRESH_RATE,
                                        Surface.FRAME_RATE_COMPATIBILITY_DEFAULT
                                    )
                                }
                                Log.d(TAG, "Window Surface frame-rate locked to $TARGET_REFRESH_RATE Hz via Surface")
                            }
                        }
                    } catch (e: Throwable) {
                        Log.d(TAG, "Direct Surface frame rate hint skipped: ${e.message}")
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Error configuring Surface frame-rate hints: ${t.message}")
            }
        }
    }
}
