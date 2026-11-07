package com.motorola.launcher.ui.gesture

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.ThemeColorPreset
import kotlin.math.abs

/**
 * Gesture-Driven World Engine for STARK OS.
 *
 * Groundbreaking interaction layers designed for the Motorola 144Hz display:
 * 1. Screen rotation — dynamic orientation sensing (Portrait HUD vs Landscape Bridge).
 * 2. Two-finger pinch / dual-finger drag — cycles Marvel superhero protocols (Iron Man ↔ Spider-Man ↔ Thor, etc.).
 * 3. Screen edge swipe — quick accent color spectrum cycle.
 *
 * Strict Battery-Safe Rule: 0% CPU at idle, no allocations on the hot path.
 */
@Stable
class GestureWorldState(
    initialHero: MarvelHeroProtocol = MarvelHeroProtocol.IRON_MAN,
    initialColor: ThemeColorPreset = ThemeColorPreset.CYAN
) {
    // ——— Orientation Layer ———
    var isLandscape by mutableStateOf(false)
        private set

    // ——— Superhero Protocol Cycling Layer ———
    val heroCycle = listOf(
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

    var heroIndex by mutableStateOf(
        heroCycle.indexOf(initialHero).coerceAtLeast(0)
    )
        private set

    val currentHero: MarvelHeroProtocol
        get() = heroCycle[heroIndex % heroCycle.size]

    // ——— Theme Accent Color Cycling Layer ———
    val colorCycle = listOf(
        ThemeColorPreset.CYAN,
        ThemeColorPreset.PURPLE,
        ThemeColorPreset.RED,
        ThemeColorPreset.AMBER,
        ThemeColorPreset.GREEN
    )

    var colorIndex by mutableStateOf(
        colorCycle.indexOf(initialColor).coerceAtLeast(0)
    )
        private set

    val currentColor: ThemeColorPreset
        get() = colorCycle[colorIndex % colorCycle.size]

    // Transition progress animatable for smooth visual spring feedback
    val transitionProgress = Animatable(0f)

    fun onRotationChanged(landscape: Boolean) {
        if (isLandscape == landscape) return
        isLandscape = landscape
    }

    fun onPinch(scale: Float, onHeroSelected: ((MarvelHeroProtocol) -> Unit)? = null) {
        // Pinch in (scale < 0.85f) cycles forward, pinch out (scale > 1.15f) cycles back
        if (scale < 0.88f) {
            heroIndex = (heroIndex + 1) % heroCycle.size
            onHeroSelected?.invoke(currentHero)
        } else if (scale > 1.12f) {
            heroIndex = (heroIndex - 1 + heroCycle.size) % heroCycle.size
            onHeroSelected?.invoke(currentHero)
        }
    }

    fun onTwoFingerDrag(deltaY: Float, onHeroSelected: ((MarvelHeroProtocol) -> Unit)? = null) {
        if (abs(deltaY) > 80f) {
            if (deltaY > 0) {
                heroIndex = (heroIndex + 1) % heroCycle.size
            } else {
                heroIndex = (heroIndex - 1 + heroCycle.size) % heroCycle.size
            }
            onHeroSelected?.invoke(currentHero)
        }
    }

    fun onEdgeSwipe(direction: EdgeDirection, onColorSelected: ((ThemeColorPreset) -> Unit)? = null) {
        colorIndex = when (direction) {
            EdgeDirection.LEFT -> (colorIndex - 1 + colorCycle.size) % colorCycle.size
            EdgeDirection.RIGHT -> (colorIndex + 1) % colorCycle.size
        }
        onColorSelected?.invoke(currentColor)
    }

    enum class EdgeDirection { LEFT, RIGHT }
}

@Composable
fun rememberGestureWorldState(
    currentHero: MarvelHeroProtocol = MarvelHeroProtocol.IRON_MAN,
    currentColor: ThemeColorPreset = ThemeColorPreset.CYAN
): GestureWorldState {
    val state = remember { GestureWorldState(currentHero, currentColor) }
    LaunchedEffect(currentHero) {
        val idx = state.heroCycle.indexOf(currentHero)
        if (idx >= 0 && idx != state.heroIndex) {
            // sync with external state changes
        }
    }
    return state
}

/**
 * Orientation listener responding instantly to screen rotations without persistent receivers.
 */
@Composable
fun GestureWorldRotationEffect(state: GestureWorldState) {
    val configuration = LocalConfiguration.current
    LaunchedEffect(configuration.orientation) {
        state.onRotationChanged(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
    }
}

/**
 * Two-finger transform gesture (pinch / pan) to cycle heroes.
 */
fun Modifier.heroGestureTransform(
    state: GestureWorldState,
    enabled: Boolean = true,
    onHeroChanged: (MarvelHeroProtocol) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(state) {
    detectTransformGestures(panZoomLock = true) { _, pan, zoom, _ ->
        if (abs(zoom - 1f) > 0.08f) {
            state.onPinch(zoom, onHeroChanged)
        } else if (abs(pan.y) > 50f) {
            state.onTwoFingerDrag(pan.y, onHeroChanged)
        }
    }
}

/**
 * Edge-swipe detector for fast theme accent color switching.
 */
fun Modifier.edgeSwipeGesture(
    state: GestureWorldState,
    enabled: Boolean = true,
    onColorChanged: (ThemeColorPreset) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Main)
            val change = event.changes.firstOrNull() ?: continue
            if (!change.pressed) continue

            val startX = change.position.x
            val startY = change.position.y
            val width = size.width.toFloat()

            val fromLeftEdge = startX < 72f
            val fromRightEdge = startX > width - 72f
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

            if (abs(totalDx) > abs(totalDy) && abs(totalDx) > 75f) {
                val dir = if (totalDx > 0) GestureWorldState.EdgeDirection.RIGHT else GestureWorldState.EdgeDirection.LEFT
                state.onEdgeSwipe(dir, onColorChanged)
            }
        }
    }
}
