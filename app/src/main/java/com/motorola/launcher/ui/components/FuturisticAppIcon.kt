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
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.model.IconStylePreset
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.TextPrimary

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
        // Futuristic Icon Frame Container
        val borderColor = when {
            isPressed -> themeColor.bright
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
                                if (isPressed) themeColor.subtle else Color(0x1F060C16)
                            )
                            .border(
                                width = if (isPressed || iconStyle.hasGlow) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = frameShape
                            )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            // Draw Cybernetic Corner Brackets in TECH icon style
            if (iconStyle == IconStylePreset.TECH) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val bracketSize = 5.dp.toPx()
                    val bracketColor = if (isPressed) themeColor.bright else themeColor.primary.copy(alpha = 0.7f)
                    val stroke = 1.2.dp.toPx()

                    // Top-Left corner
                    drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx() + bracketSize, 2.dp.toPx()), stroke)
                    drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx(), 2.dp.toPx() + bracketSize), stroke)

                    // Bottom-Right corner
                    drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx() - bracketSize, size.height - 2.dp.toPx()), stroke)
                    drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx() - bracketSize), stroke)
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
