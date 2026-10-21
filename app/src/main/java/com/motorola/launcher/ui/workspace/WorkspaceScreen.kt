package com.motorola.launcher.ui.workspace

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoCyanGlow
import com.motorola.launcher.ui.theme.MotoDarkSurface
import com.motorola.launcher.ui.theme.MotoElectricViolet
import com.motorola.launcher.ui.theme.MotoGlassBorder
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.theme.TextTertiary
import kotlinx.collections.immutable.ImmutableList

/**
 * Main Home Workspace composable.
 * Features ambient curved waterfall edge highlights, floating 3D glass dock,
 * and tactile micro-spring icon animations.
 */
@Composable
fun WorkspaceScreen(
    dockApps: ImmutableList<AppItem>,
    progressProvider: () -> Float,
    onAppClick: (AppItem) -> Unit,
    onOpenDrawerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    // Pulsing breathing animation for swipe up indicator
    val infiniteTransition = rememberInfiniteTransition(label = "indicatorBreath")
    val indicatorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progressProvider()
                val scale = 1.0f - (0.08f * p)
                scaleX = scale
                scaleY = scale
                alpha = (1.0f - (p * 1.8f)).coerceIn(0.0f, 1.0f)
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Motorola 53-degree Curved Waterfall Edge Ambient Glow (Left Edge)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.7f)
                .width(4.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MotoCyanAccent.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Motorola Curved Waterfall Edge Ambient Glow (Right Edge)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.7f)
                .width(4.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MotoElectricViolet.copy(alpha = 0.45f)
                        )
                    )
                )
        )

        // Top Clock & Glanceable Information
        ClockWidget(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 20.dp)
        )

        // Bottom Workspace Dock Area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Luminous Swipe Up Pill & Micro-Chevron
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, MotoCyanAccent.copy(alpha = indicatorAlpha * 0.5f), RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenDrawerClick
                    )
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "▲",
                    color = MotoCyanAccent.copy(alpha = indicatorAlpha),
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "APPS",
                    color = TextSecondary.copy(alpha = indicatorAlpha),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Premium Floating 3D Frosted Glass Dock Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x331E293B),
                                Color(0x4D0F172A)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MotoCyanAccent.copy(alpha = 0.65f),
                                MotoElectricViolet.copy(alpha = 0.65f),
                                MotoCyanAccent.copy(alpha = 0.65f)
                            )
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (item in dockApps.take(5)) {
                        DockIconItem(
                            app = item,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onAppClick(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockIconItem(
    app: AppItem,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth physical spring bounce on touch
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockIconScale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (app.iconBitmap != null) {
            Image(
                bitmap = app.iconBitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        } else {
            // High-contrast cyber-glow avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x6600E5FF),
                                Color(0x227B2CBF)
                            )
                        )
                    )
                    .border(1.dp, MotoCyanAccent.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.label.take(1).uppercase(),
                    color = MotoCyanAccent,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
