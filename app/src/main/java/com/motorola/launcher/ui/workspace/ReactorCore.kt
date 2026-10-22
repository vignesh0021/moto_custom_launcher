package com.motorola.launcher.ui.workspace

import android.graphics.Paint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkCyanGlow
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkGold
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.StarkIceBlue
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.StarkReticleColor
import com.motorola.launcher.ui.theme.StarkTickColor
import com.motorola.launcher.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural Arc Reactor Command Center Core for STARK OS.
 * Features multi-layered concentric technical rings, radial magnetic coils,
 * real hardware battery gauge integration, and an interactive diagnostic trigger.
 */
@Composable
fun ReactorCore(
    telemetry: SystemTelemetryState,
    onCoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val view = LocalView.current
    val infiniteTransition = rememberInfiniteTransition(label = "reactorTransition")

    // Slow, power-efficient rotation for the intermediate data ring (24 seconds per full loop)
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactorDataRingRotation"
    )

    // Gentle energy pulse when charging
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = if (telemetry.isCharging) 1.0f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (telemetry.isCharging) 1400 else 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "reactorEnergyPulse"
    )

    val coreColor = when {
        telemetry.isCharging -> StarkEmerald
        telemetry.batteryLevel <= 15 -> StarkReactorRed
        else -> StarkCyan
    }

    val coreGlowColor = when {
        telemetry.isCharging -> StarkEmerald.copy(alpha = 0.5f)
        telemetry.batteryLevel <= 15 -> StarkReactorRed.copy(alpha = 0.5f)
        else -> StarkCyanGlow
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onCoreClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // 1. Draw Outer Radial Scale & Technical Ticks (Static, low overhead)
            drawOuterTicks(center, radius * 0.96f)

            // 2. Draw Battery Energy Level Circumferential Arc
            drawBatteryArc(
                center = center,
                radius = radius * 0.88f,
                batteryPercent = telemetry.batteryLevel,
                color = coreColor,
                glowColor = coreGlowColor
            )

            // 3. Draw Segmented Magnetic Confinement Coils (10 segments)
            drawMagneticCoilSegments(
                center = center,
                radius = radius * 0.76f,
                segmentCount = 10,
                activeColor = coreColor
            )

            // 4. Draw Rotating Data Segment Ring
            rotate(rotationAngle, center) {
                drawRotatingDataRing(
                    center = center,
                    radius = radius * 0.62f,
                    color = StarkIceBlue.copy(alpha = 0.65f)
                )
            }

            // 5. Draw Inner Confinement Ring with Cardinal Reticle Ticks
            drawInnerConfinementRing(
                center = center,
                radius = radius * 0.48f,
                color = StarkCyan.copy(alpha = 0.5f)
            )

            // 6. Draw Radiant Central Energy Core (Pulsing glowing circle)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f * pulseGlow),
                        coreGlowColor.copy(alpha = 0.15f * pulseGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.42f
                ),
                radius = radius * 0.42f,
                center = center
            )
        }

        // Center Digital Telemetry Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${telemetry.batteryLevel}%",
                color = TextPrimary,
                fontSize = 26.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (telemetry.isCharging) "CHARGING" else "ARC CORE",
                color = coreColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

private fun DrawScope.drawOuterTicks(center: Offset, radius: Float) {
    val totalTicks = 72 // 5-degree increments
    for (i in 0 until totalTicks) {
        val angleDeg = i * (360f / totalTicks)
        val angleRad = (angleDeg * PI / 180f).toFloat()

        val isMajor = i % 6 == 0 // Every 30 degrees
        val isMedium = i % 2 == 0 // Every 10 degrees

        val tickLength = when {
            isMajor -> radius * 0.08f
            isMedium -> radius * 0.05f
            else -> radius * 0.03f
        }

        val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
        val tickColor = when {
            isMajor -> StarkCyanBright
            isMedium -> StarkTickColor
            else -> StarkTickColor.copy(alpha = 0.35f)
        }

        val startX = center.x + (radius - tickLength) * cos(angleRad)
        val startY = center.y + (radius - tickLength) * sin(angleRad)
        val endX = center.x + radius * cos(angleRad)
        val endY = center.y + radius * sin(angleRad)

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }

    // Outer Thin Baseline Ring
    drawCircle(
        color = StarkHudBorder.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawBatteryArc(
    center: Offset,
    radius: Float,
    batteryPercent: Int,
    color: Color,
    glowColor: Color
) {
    val sweepAngle = (batteryPercent.coerceIn(0, 100) / 100f) * 360f
    val strokeWidth = 4.dp.toPx()

    // Background track
    drawArc(
        color = StarkHudBorder.copy(alpha = 0.2f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Active glowing battery arc
    if (sweepAngle > 0f) {
        // Subtle outer glow stroke
        drawArc(
            color = glowColor,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round)
        )

        // Core solid arc
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawMagneticCoilSegments(
    center: Offset,
    radius: Float,
    segmentCount: Int,
    activeColor: Color
) {
    val totalAnglePerSegment = 360f / segmentCount
    val gapAngle = 8f
    val sweepAngle = totalAnglePerSegment - gapAngle
    val coilStrokeWidth = 7.dp.toPx()

    for (i in 0 until segmentCount) {
        val startAngle = (i * totalAnglePerSegment) + (gapAngle / 2f)

        // Draw segmented arc representing magnetic coil housing
        drawArc(
            brush = Brush.sweepGradient(
                listOf(
                    activeColor.copy(alpha = 0.35f),
                    activeColor.copy(alpha = 0.85f),
                    activeColor.copy(alpha = 0.35f)
                ),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = coilStrokeWidth, cap = StrokeCap.Butt)
        )
    }
}

private fun DrawScope.drawRotatingDataRing(
    center: Offset,
    radius: Float,
    color: Color
) {
    val intervals = floatArrayOf(24f, 16f, 8f, 16f)
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(intervals, 0f)
        )
    )
}

private fun DrawScope.drawInnerConfinementRing(
    center: Offset,
    radius: Float,
    color: Color
) {
    // Solid thin confinement ring
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // 4 Cardinal Reticle Crosshair Ticks (0, 90, 180, 270)
    val tickLen = 6.dp.toPx()
    val offsets = listOf(
        Pair(Offset(center.x + radius - tickLen, center.y), Offset(center.x + radius + tickLen, center.y)),
        Pair(Offset(center.x - radius - tickLen, center.y), Offset(center.x - radius + tickLen, center.y)),
        Pair(Offset(center.x, center.y + radius - tickLen), Offset(center.x, center.y + radius + tickLen)),
        Pair(Offset(center.x, center.y - radius - tickLen), Offset(center.x, center.y - radius + tickLen))
    )

    for (tick in offsets) {
        drawLine(
            color = StarkCyanBright,
            start = tick.first,
            end = tick.second,
            strokeWidth = 1.5.dp.toPx()
        )
    }
}
