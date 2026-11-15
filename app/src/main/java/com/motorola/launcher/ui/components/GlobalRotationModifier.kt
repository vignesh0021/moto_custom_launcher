package com.motorola.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Very subtle global rotation / breathing effect for the workspace.
 * Extremely low amplitude so it stays battery-safe and non-distracting.
 */
@Composable
fun Modifier.subtleGlobalRotation(enabled: Boolean = true): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "globalRot")
    val rot by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rot"
    )

    this.graphicsLayer {
        rotationZ = rot
        // Keep scale almost 1 to avoid visual noise
        scaleX = 1.002f
        scaleY = 1.002f
    }
}
