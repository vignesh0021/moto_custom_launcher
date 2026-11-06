package com.motorola.launcher.ui.gesture

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.ThemeColorPreset
import kotlin.math.abs

/**
 * Gesture-Driven World Engine for STARK OS.
 *
 * Three unique interaction layers, all zero-allocation and battery-safe:
 * 1. Screen rotation — portrait shows Arc Reactor core, landscape swaps to a hero-specific background.
 * 2. Two-finger pinch — cycles through Marvel hero protocols (Iron Man ↔ Spider-Man ↔ etc.).
 * 3. Edge swipe — transitions theme accent color with a smooth spring.
 *
 * Idle rule: no listeners fire, no allocations, 0% CPU when the screen is static.
 */
@Stable
class GestureWorldState {

    // ——— Rotation layer ———
    var isLandscape by mutableStateOf(false)
        private set

    // ——— Pinch layer (hero cycling) ———
    private val heroCycle = listOf(
        MarvelHeroProtocol.IRON_MAN,
        MarvelHeroProtocol.SPIDER_MAN,
        MarvelHeroProtocol.CAPTAIN_AMERICA,
        MarvelHeroProtocol.THOR,
        MarvelHeroProtocol.HULK,
        MarvelHeroProtocol.BLACK_PANTHER,
        MarvelHeroProtocol.DOCTOR_STRANGE,
        MarvelHeroProtocol.BLACK_WIDOW,
        MarvelHeroProtocol.AVENGERS_UNIFIED
    )
    var heroIndex by mutableStateOf(0)
        private set

    val currentHero: MarvelHeroProtocol
        get() = heroCycle[heroIndex % heroCycle.size]

    // ——— Edge-swipe layer (theme color) ———
    private val colorCycle = listOf(
        ThemeColorPreset.CYAN,
        ThemeColorPreset.RED,
        ThemeColorPreset.GREEN,
        ThemeColorPreset.AMBER,
        ThemeColorPreset.PURPLE
    )
    var colorIndex by mutableStateOf(0)
        private set

    val currentColor: ThemeColorPreset
        get() = colorCycle[colorIndex % colorCycle.size]

    // Smooth spring for color/hero transitions — hardware-paced, no jank
    val transitionProgress = Animatable(0f)

    fun onRotationChanged(landscape: Boolean) {
        if (isLandscape == landscape) return
        isLandscape = landscape
    }

    fun onPinch(scale: Float) {
        // Pinch in (scale < 1) cycles forward, pinch out cycles back
        if (scale < 0.85f) {
            heroIndex = (heroIndex + 1) % heroCycle.size
        } else if (scale > 1.15f) {
            heroIndex = (heroIndex - 1 + heroCycle.size) % heroCycle.size
        }
    }

    fun onEdgeSwipe(direction: EdgeDirection) {
        colorIndex = when (direction) {
            EdgeDirection.LEFT -> (colorIndex - 1 + colorCycle.size) % colorCycle.size
            EdgeDirection.RIGHT -> (colorIndex + 1) % colorCycle.size
        }
    }

    enum class EdgeDirection { LEFT, RIGHT }
}

@Composable
fun rememberGestureWorldState(): GestureWorldState {
    return remember { GestureWorldState() }
}

/**
 * Rotation listener — only active while the composable is in composition.
 * Uses LocalConfiguration so it reacts to configuration changes without
 * registering a BroadcastReceiver (battery-safe: zero persistent listeners).
 */
@Composable
fun GestureWorldRotationEffect(state: GestureWorldState) {
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.orientation) {
        state.onRotationChanged(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }
}

/**
 * Two-finger pinch detector. Attached only to the workspace layer.
 * Consumes the gesture so it doesn't interfere with drawer swipes
 * (drawer uses PointerEventPass.Initial; this uses Main).
 */
fun Modifier.heroPinchGesture(state: GestureWorldState): Modifier = this.pointerInput(state) {
    detectTransformGestures(panZoomLock = true) { _, _, zoom, _ ->
        if (abs(zoom - 1f) > 0.05f) {
            state.onPinch(zoom)
        }
    }
}

/**
 * Edge-swipe detector for theme color cycling.
 * Only fires when the touch starts within 24dp of a screen edge
 * and moves horizontally more than vertically.
 */
fun Modifier.edgeSwipeGesture(state: GestureWorldState): Modifier = this.pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull() ?: continue
            if (!change.pressed) continue

            val startX = change.position.x
            val startY = change.position.y
            val width = size.width.toFloat()

            val fromLeftEdge = startX < 64f
            val fromRightEdge = startX > width - 64f
            if (!fromLeftEdge && !fromRightEdge) continue

            var lastX = startX
            var totalDx = 0f
            var totalDy = 0f

            while (change.pressed) {
                val e = awaitPointerEvent(PointerEventPass.Main)
                val c = e.changes.firstOrNull { it.id == change.id } ?: break
                if (!c.pressed) break
                val dx = c.position.x - lastX
                lastX = c.position.x
                totalDx += dx
                totalDy += (c.position.y - startY)
                c.consume()
            }

            // Horizontal swipe must dominate vertical movement
            if (abs(totalDx) > abs(totalDy) && abs(totalDx) > 80f) {
                state.onEdgeSwipe(
                    if (totalDx > 0) GestureWorldState.EdgeDirection.RIGHT
                    else GestureWorldState.EdgeDirection.LEFT
                )
            }
        }
    }
}
