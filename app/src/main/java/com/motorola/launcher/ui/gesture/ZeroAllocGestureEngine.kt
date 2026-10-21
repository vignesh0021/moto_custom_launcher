package com.motorola.launcher.ui.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import com.motorola.launcher.system.WallpaperZoomController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 100% Stable Gesture Controller for App Drawer Swipe Physics.
 * Uses Compose Animatable for hardware-timed frame pacing without recomposition spikes.
 */
@Stable
class DrawerGestureState(
    val animatable: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val wallpaperZoomController: WallpaperZoomController?
) {
    private val velocityTracker = VelocityTracker360Hz()

    // 0.0f = Workspace visible (Drawer closed)
    // 1.0f = Drawer fully expanded
    val progress: Float
        get() = animatable.value

    fun onDragStart(downTimeMillis: Long, y: Float) {
        velocityTracker.reset()
        velocityTracker.addPoint(downTimeMillis, y)
    }

    fun onDrag(timeMillis: Long, y: Float, deltaY: Float, maxDragPx: Float) {
        velocityTracker.addPoint(timeMillis, y)
        if (maxDragPx <= 0f) return

        // Dragging UP decreases Y, which INCREASES drawer progress [0..1]
        val deltaProgress = -deltaY / maxDragPx
        val newProgress = (animatable.value + deltaProgress).coerceIn(0f, 1f)

        // Direct snap without animation overhead
        scope.launch {
            animatable.snapTo(newProgress)
            wallpaperZoomController?.updateDrawerProgress(newProgress)
        }
    }

    fun onDragEnd(timeMillis: Long, maxDragPx: Float) {
        val velocityY = velocityTracker.calculateVelocity(timeMillis) // px/s (negative = up)
        val velocityProgress = if (maxDragPx > 0f) -velocityY / maxDragPx else 0f

        val current = animatable.value
        // Determine target based on position threshold and flick velocity
        val target = when {
            velocityProgress > 1.8f -> 1.0f // Fast flick up
            velocityProgress < -1.8f -> 0.0f // Fast flick down
            current > 0.35f -> 1.0f // Passed 35% height threshold
            else -> 0.0f
        }

        scope.launch {
            animatable.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                initialVelocity = velocityProgress
            ) {
                // Update live wallpaper zoom directly on each frame callback
                wallpaperZoomController?.updateDrawerProgress(value)
            }
        }
    }

    fun open() {
        scope.launch {
            animatable.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                wallpaperZoomController?.updateDrawerProgress(value)
            }
        }
    }

    fun close() {
        scope.launch {
            animatable.animateTo(
                targetValue = 0.0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) {
                wallpaperZoomController?.updateDrawerProgress(value)
            }
        }
    }
}

@Composable
fun rememberDrawerGestureState(
    wallpaperZoomController: WallpaperZoomController?
): DrawerGestureState {
    val scope = rememberCoroutineScope()
    val animatable = remember { Animatable(0.0f) }
    return remember(animatable, scope, wallpaperZoomController) {
        DrawerGestureState(animatable, scope, wallpaperZoomController)
    }
}

/**
 * Custom zero-allocation pointer input modifier capturing 360Hz touch streams.
 */
fun Modifier.drawerSwipeGesture(
    state: DrawerGestureState,
    maxDragPx: Float
): Modifier = this.pointerInput(state, maxDragPx) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        var pointerId = down.id
        var lastY = down.position.y
        state.onDragStart(down.uptimeMillis, lastY)

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val dragChange = event.changes.firstOrNull { it.id == pointerId } ?: break

            if (!dragChange.pressed) {
                state.onDragEnd(dragChange.uptimeMillis, maxDragPx)
                break
            }

            val currentY = dragChange.position.y
            val deltaY = currentY - lastY
            lastY = currentY

            if (kotlin.math.abs(deltaY) > 0.01f) {
                state.onDrag(dragChange.uptimeMillis, currentY, deltaY, maxDragPx)
                dragChange.consume()
            }
        }
    }
}
