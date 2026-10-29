package com.motorola.launcher.ui.workspace

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.model.CoreGeometry
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.StarkRedGlow
import com.motorola.launcher.ui.theme.StarkTickColor
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Universal Marvel Hero All-In-One Command Core.
 *
 * Designed with absolute battery preservation:
 * 1. 0% idle CPU on the home screen when stationary (no infinite canvas recomposition loops).
 * 2. Animations activate exclusively on interaction (tap, burst) or charging state.
 * 3. All metrics unified into the core:
 *    - Left Outer Arc: Battery % with charging glow
 *    - Right Outer Arc: RAM Usage %
 *    - Center: Live Battery percentage + hero protocol readout
 *    - Wing controls: Attached HUD buttons for [ CONTROLS ] and [ SETTINGS ]
 * 4. Procedural Marvel Core Geometries:
 *    - Iron Man: Arc Reactor coils & inverted triangle
 *    - Black Panther: Vibranium kinetic tooth weave & diamond crest
 *    - Captain America: Concentric vibranium shield rings & 5-point star
 *    - Thor: Asgardian runic compass with lightning crackles & triquetra
 *    - Doctor Strange: Mystic Tao mandalas & Eye of Agamotto
 *    - Spider-Man: Nano-spider targeting reticle & 8-axis web matrix
 */
