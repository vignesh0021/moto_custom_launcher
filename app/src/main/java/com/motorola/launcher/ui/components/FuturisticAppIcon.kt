package com.motorola.launcher.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.model.IconStylePreset
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.TextPrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Futuristic App Icon component supporting customizable icon styles (Tech, Glow, Minimal, Rounded),
 * dynamic theme accent colors, cybernetic corner brackets, and tactile press physics.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FuturisticAppIcon(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 50.dp,
    showLabel: Boolean = true
) {
    val view = LocalView.current
    val customization = LocalCustomization.current
    val themeColor = customization.themeColor
    val iconStyle = customization.iconStyle

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "futuristicIconScale"
    )

    val frameShape = RoundedCornerShape(iconStyle.cornerRadius)
    val innerIconShape = RoundedCornerShape((iconStyle.cornerRadius - 2.dp).coerceAtLeast(4.dp))

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (customization.hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onClick()
                },
                onLongClick = {
                    if (customization.hapticFeedback) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    onLongClick()
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val hero = customization.heroProtocol
        // Futuristic Icon Frame Container
        val borderColor = when {
            isPressed -> hero.bright
            iconStyle == IconStylePreset.HERO_THEMED -> hero.primary.copy(alpha = 0.85f)
            iconStyle.hasGlow -> themeColor.primary
            else -> themeColor.primary.copy(alpha = iconStyle.borderAlpha)
        }

        val isClean = iconStyle == IconStylePreset.CLEAN

        Box(
            modifier = Modifier
                .size(iconSize + if (isClean) 0.dp else 10.dp)
                .clip(frameShape)
                .then(
                    if (!isClean) {
                        Modifier
                            .background(
                                if (isPressed) hero.subtle else Color(0x2E060C16)
                            )
                            .border(
                                width = if (isPressed || iconStyle == IconStylePreset.HERO_THEMED || iconStyle.hasGlow) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = frameShape
                            )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Draw Marvel Hero-Specific Insignia Embellishments or TECH brackets
            if (iconStyle == IconStylePreset.HERO_THEMED || iconStyle == IconStylePreset.TECH) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val stroke = 1.2.dp.toPx()
                    if (iconStyle == IconStylePreset.HERO_THEMED) {
                        when (hero) {
                            MarvelHeroProtocol.IRON_MAN -> {
                                val nodeColor = if (isPressed) hero.bright else hero.primary
                                drawCircle(nodeColor, radius = 2.5.dp.toPx(), center = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()))
                                drawCircle(Color.White, radius = 1.0.dp.toPx(), center = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()))
                                drawCircle(nodeColor, radius = 2.5.dp.toPx(), center = Offset(4.dp.toPx(), size.height - 4.dp.toPx()))
                                drawCircle(Color.White, radius = 1.0.dp.toPx(), center = Offset(4.dp.toPx(), size.height - 4.dp.toPx()))
                                drawLine(hero.secondary.copy(alpha = 0.7f), Offset(1.dp.toPx(), 7.dp.toPx()), Offset(7.dp.toPx(), 1.dp.toPx()), strokeWidth = 1.5f)
                                drawLine(hero.secondary.copy(alpha = 0.7f), Offset(size.width - 7.dp.toPx(), size.height - 1.dp.toPx()), Offset(size.width - 1.dp.toPx(), size.height - 7.dp.toPx()), strokeWidth = 1.5f)
                            }
                            MarvelHeroProtocol.BLACK_PANTHER -> {
                                val midX = size.width / 2f
                                val toothPath = Path()
                                toothPath.moveTo(midX - 4.dp.toPx(), 1.dp.toPx())
                                toothPath.lineTo(midX, 5.dp.toPx())
                                toothPath.lineTo(midX + 4.dp.toPx(), 1.dp.toPx())
                                drawPath(toothPath, color = if (isPressed) hero.bright else hero.primary, style = Stroke(stroke))
                                val bottomPath = Path()
                                bottomPath.moveTo(midX - 4.dp.toPx(), size.height - 1.dp.toPx())
                                bottomPath.lineTo(midX, size.height - 5.dp.toPx())
                                bottomPath.lineTo(midX + 4.dp.toPx(), size.height - 1.dp.toPx())
                                drawPath(bottomPath, color = if (isPressed) hero.bright else hero.primary, style = Stroke(stroke))
                            }
                            MarvelHeroProtocol.CAPTAIN_AMERICA -> {
                                val starC = Offset(size.width - 5.dp.toPx(), 5.dp.toPx())
                                drawCircle(hero.secondary, radius = 3.dp.toPx(), center = starC)
                                drawCircle(Color.White, radius = 1.2.dp.toPx(), center = starC)
                                drawArc(hero.primary, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()), size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx()), style = Stroke(stroke))
                                drawArc(hero.secondary, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(size.width - 11.5.dp.toPx(), size.height - 11.5.dp.toPx()), size = androidx.compose.ui.geometry.Size(10.dp.toPx(), 10.dp.toPx()), style = Stroke(stroke))
                            }
                            MarvelHeroProtocol.THOR -> {
                                val boltColor = if (isPressed) hero.bright else hero.primary
                                val boltPath = Path()
                                boltPath.moveTo(size.width - 7.dp.toPx(), 2.dp.toPx())
                                boltPath.lineTo(size.width - 3.5.dp.toPx(), 5.dp.toPx())
                                boltPath.lineTo(size.width - 5.dp.toPx(), 5.dp.toPx())
                                boltPath.lineTo(size.width - 2.dp.toPx(), 8.5.dp.toPx())
                                drawPath(boltPath, color = boltColor, style = Stroke(1.5f))
                                drawLine(hero.secondary, Offset(2.dp.toPx(), size.height - 5.dp.toPx()), Offset(5.dp.toPx(), size.height - 2.dp.toPx()), strokeWidth = 1.8f)
                            }
                            MarvelHeroProtocol.DOCTOR_STRANGE -> {
                                val mysticColor = if (isPressed) hero.bright else hero.primary
                                drawArc(mysticColor, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(2.dp.toPx(), 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(9.dp.toPx(), 9.dp.toPx()), style = Stroke(stroke))
                                drawArc(mysticColor, startAngle = 0f, sweepAngle = 90f, useCenter = false, topLeft = Offset(size.width - 11.dp.toPx(), size.height - 11.dp.toPx()), size = androidx.compose.ui.geometry.Size(9.dp.toPx(), 9.dp.toPx()), style = Stroke(stroke))
                                drawCircle(hero.secondary, radius = 2.dp.toPx(), center = Offset(size.width - 4.5.dp.toPx(), 4.5.dp.toPx()))
                            }
                            MarvelHeroProtocol.HULK -> {
                                val hulkColor = if (isPressed) hero.bright else hero.primary
                                val cornerLen = 6.dp.toPx()
                                drawLine(hulkColor, Offset(1.5.dp.toPx(), 1.5.dp.toPx()), Offset(1.5.dp.toPx() + cornerLen, 1.5.dp.toPx()), strokeWidth = 2.dp.toPx())
                                drawLine(hulkColor, Offset(1.5.dp.toPx(), 1.5.dp.toPx()), Offset(1.5.dp.toPx(), 1.5.dp.toPx() + cornerLen), strokeWidth = 2.dp.toPx())
                                drawCircle(hero.secondary, radius = 2.2.dp.toPx(), center = Offset(size.width - 4.5.dp.toPx(), 4.5.dp.toPx()))
                                drawLine(hulkColor, Offset(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()), Offset(size.width - 1.5.dp.toPx() - cornerLen, size.height - 1.5.dp.toPx()), strokeWidth = 2.dp.toPx())
                                drawLine(hulkColor, Offset(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx()), Offset(size.width - 1.5.dp.toPx(), size.height - 1.5.dp.toPx() - cornerLen), strokeWidth = 2.dp.toPx())
                            }
                            MarvelHeroProtocol.SPIDER_MAN -> {
                                val spiderColor = if (isPressed) hero.bright else hero.primary
                                val rSize = 4.5.dp.toPx()
                                drawLine(spiderColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx() + rSize, 2.dp.toPx()), stroke)
                                drawLine(spiderColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx(), 2.dp.toPx() + rSize), stroke)
                                drawCircle(hero.secondary, radius = 2.dp.toPx(), center = Offset(size.width - 4.5.dp.toPx(), 4.5.dp.toPx()))
                                drawLine(spiderColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx() - rSize, size.height - 2.dp.toPx()), stroke)
                                drawLine(spiderColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx() - rSize), stroke)
                                drawCircle(hero.secondary, radius = 2.dp.toPx(), center = Offset(4.5.dp.toPx(), size.height - 4.5.dp.toPx()))
                            }
                            MarvelHeroProtocol.BLACK_WIDOW -> {
                                val widowColor = if (isPressed) hero.bright else hero.primary
                                drawLine(widowColor, Offset(size.width / 2f - 4.dp.toPx(), 2.dp.toPx()), Offset(size.width / 2f + 4.dp.toPx(), 2.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                                drawCircle(hero.secondary, radius = 2.dp.toPx(), center = Offset(size.width - 4.dp.toPx(), 4.dp.toPx()))
                            }
                            MarvelHeroProtocol.AVENGERS_UNIFIED -> {
                                val avengerColor = if (isPressed) hero.bright else hero.primary
                                val arrowW = 5.dp.toPx()
                                drawLine(avengerColor, Offset(size.width - 2.dp.toPx() - arrowW, 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), 2.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                                drawLine(avengerColor, Offset(size.width - 2.dp.toPx(), 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), 2.dp.toPx() + arrowW), strokeWidth = 1.5.dp.toPx())
                                drawCircle(hero.secondary, radius = 2.dp.toPx(), center = Offset(4.dp.toPx(), size.height - 4.dp.toPx()))
                            }
                        }
                    } else if (iconStyle == IconStylePreset.TECH) {
                        val bracketSize = 5.dp.toPx()
                        val bracketColor = if (isPressed) themeColor.bright else themeColor.primary.copy(alpha = 0.7f)
                        drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx() + bracketSize, 2.dp.toPx()), stroke)
                        drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx(), 2.dp.toPx() + bracketSize), stroke)
                        drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx() - bracketSize, size.height - 2.dp.toPx()), stroke)
                        drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx() - bracketSize), stroke)
                    }
                }
            }

            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(innerIconShape)
                )
            } else {
                // Procedural Holographic Monogram Fallback
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(innerIconShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    themeColor.subtle,
                                    Color(0x11040812)
                                )
                            )
                        )
                        .border(1.dp, StarkHudBorder, innerIconShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = themeColor.primary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = app.label,
                color = TextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = Color(0xCC000000),
                        offset = Offset(0f, 2f),
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}
