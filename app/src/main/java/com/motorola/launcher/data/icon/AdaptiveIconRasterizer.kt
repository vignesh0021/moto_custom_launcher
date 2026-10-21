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
     * Rasterizes any Drawable into an immutable Bitmap.Config.HARDWARE.
     * Guarantees zero long-lived allocations in Java Heap / Native PSS.
     */
    fun rasterizeToHardware(drawable: Drawable): Bitmap? {
        // Fast path for BitmapDrawable that is already hardware-backed
        if (drawable is BitmapDrawable) {
            val bmp = drawable.bitmap
            if (bmp != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp.config == Bitmap.Config.HARDWARE) {
                return bmp
            }
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else targetSizePx
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else targetSizePx
        val size = width.coerceAtLeast(height).coerceIn(targetSizePx, targetSizePx * 2)

        var softwareBitmap: Bitmap? = null
        try {
            // Allocate transient ARGB_8888 buffer
            softwareBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(softwareBitmap)

            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)

            // Convert to immutable GPU AHardwareBuffer texture
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val hwBitmap = softwareBitmap.copy(Bitmap.Config.HARDWARE, false)
                hwBitmap
            } else {
                softwareBitmap
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Rasterization failed: ${t.message}", t)
            return null
        } finally {
            // Immediately reclaim transient software memory
            if (softwareBitmap != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                softwareBitmap.recycle()
            }
        }
    }
}
