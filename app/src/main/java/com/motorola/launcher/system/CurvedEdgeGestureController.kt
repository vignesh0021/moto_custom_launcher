package com.motorola.launcher.system

import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.View

/**
 * Manages systemGestureExclusion zones along the 53-degree curved pOLED waterfall edges
 * of the Motorola Edge 50 Fusion / Pro to eliminate false palm touches during one-handed gestures.
 */
class CurvedEdgeGestureController(private val decorView: View) : View.OnLayoutChangeListener {

    companion object {
        private const val TAG = "CurvedEdgeGesture"
        // 28dp curved waterfall touch rejection zone on each edge
        private const val CURVED_EDGE_WIDTH_DP = 28
        // Android enforces max 200dp vertical exclusion per edge
        private const val MAX_EXCLUSION_HEIGHT_DP = 200
    }

    private val exclusionRects = ArrayList<Rect>(2)
    private val leftRect = Rect()
    private val rightRect = Rect()

    init {
        decorView.addOnLayoutChangeListener(this)
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        val width = right - left
        val height = bottom - top

        if (width <= 0 || height <= 0) return

        if (width != (oldRight - oldLeft) || height != (oldBottom - oldTop)) {
            updateExclusionZones(width, height)
        }
    }

    private fun updateExclusionZones(width: Int, height: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val dm: DisplayMetrics = decorView.resources.displayMetrics
        val density = dm.density

        val edgeWidthPx = (CURVED_EDGE_WIDTH_DP * density).toInt()
        val exclusionHeightPx = (MAX_EXCLUSION_HEIGHT_DP * density).toInt()

        // Place exclusion rects in the lower palm contact area (55% down the screen)
        val palmCenterY = (height * 0.60f).toInt()
        val topY = (palmCenterY - exclusionHeightPx / 2).coerceAtLeast(0)
        val bottomY = (topY + exclusionHeightPx).coerceAtMost(height)

        leftRect.set(0, topY, edgeWidthPx, bottomY)
        rightRect.set(width - edgeWidthPx, topY, width, bottomY)

        exclusionRects.clear()
        exclusionRects.add(leftRect)
        exclusionRects.add(rightRect)

        try {
            decorView.systemGestureExclusionRects = exclusionRects
            Log.d(TAG, "Configured curved palm exclusion: Left=$leftRect, Right=$rightRect")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed setting systemGestureExclusionRects: ${t.message}")
        }
    }

    fun detach() {
        decorView.removeOnLayoutChangeListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            decorView.systemGestureExclusionRects = emptyList()
        }
    }
}
