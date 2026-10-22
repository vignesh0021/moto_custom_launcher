package com.motorola.launcher.system

import android.app.Activity
import android.app.WallpaperManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import java.lang.reflect.Method

/**
 * Controller for hardware-backed live wallpaper zooming and parallax offset integration.
 * Executes low-overhead updates to the SurfaceFlinger wallpaper layer with reflection caching and IPC coalescing.
 */
class WallpaperZoomController(private val activity: Activity) {

    companion object {
        private const val TAG = "WallpaperZoomController"
        private const val MAX_WALLPAPER_ZOOM = 0.22f // 22% zoom out on full drawer expansion
        private const val ZOOM_DELTA_THRESHOLD = 0.005f

        // Cached reflection method for Android 12+ (API 31+) WallpaperManager.setWallpaperZoomOut
        private val setWallpaperZoomOutMethod: Method? by lazy {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    WallpaperManager::class.java.getMethod(
                        "setWallpaperZoomOut",
                        IBinder::class.java,
                        Float::class.javaPrimitiveType
                    ).apply { isAccessible = true }
                } catch (t: Throwable) {
                    Log.d(TAG, "setWallpaperZoomOut reflection unavailable: ${t.message}")
                    null
                }
            } else {
                null
            }
        }
    }

    private val wallpaperManager: WallpaperManager by lazy {
        WallpaperManager.getInstance(activity)
    }

    private var cachedWindowToken: IBinder? = null
    private var lastZoomApplied = -1.0f

    /**
     * Retrieves the window token without allocating on each frame.
     */
    private fun getWindowToken(): IBinder? {
        if (cachedWindowToken == null) {
            val decorView = activity.window?.peekDecorView()
            cachedWindowToken = decorView?.windowToken
        }
        return cachedWindowToken
    }

    /**
     * Updates wallpaper zoom out level based on App Drawer expansion progress [0.0f..1.0f].
     * Coalesces updates to prevent binder IPC saturation while guaranteeing exact 0.0f reset.
     */
    fun updateDrawerProgress(progress: Float) {
        val token = getWindowToken() ?: return
        val clampedProgress = progress.coerceIn(0.0f, 1.0f)

        // Fast cubic-out interpolation: 1 - (1 - p)^3
        val inv = 1.0f - clampedProgress
        val interpolated = 1.0f - (inv * inv * inv)
        val targetZoom = interpolated * MAX_WALLPAPER_ZOOM

        val isReset = clampedProgress <= 0.001f
        val isFull = clampedProgress >= 0.999f
        val delta = kotlin.math.abs(targetZoom - lastZoomApplied)

        // Coalesce updates unless crossing threshold or reaching exact reset/full states
        if (delta < ZOOM_DELTA_THRESHOLD && !isReset && !isFull) {
            return
        }

        val appliedZoom = if (isReset) 0.0f else if (isFull) MAX_WALLPAPER_ZOOM else targetZoom
        lastZoomApplied = appliedZoom

        val method = setWallpaperZoomOutMethod ?: return
        try {
            method.invoke(wallpaperManager, token, appliedZoom)
        } catch (_: Throwable) {
            // Graceful fallback if system API throws or token becomes invalid
        }
    }

    /**
     * Updates horizontal workspace wallpaper parallax offset.
     */
    fun setWorkspaceOffset(pageOffset: Float, numPages: Int) {
        val token = getWindowToken() ?: return
        try {
            if (numPages <= 1) {
                wallpaperManager.setWallpaperOffsets(token, 0.5f, 0.0f)
                return
            }
            val step = 1.0f / (numPages - 1).toFloat()
            wallpaperManager.setWallpaperOffsetSteps(step, 1.0f)
            wallpaperManager.setWallpaperOffsets(token, pageOffset.coerceIn(0.0f, 1.0f), 0.0f)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed setting wallpaper offset: ${t.message}")
        }
    }

    /**
     * Forwards tap gesture down to interactive live wallpapers.
     */
    fun forwardTapToWallpaper(x: Int, y: Int) {
        val token = getWindowToken() ?: return
        try {
            wallpaperManager.sendWallpaperCommand(
                token,
                WallpaperManager.COMMAND_TAP,
                x,
                y,
                0,
                null
            )
        } catch (e: Throwable) {
            Log.w(TAG, "Failed sending tap to wallpaper: ${e.message}")
        }
    }

    fun onWindowAttached(view: View) {
        cachedWindowToken = view.windowToken
    }

    fun onWindowDetached() {
        cachedWindowToken = null
        lastZoomApplied = -1.0f
    }
}
