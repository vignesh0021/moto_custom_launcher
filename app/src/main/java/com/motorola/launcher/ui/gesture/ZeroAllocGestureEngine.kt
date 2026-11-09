package com.motorola.launcher.ui.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.motorola.launcher.system.WallpaperZoomController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 100% Stable Gesture Controller for App Drawer Swipe Physics.
 * Uses Compose Animatable for hardware-timed frame pacing without recomposition spikes.
 * Zero-allocation pointer loop: directly calls snapTo within the pointer input coroutine.
 */
@Stable
class DrawerGestureState(
    val animatable: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
    private val wallpaperZoomController: WallpaperZoomController?
) {
    private val velocityTracker = VelocityTracker360Hz()
    private var animationJob: Job? = null
    private var dragJob: Job? = null
    private val dragChannel = kotlinx.coroutines.channels.Channel<Float>(kotlinx.coroutines.channels.Channel.CONFLATED)
    private var currentDragProgress = 0f

    // Discrete visibility state to prevent root recomposition on every fractional progress frame
    var isDrawerVisible by mutableStateOf(false)
        private set

    // 0.0f = Workspace visible (Drawer closed)
    // 1.0f = Drawer fully expanded
    val progress: Float
        get() = animatable.value

    fun onDragStart(downTimeMillis: Long, y: Float) {
        animationJob?.cancel()
        dragJob?.cancel()
        isDrawerVisible = true
        velocityTracker.reset()
        velocityTracker.addPoint(downTimeMillis, y)
        currentDragProgress = animatable.value

        // Single dedicated coroutine draining the conflated 360Hz drag stream
        dragJob = scope.launch {
            for (p in dragChannel) {
                animatable.snapTo(p)
                wallpaperZoomController?.updateDrawerProgress(p)
            }
        }
    }

    /**
     * Non-suspending, zero-allocation 360Hz touch sample ingest.
     */
    fun onDrag(timeMillis: Long, y: Float, deltaY: Float, maxDragPx: Float) {
        velocityTracker.addPoint(timeMillis, y)
        if (maxDragPx <= 0f) return

        // Dragging UP decreases Y, which INCREASES drawer progress [0..1]
        val deltaProgress = -deltaY / maxDragPx
        currentDragProgress = (currentDragProgress + deltaProgress).coerceIn(0f, 1f)
        dragChannel.trySend(currentDragProgress)
    }

    fun onDragEnd(timeMillis: Long, maxDragPx: Float) {
        dragJob?.cancel()
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

        animationJob?.cancel()
        animationJob = scope.launch {
            try {
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialVelocity = velocityProgress
                ) {
                    wallpaperZoomController?.updateDrawerProgress(value)
                }
            } finally {
                if (target == 0.0f && animatable.value <= 0.001f) {
                    isDrawerVisible = false
                    wallpaperZoomController?.updateDrawerProgress(0.0f)
                }
            }
        }
    }

    fun open() {
        animationJob?.cancel()
        isDrawerVisible = true
        animationJob = scope.launch {
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
        animationJob?.cancel()
        animationJob = scope.launch {
            try {
                animatable.animateTo(
                    targetValue = 0.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) {
                    wallpaperZoomController?.updateDrawerProgress(value)
                }
            } finally {
                if (animatable.value <= 0.001f) {
                    isDrawerVisible = false
                    wallpaperZoomController?.updateDrawerProgress(0.0f)
                }
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
    val touchSlop = viewConfiguration.touchSlop
    awaitEachGesture {
        // Main pass: children (buttons, dock icons) get first chance at the event.
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
        val pointerId = down.id
        val startX = down.position.x
        val startY = down.position.y
        var lastY = startY
        var dragging = false

        while (true) {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

            if (!change.pressed) {
                if (dragging) state.onDragEnd(change.uptimeMillis, maxDragPx)
                break
            }

            // Multi-finger gestures belong to the hero transform detector
            if (event.changes.count { it.pressed } > 1) {
                if (dragging) state.onDragEnd(change.uptimeMillis, maxDragPx)
                break
            }

            val currentY = change.position.y
            if (!dragging) {
                val totalDx = change.position.x - startX
                val totalDy = currentY - startY
                if (kotlin.math.abs(totalDy) > touchSlop &&
                    kotlin.math.abs(totalDy) > kotlin.math.abs(totalDx) * 1.2f
                ) {
                    // Only now does this become a drawer swipe
                    dragging = true
                    state.onDragStart(change.uptimeMillis, currentY)
                    lastY = currentY
                    change.consume()
                } else if (kotlin.math.abs(totalDx) > touchSlop) {
                    // Horizontal movement: not ours (edge swipe / scrolling)
                    break
                }
                continue
            }

            val deltaY = currentY - lastY
            lastY = currentY
            if (kotlin.math.abs(deltaY) > 0.01f) {
                state.onDrag(change.uptimeMillis, currentY, deltaY, maxDragPx)
            }
            change.consume()
        }
    }
}
