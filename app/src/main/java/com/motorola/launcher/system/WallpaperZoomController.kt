package com.motorola.launcher.system

import android.app.Activity
import android.app.WallpaperManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View

/**
 * Controller for hardware-backed live wallpaper zooming and parallax offset integration.
 * Executes zero-allocation updates directly to the SurfaceFlinger wallpaper layer.
 */
class WallpaperZoomController(private val activity: Activity) {

    companion object {
        private const val TAG = "WallpaperZoomController"
        private const val MAX_WALLPAPER_ZOOM = 0.22f // 22% zoom out on full drawer expansion
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
     * Driven by 360Hz touch input and VSYNC callbacks without allocations.
     */
    fun updateDrawerProgress(progress: Float) {
        val token = getWindowToken() ?: return
        val clampedProgress = progress.coerceIn(0.0f, 1.0f)
        
        // Fast cubic-out interpolation: 1 - (1 - p)^3
        val inv = 1.0f - clampedProgress
        val interpolated = 1.0f - (inv * inv * inv)
        val targetZoom = interpolated * MAX_WALLPAPER_ZOOM

        // Threshold check to avoid redundant binder IPC across VSYNCs
        if (kotlin.math.abs(targetZoom - lastZoomApplied) < 0.001f) {
            return
        }

        lastZoomApplied = targetZoom

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                wallpaperManager.setWallpaperZoomOut(token, targetZoom)
            } catch (t: Throwable) {
                // Ignore binder errors if window token is detached
            }
        }
    }

    /**
     * Updates horizontal workspace wallpaper parallax offset.
     */
    fun setWorkspaceOffset(pageOffset: Float, numPages: Int) {
        val token = getWindowToken() ?: return
        if (numPages <= 1) {
            wallpaperManager.setWallpaperOffsets(token, 0.5f, 0.0f)
            return
        }
        val step = 1.0f / (numPages - 1).toFloat()
        wallpaperManager.setWallpaperOffsetSteps(step, 1.0f)
        wallpaperManager.setWallpaperOffsets(token, pageOffset.coerceIn(0.0f, 1.0f), 0.0f)
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
