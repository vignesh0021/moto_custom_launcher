package com.motorola.launcher.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.theme.LocalCustomization
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Next-level Quantum Resonance Overlay.
 * Renders hero-specific energy particles and resonance rings that react to system state.
 * Zero-allocation on idle; only active when charging or high telemetry.
 */
@Composable
fun QuantumResonanceOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val customization = LocalCustomization.current
    val hero = customization.heroProtocol
    val infinite = rememberInfiniteTransition(label = "quantum")

    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawQuantumField(hero, phase)
    }
}

private fun DrawScope.drawQuantumField(hero: MarvelHeroProtocol, phase: Float) {
    val center = Offset(size.width / 2f, size.height * 0.42f)
    val radius = size.minDimension * 0.28f
    val color = hero.primary
    val glow = hero.glow

    // Outer resonance rings
    for (i in 0..3) {
        val r = radius * (1.1f + i * 0.18f + phase * 0.12f)
        drawCircle(
            color = color.copy(alpha = 0.18f - i * 0.04f),
            radius = r,
            center = center,
            style = Stroke(width = 1.5f)
        )
    }

    // Hero-specific particle burst
    val particleCount = 24
    for (i in 0 until particleCount) {
        val angle = (i / particleCount.toFloat()) * 2f * PI.toFloat() + phase * 2f * PI.toFloat()
        val dist = radius * (0.65f + (sin(phase * 4f + i) * 0.15f))
        val px = center.x + cos(angle) * dist
        val py = center.y + sin(angle) * dist * 0.7f
        drawCircle(
            color = glow.copy(alpha = 0.55f),
            radius = 2.2f,
            center = Offset(px, py)
        )
    }

    // Core pulse
    drawCircle(
        color = color.copy(alpha = 0.25f + phase * 0.15f),
        radius = radius * 0.35f,
        center = center
    )
}
