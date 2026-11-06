package com.motorola.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanGlow
import com.motorola.launcher.ui.theme.StarkGold
import kotlin.math.cos
import kotlin.math.sin

/**
 * Quantum Resonance Overlay — hero-specific energy particle fields and resonance rings
 * that activate behind the Reactor Core when charging or battery is high.
 * Zero allocation on the idle path; particles only render when isActive is true.
 */
@Composable
fun QuantumResonanceOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 320.dp
) {
    val marvelTheme = LocalMarvelTheme.current
    val transition = rememberInfiniteTransition(label = "resonance")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    if (!isActive) return

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val maxR = this.size.minDimension / 2f

        // Resonance rings
        for (i in 1..3) {
            val r = maxR * (0.55f + i * 0.15f) * pulse
            drawCircle(
                color = marvelTheme.colors.glow.copy(alpha = 0.25f / i),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Rotating particle field
        val particleCount = 24
        for (i in 0 until particleCount) {
            val angle = Math.toRadians((rotation + i * (360f / particleCount)).toDouble())
            val r = maxR * (0.4f + (i % 5) * 0.08f)
            val x = cx + (cos(angle) * r).toFloat()
            val y = cy + (sin(angle) * r).toFloat()
            val pColor = if (i % 3 == 0) StarkGold else StarkCyan
            drawCircle(
                color = pColor.copy(alpha = 0.5f),
                radius = 2.5.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Center glow core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(StarkCyanGlow, Color.Transparent),
                center = Offset(cx, cy),
                radius = maxR * 0.35f
            ),
            radius = maxR * 0.35f,
            center = Offset(cx, cy)
        )
    }
}
