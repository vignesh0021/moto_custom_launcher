package com.motorola.launcher.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * High-aesthetic animated water-glass indicator for the launcher home screen.
 * Shows real-time liquid level and bubble animations, letting the user track hydration
 * at a glance beside Mithra and Amma.
 *
 * Click: Opens dedicated Hydration Dashboard.
 * Long-click: Quick-logs 250 ml of water with haptic feedback.
 */
@Composable
fun AnimatedWaterGlassIndicator(
    progressRatio: Float,
    consumedMl: Int,
    targetMl: Int,
    accentColor: Color = Color(0xFF00E5FF),
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val smoothProgress by animateFloatAsState(
        targetValue = progressRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "water_fill_progress"
    )

    // Liquid wave phase animation
    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        wavePhase.animateTo(
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = accentColor.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xE6081018))
            .border(1.dp, Brush.linearGradient(listOf(accentColor.copy(alpha = 0.6f), Color(0x3300E5FF))), RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onClick()
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated Water Glass Canvas
            Canvas(modifier = Modifier.size(width = 24.dp, height = 34.dp)) {
                val w = size.width
                val h = size.height

                // 1. Draw Glass Outline
                val glassPath = Path().apply {
                    moveTo(w * 0.12f, 2f)
                    lineTo(w * 0.22f, h - 4f)
                    cubicTo(w * 0.25f, h, w * 0.75f, h, w * 0.78f, h - 4f)
                    lineTo(w * 0.88f, 2f)
                    close()
                }

                // Glass container background
                drawPath(glassPath, color = Color(0x2600E5FF))

                // 2. Liquid Fill with sine wave surface
                val fillTop = h - (h - 6f) * smoothProgress - 2f
                val liquidPath = Path().apply {
                    val waveAmp = if (smoothProgress > 0.05f) 2.5f else 0f
                    val waveY = fillTop + sin(wavePhase.value) * waveAmp

                    moveTo(w * 0.22f, h - 4f)
                    cubicTo(w * 0.25f, h, w * 0.75f, h, w * 0.78f, h - 4f)
                    lineTo(w * 0.85f, waveY)

                    // Wave surface
                    val midX = w * 0.5f
                    val wavePeak = waveY + sin(wavePhase.value + PI.toFloat() * 0.5f) * waveAmp
                    quadraticBezierTo(midX, wavePeak, w * 0.15f, waveY)
                    close()
                }

                val waterGradient = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF80F7FF),
                        Color(0xFF00E5FF),
                        Color(0xFF007799)
                    )
                )
                drawPath(liquidPath, brush = waterGradient)

                // 3. Shimmering rim and glass highlights
                drawPath(
                    glassPath,
                    color = Color(0x8080F7FF),
                    style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                )

                // Lip highlight
                drawLine(
                    color = Color.White.copy(alpha = 0.8f),
                    start = Offset(w * 0.15f, 3f),
                    end = Offset(w * 0.35f, 3f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // Text telemetry
            Column {
                Text(
                    text = "${consumedMl}ml",
                    color = Color(0xFFE0F7FA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${(smoothProgress * 100).toInt()}% goal",
                    color = Color(0xFF80DEEA),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