@Composable
fun ReactorCore(
    telemetry: SystemTelemetryState,
    onCoreClick: () -> Unit,
    onOpenQuickControls: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp
) {
    val view = LocalView.current
    val customization = LocalCustomization.current
    val hero = customization.heroProtocol
    val scope = rememberCoroutineScope()
    val touchBurst = remember { Animatable(0f) }

    // Battery conservation: Only animate infinite loop when charging!
    // When idle and discharging, rotation is 0f and pulse is steady -> 0% CPU canvas rendering.
    val isCharging = telemetry.isCharging
    val infiniteTransition = rememberInfiniteTransition(label = "chargingTransition")

    val chargingPulse by if (isCharging) {
        infiniteTransition.animateFloat(
            initialValue = 0.75f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "chargingPulse"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.85f) }
    }

    val primaryColor = hero.primary
    val secondaryColor = hero.secondary
    val glowColor = hero.glow

    val coreColor = when {
        telemetry.isCharging -> StarkEmerald
        telemetry.batteryLevel in 0..15 -> StarkReactorRed
        else -> primaryColor
    }

    val tapFeedback: () -> Unit = {
        if (customization.hapticFeedback) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Core Display
        Box(
            modifier = Modifier
                .size(size)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tapFeedback()
                        scope.launch {
                            touchBurst.snapTo(1f)
                            touchBurst.animateTo(0f, tween(500, easing = FastOutSlowInEasing))
                        }
                        onCoreClick()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val radius = this.size.minDimension / 2f
                val burst = touchBurst.value

                // 1. Outer Technical Calibration Ticks
                drawOuterTicks(center, radius * 0.96f, secondaryColor)

                // 2. Dual Telemetry Orbital Arcs (Left = Battery, Right = RAM)
                // Battery Arc (Left half: 90° to 270°)
                drawBatteryGaugeArc(
                    center = center,
                    radius = radius * 0.86f,
                    batteryPercent = if (telemetry.batteryLevel >= 0) telemetry.batteryLevel else 0,
                    activeColor = coreColor,
                    glowColor = glowColor,
                    burst = burst
                )

                // RAM Arc (Right half: 270° to 90°)
                drawRamGaugeArc(
                    center = center,
                    radius = radius * 0.86f,
                    ramPercent = telemetry.ramPercent,
                    color = primaryColor,
                    glowColor = glowColor
                )

                // 3. Middle Structural HUD Ring
                drawCircle(
                    color = StarkHudBorder.copy(alpha = 0.45f),
                    radius = radius * 0.74f,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                    )
                )

                // 4. Hero-Specific Procedural Geometry Core
                when (hero.coreGeometry) {
                    CoreGeometry.ARC_REACTOR -> drawArcReactorGeometry(center, radius, coreColor, glowColor, burst)
                    CoreGeometry.VIBRANIUM_WEAVE -> drawVibraniumWeaveGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.STAR_SHIELD -> drawStarShieldGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.MJOLNIR_RUNIC -> drawMjolnirRunicGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.GAMMA_FIST -> drawGammaFistGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.EYE_OF_AGAMOTTO -> drawEyeOfAgamottoGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.SPIDER_MATRIX -> drawSpiderMatrixGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.BLACK_WIDOW_HOURGLASS -> drawBlackWidowHourglassGeometry(center, radius, primaryColor, secondaryColor, burst)
                    CoreGeometry.AVENGERS_CHEVRON -> drawAvengersChevronGeometry(center, radius, primaryColor, secondaryColor, burst)
                }

                // 5. Central Radiant Core Backing
                val effectivePulse = if (isCharging) chargingPulse else 0.85f
                val glowAlpha = ((0.32f * effectivePulse) + (burst * 0.4f)).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = glowAlpha),
                            glowColor.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 0.38f
                    ),
                    radius = radius * 0.38f,
                    center = center
                )
            }

            // Central Battery Percentage — Centered dead-center in the Marvel core geometry & arcs
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (telemetry.isCharging) {
                            Text(
                                text = "⚡",
                                color = StarkEmerald,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        }
                        Text(
                            text = if (telemetry.batteryLevel >= 0) "${telemetry.batteryLevel}%" else "--%",
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            style = TextStyle(
                                shadow = Shadow(color = Color.Black, offset = Offset(0f, 2f), blurRadius = 8f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (telemetry.isCharging) "CHARGING" else "BATTERY",
                        color = if (telemetry.isCharging) StarkEmerald else coreColor.copy(alpha = 0.85f),
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        style = TextStyle(
                            shadow = Shadow(color = Color.Black, offset = Offset(0f, 1f), blurRadius = 4f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Attached Core Wing Buttons: [ CONTROLS ] & [ SETTINGS ]
        // Keeping controls inside/attached to the core so wallpaper is completely unblocked.
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoreAttachedPill(
                icon = "⚡",
                label = "CONTROLS",
                color = primaryColor,
                onClick = {
                    tapFeedback()
                    onOpenQuickControls()
                }
            )
            CoreAttachedPill(
                icon = "⚙",
                label = "SETTINGS",
                color = primaryColor,
                onClick = {
                    tapFeedback()
                    onOpenCustomization()
                }
            )
        }
    }
}

/**
 * Compact pill button directly anchored to the Reactor Core.
 */
@Composable
private fun CoreAttachedPill(
    icon: String,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .clip(pillShape)
            .background(Color(0xCC050A14))
            .border(1.dp, color.copy(alpha = 0.5f), pillShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = icon, fontSize = 10.sp, color = color)
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 9.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}

// -------------------------------------------------------------------------------------------------
// TELEMETRY GAUGES (Battery on Left arc, RAM on Right arc)
// -------------------------------------------------------------------------------------------------

private fun DrawScope.drawBatteryGaugeArc(
    center: Offset,
    radius: Float,
    batteryPercent: Int,
    activeColor: Color,
    glowColor: Color,
    burst: Float
) {
    val strokeWidth = 3.5.dp.toPx()
    val arcSpan = 160f // Spans from 100° to 260° (Left side)
    val startAngle = 100f
    val fillSpan = (batteryPercent.coerceIn(0, 100) / 100f) * arcSpan

    // Track
    drawArc(
        color = StarkHudBorder.copy(alpha = 0.25f),
        startAngle = startAngle,
        sweepAngle = arcSpan,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Filled arc
    if (fillSpan > 0f) {
        drawArc(
            color = glowColor.copy(alpha = 0.5f + (burst * 0.4f)),
            startAngle = startAngle,
            sweepAngle = fillSpan,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth * 2f, cap = StrokeCap.Round)
        )
        drawArc(
            color = activeColor,
            startAngle = startAngle,
            sweepAngle = fillSpan,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawRamGaugeArc(
    center: Offset,
    radius: Float,
    ramPercent: Int,
    color: Color,
    glowColor: Color
) {
    val strokeWidth = 3.5.dp.toPx()
    val arcSpan = 160f // Spans from 280° to 80° (Right side)
    val startAngle = 280f
    val fillSpan = (ramPercent.coerceIn(0, 100) / 100f) * arcSpan

    // Track
    drawArc(
        color = StarkHudBorder.copy(alpha = 0.25f),
        startAngle = startAngle,
        sweepAngle = arcSpan,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    // Filled arc
    if (fillSpan > 0f) {
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = fillSpan,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

// -------------------------------------------------------------------------------------------------
// PROCEDURAL MARVEL GEOMETRIES
// -------------------------------------------------------------------------------------------------

/** Iron Man: Arc Reactor magnetic coils + central inverted triangle (▼) */
private fun DrawScope.drawArcReactorGeometry(
    center: Offset,
    radius: Float,
    color: Color,
    glowColor: Color,
    burst: Float
) {
    val coilRadius = radius * 0.64f
    val coilCount = 10
    val totalAngle = 360f / coilCount
    val sweepAngle = totalAngle - 8f

    for (i in 0 until coilCount) {
        val startAngle = (i * totalAngle) + 4f
        val segColor = if (i == 2 || i == 7) StarkReactorRed else color
        drawArc(
            color = segColor.copy(alpha = 0.85f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - coilRadius, center.y - coilRadius),
            size = Size(coilRadius * 2f, coilRadius * 2f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Butt)
        )
    }

    // Central inverted triangle
    val triRadius = radius * 0.28f + (burst * 4f)
    val p1 = Offset(center.x, center.y + triRadius)
    val p2 = Offset(center.x + triRadius * cos((210.0 * PI / 180.0).toFloat()), center.y + triRadius * sin((210.0 * PI / 180.0).toFloat()))
    val p3 = Offset(center.x + triRadius * cos((330.0 * PI / 180.0).toFloat()), center.y + triRadius * sin((330.0 * PI / 180.0).toFloat()))

    val path = Path().apply {
        moveTo(p1.x, p1.y); lineTo(p2.x, p2.y); lineTo(p3.x, p3.y); close()
    }
    drawPath(path, color = glowColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
    drawPath(path, color = color, style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round))
}

/** Black Panther: Vibranium kinetic chevron teeth ring + tribal diamond core */
private fun DrawScope.drawVibraniumWeaveGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val toothCount = 16
    val rOuter = radius * 0.65f
    val rInner = radius * 0.52f

    val toothPath = Path()
    for (i in 0 until toothCount) {
        val angleDeg = i * (360f / toothCount)
        val aRad = (angleDeg * PI / 180f).toFloat()
        val aMidRad = ((angleDeg + (180f / toothCount)) * PI / 180f).toFloat()

        val xTip = center.x + rOuter * cos(aRad)
        val yTip = center.y + rOuter * sin(aRad)
        val xBase = center.x + rInner * cos(aMidRad)
        val yBase = center.y + rInner * sin(aMidRad)

        if (i == 0) toothPath.moveTo(xTip, yTip) else toothPath.lineTo(xTip, yTip)
        toothPath.lineTo(xBase, yBase)
    }
    toothPath.close()

    drawPath(toothPath, color = primary.copy(alpha = 0.8f), style = Stroke(width = 1.8.dp.toPx()))

    // Diamond reticle
    val dR = radius * 0.28f + (burst * 3f)
    val dPath = Path().apply {
        moveTo(center.x, center.y - dR)
        lineTo(center.x + dR, center.y)
        lineTo(center.x, center.y + dR)
        lineTo(center.x - dR, center.y)
        close()
    }
    drawPath(dPath, color = secondary, style = Stroke(width = 2.dp.toPx()))
}

/** Captain America: Vibranium concentric shield rings + 5-point star */
private fun DrawScope.drawStarShieldGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    // Shield outer bands
    drawCircle(color = secondary, radius = radius * 0.66f, center = center, style = Stroke(width = 3.dp.toPx()))
    drawCircle(color = primary.copy(alpha = 0.6f), radius = radius * 0.54f, center = center, style = Stroke(width = 2.5.dp.toPx()))
    drawCircle(color = secondary, radius = radius * 0.42f, center = center, style = Stroke(width = 2.dp.toPx()))

    // 5-point star
    val starRadius = radius * 0.28f + (burst * 3f)
    val innerRadius = starRadius * 0.42f
    val starPath = Path()

    for (i in 0 until 10) {
        val r = if (i % 2 == 0) starRadius else innerRadius
        val angleRad = ((i * 36f - 90f) * PI / 180f).toFloat()
        val x = center.x + r * cos(angleRad)
        val y = center.y + r * sin(angleRad)
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()

    drawPath(starPath, color = primary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

/** Thor: Asgardian runic compass with lightning crackles & triquetra */
private fun DrawScope.drawMjolnirRunicGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val dialRadius = radius * 0.64f
    drawCircle(color = primary.copy(alpha = 0.8f), radius = dialRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))

    // 8 Runic cardinal tick pairs
    for (i in 0 until 8) {
        val angleRad = (i * 45f * PI / 180f).toFloat()
        val x1 = center.x + (dialRadius - 10f) * cos(angleRad)
        val y1 = center.y + (dialRadius - 10f) * sin(angleRad)
        val x2 = center.x + (dialRadius + 6f) * cos(angleRad)
        val y2 = center.y + (dialRadius + 6f) * sin(angleRad)
        drawLine(color = secondary, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.dp.toPx())
    }

    // Triquetra knot symbol in center
    val triRadius = radius * 0.22f + (burst * 3f)
    for (i in 0 until 3) {
        val aRad = (i * 120f * PI / 180f).toFloat()
        val cx = center.x + (triRadius * 0.6f) * cos(aRad)
        val cy = center.y + (triRadius * 0.6f) * sin(aRad)
        drawCircle(color = primary.copy(alpha = 0.65f), radius = triRadius, center = Offset(cx, cy), style = Stroke(width = 1.4.dp.toPx()))
    }
}

/** Doctor Strange: Mystic Tao mandalas & Eye of Agamotto */
private fun DrawScope.drawEyeOfAgamottoGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val mandalaRadius = radius * 0.62f
    // Tao spell ring square rotation
    drawCircle(
        color = primary.copy(alpha = 0.65f),
        radius = mandalaRadius,
        center = center,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
    )

    // Overlapping mystic squares (mandala)
    val sqSize = mandalaRadius * 1.3f
    val rectOffset = Offset(center.x - sqSize / 2f, center.y - sqSize / 2f)
    rotate(15f, center) {
        drawRect(color = primary.copy(alpha = 0.4f), topLeft = rectOffset, size = Size(sqSize, sqSize), style = Stroke(width = 1.dp.toPx()))
    }
    rotate(60f, center) {
        drawRect(color = primary.copy(alpha = 0.4f), topLeft = rectOffset, size = Size(sqSize, sqSize), style = Stroke(width = 1.dp.toPx()))
    }

    // Eye of Agamotto central iris (almond shape)
    val eyeWidth = radius * 0.32f + (burst * 4f)
    val eyeHeight = radius * 0.18f
    val eyePath = Path().apply {
        moveTo(center.x - eyeWidth, center.y)
        quadraticBezierTo(center.x, center.y - eyeHeight, center.x + eyeWidth, center.y)
        quadraticBezierTo(center.x, center.y + eyeHeight, center.x - eyeWidth, center.y)
        close()
    }
    drawPath(eyePath, color = secondary, style = Stroke(width = 2.dp.toPx()))
    drawCircle(color = secondary, radius = radius * 0.08f, center = center)
}

/** Spider-Man: Nano-spider targeting reticle & 8-axis web matrix */
private fun DrawScope.drawSpiderMatrixGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val webRadius = radius * 0.65f

    // 8 Web Spokes
    for (i in 0 until 8) {
        val angleRad = (i * 45f * PI / 180f).toFloat()
        val xEnd = center.x + webRadius * cos(angleRad)
        val yEnd = center.y + webRadius * sin(angleRad)
        drawLine(color = primary.copy(alpha = 0.4f), start = center, end = Offset(xEnd, yEnd), strokeWidth = 1.dp.toPx())
    }

    // Web concentric octagons
    val rings = listOf(0.35f, 0.50f, 0.65f)
    for (rFactor in rings) {
        val r = webRadius * rFactor
        val octPath = Path()
        for (i in 0 until 8) {
            val aRad = (i * 45f * PI / 180f).toFloat()
            val x = center.x + r * cos(aRad)
            val y = center.y + r * sin(aRad)
            if (i == 0) octPath.moveTo(x, y) else octPath.lineTo(x, y)
        }
        octPath.close()
        drawPath(octPath, color = secondary.copy(alpha = 0.6f), style = Stroke(width = 1.2.dp.toPx()))
    }

    // Central crosshair targeting bracket
    val crossLen = radius * 0.22f + (burst * 4f)
    drawLine(color = primary, start = Offset(center.x - crossLen, center.y), end = Offset(center.x - crossLen + 12f, center.y), strokeWidth = 2.dp.toPx())
    drawLine(color = primary, start = Offset(center.x + crossLen, center.y), end = Offset(center.x + crossLen - 12f, center.y), strokeWidth = 2.dp.toPx())
    drawLine(color = primary, start = Offset(center.x, center.y - crossLen), end = Offset(center.x, center.y - crossLen + 12f), strokeWidth = 2.dp.toPx())
    drawLine(color = primary, start = Offset(center.x, center.y + crossLen), end = Offset(center.x, center.y + crossLen - 12f), strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawGammaFistGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val coreR = radius * 0.58f
    // Tri-blade gamma radiation trefoil
    for (i in 0 until 3) {
        val angleDeg = i * 120f - 90f
        val sweepAngle = 55f
        drawArc(
            color = primary.copy(alpha = 0.65f),
            startAngle = angleDeg - sweepAngle / 2f,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - coreR, center.y - coreR),
            size = Size(coreR * 2, coreR * 2),
            style = Stroke(width = 3.5.dp.toPx() + (burst * 4f))
        )
    }

    // Concentric kinetic shockwave rings
    drawCircle(
        color = secondary.copy(alpha = 0.5f),
        radius = radius * 0.44f,
        center = center,
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawCircle(
        color = primary.copy(alpha = 0.7f),
        radius = radius * 0.30f,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    // 4 Heavy Gamma Compression Nodes (North, South, East, West)
    val nodeDist = radius * 0.50f
    listOf(0f, 90f, 180f, 270f).forEach { deg ->
        val rad = (deg * PI / 180f).toFloat()
        val nodeX = center.x + nodeDist * cos(rad)
        val nodeY = center.y + nodeDist * sin(rad)
        drawCircle(color = primary, radius = 3.5.dp.toPx(), center = Offset(nodeX, nodeY))
        drawCircle(color = secondary, radius = 1.8.dp.toPx(), center = Offset(nodeX, nodeY))
    }
}

private fun DrawScope.drawOuterTicks(center: Offset, radius: Float, accentColor: Color) {
    val totalTicks = 60
    for (i in 0 until totalTicks) {
        val angleRad = (i * (360f / totalTicks) * PI / 180f).toFloat()
        val isMajor = i % 5 == 0
        val isAccent = i == 15 || i == 45 // 90° and 270°

        val tickLength = if (isMajor) radius * 0.07f else radius * 0.03f
        val strokeWidth = if (isMajor) 1.8.dp.toPx() else 1.dp.toPx()
        val tickColor = when {
            isAccent -> accentColor
            isMajor -> StarkCyanBright
            else -> StarkTickColor.copy(alpha = 0.25f)
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
}

/** Black Widow: Tactical dual crimson hourglass reticle & precision crosshairs */
private fun DrawScope.drawBlackWidowHourglassGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val hgRadius = radius * 0.58f

    // Dual outer targeting brackets
    drawArc(
        color = primary.copy(alpha = 0.5f),
        startAngle = 135f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - hgRadius, center.y - hgRadius),
        size = Size(hgRadius * 2, hgRadius * 2),
        style = Stroke(width = 1.5.dp.toPx())
    )
    drawArc(
        color = primary.copy(alpha = 0.5f),
        startAngle = 315f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(center.x - hgRadius, center.y - hgRadius),
        size = Size(hgRadius * 2, hgRadius * 2),
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Hourglass shape (symmetric triangles meeting at center)
    val hgHalfW = radius * 0.24f + (burst * 4f)
    val hgHalfH = radius * 0.36f + (burst * 4f)
    val waistGap = 2.dp.toPx()

    // Top triangle (pointing down)
    val topPath = Path().apply {
        moveTo(center.x - hgHalfW, center.y - hgHalfH)
        lineTo(center.x + hgHalfW, center.y - hgHalfH)
        lineTo(center.x, center.y - waistGap)
        close()
    }
    drawPath(topPath, color = primary, style = Stroke(width = 2.dp.toPx()))
    drawPath(topPath, color = primary.copy(alpha = 0.25f))

    // Bottom triangle (pointing up)
    val bottomPath = Path().apply {
        moveTo(center.x - hgHalfW, center.y + hgHalfH)
        lineTo(center.x + hgHalfW, center.y + hgHalfH)
        lineTo(center.x, center.y + waistGap)
        close()
    }
    drawPath(bottomPath, color = primary, style = Stroke(width = 2.dp.toPx()))
    drawPath(bottomPath, color = primary.copy(alpha = 0.25f))

    // Tactical crosshairs
    val tickLen = radius * 0.12f
    drawLine(secondary, Offset(center.x - hgRadius - tickLen, center.y), Offset(center.x - hgRadius + 4f, center.y), 1.5.dp.toPx())
    drawLine(secondary, Offset(center.x + hgRadius - 4f, center.y), Offset(center.x + hgRadius + tickLen, center.y), 1.5.dp.toPx())
}

/** Avengers Unified: Stylized Avengers chevron emblem with orbital hero rings */
private fun DrawScope.drawAvengersChevronGeometry(
    center: Offset,
    radius: Float,
    primary: Color,
    secondary: Color,
    burst: Float
) {
    val aRadius = radius * 0.62f

    // Precision outer initiative circle with broken gap at arrow pass-through
    drawArc(
        color = primary,
        startAngle = 20f,
        sweepAngle = 310f,
        useCenter = false,
        topLeft = Offset(center.x - aRadius, center.y - aRadius),
        size = Size(aRadius * 2, aRadius * 2),
        style = Stroke(width = 2.5.dp.toPx() + (burst * 2f))
    )

    // Inner orbital accent ring
    drawCircle(
        color = secondary.copy(alpha = 0.5f),
        radius = aRadius * 0.82f,
        center = center,
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f))
    )

    // Stylized slanted "A" chevron
    val scale = (radius * 0.44f + (burst * 4f))
    val aPath = Path().apply {
        // Left slanted leg
        moveTo(center.x - scale * 0.52f, center.y + scale * 0.85f)
        lineTo(center.x + scale * 0.10f, center.y - scale * 0.95f)
        lineTo(center.x + scale * 0.35f, center.y - scale * 0.95f)
        lineTo(center.x - scale * 0.18f, center.y + scale * 0.85f)
        close()
    }
    drawPath(aPath, color = primary)

    // Right leg
    val rPath = Path().apply {
        moveTo(center.x + scale * 0.18f, center.y - scale * 0.60f)
        lineTo(center.x + scale * 0.65f, center.y + scale * 0.85f)
        lineTo(center.x + scale * 0.38f, center.y + scale * 0.85f)
        lineTo(center.x + scale * 0.05f, center.y - scale * 0.20f)
        close()
    }
    drawPath(rPath, color = primary)

    // Horizontal arrow crossbar pointing right
    val arrowPath = Path().apply {
        moveTo(center.x - scale * 0.65f, center.y + scale * 0.12f)
        lineTo(center.x + scale * 0.85f, center.y + scale * 0.12f)
        lineTo(center.x + scale * 0.62f, center.y - scale * 0.08f)
        moveTo(center.x + scale * 0.85f, center.y + scale * 0.12f)
        lineTo(center.x + scale * 0.62f, center.y + scale * 0.32f)
    }
    drawPath(arrowPath, color = secondary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

