package com.motorola.launcher.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkCyanGlow
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.TextPrimary

/**
 * Futuristic App Icon component featuring procedural HUD corner brackets,
 * cybernetic framing, responsive micro-spring tactile bounce, and high-legibility labels.
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile micro-spring press physics
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "futuristicIconScale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Futuristic Icon Frame Container
        Box(
            modifier = Modifier
                .size(iconSize + 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isPressed) Color(0x3300F0FF) else Color(0x1A080F1D)
                )
                .border(
                    width = if (isPressed) 1.5.dp else 1.dp,
                    color = if (isPressed) StarkCyanBright else StarkHudBorder,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Cybernetic Corner Reticle Accents
            Canvas(modifier = Modifier.matchParentSize()) {
                val bracketSize = 6.dp.toPx()
                val bracketColor = if (isPressed) StarkCyanBright else StarkHudBorderBright.copy(alpha = 0.6f)
                val stroke = 1.5.dp.toPx()

                // Top-Left corner
                drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx() + bracketSize, 2.dp.toPx()), stroke)
                drawLine(bracketColor, Offset(2.dp.toPx(), 2.dp.toPx()), Offset(2.dp.toPx(), 2.dp.toPx() + bracketSize), stroke)

                // Bottom-Right corner
                drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx() - bracketSize, size.height - 2.dp.toPx()), stroke)
                drawLine(bracketColor, Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()), Offset(size.width - 2.dp.toPx(), size.height - 2.dp.toPx() - bracketSize), stroke)
            }

            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                // Procedural Holographic Monogram Fallback
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x3300F0FF),
                                    Color(0x11040812)
                                )
                            )
                        )
                        .border(1.dp, StarkHudBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = StarkCyan,
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
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
