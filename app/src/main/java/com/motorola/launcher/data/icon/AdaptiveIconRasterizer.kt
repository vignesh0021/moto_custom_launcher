package com.motorola.launcher.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log

/**
 * Off-main-thread rasterizer converting AdaptiveIconDrawable and legacy drawables
 * into immutable GPU-backed Bitmap.Config.HARDWARE textures.
 */
class AdaptiveIconRasterizer(private val context: Context) {

    companion object {
        private const val TAG = "IconRasterizer"
        private const val DEFAULT_ICON_SIZE_DP = 64
    }

    private val targetSizePx: Int

    init {
        val density = context.resources.displayMetrics.density
        targetSizePx = (DEFAULT_ICON_SIZE_DP * density).toInt()
    }

    /**
     * Rasterizes any Drawable into an ARGB_8888 software bitmap suitable for disk serialization and display.
     */
    fun rasterize(drawable: Drawable): Bitmap? {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSizePx
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSizePx
        val size = width.coerceAtLeast(height).coerceIn(targetSizePx, targetSizePx * 2)

        return try {
            val softwareBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(softwareBitmap)

            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)

            softwareBitmap
        } catch (t: Throwable) {
            Log.e(TAG, "Rasterization failed: ${t.message}", t)
            null
        }
    }

    /**
     * Alias for backward compatibility.
     */
    fun rasterizeToHardware(drawable: Drawable): Bitmap? = rasterize(drawable)
}
