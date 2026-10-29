package com.motorola.launcher.ui.workspace

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.FuturisticAppIcon
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.collections.immutable.ImmutableList

/** Soft drop shadow so HUD text stays legible on any wallpaper (including bright photos). */
internal val HudTextStyle = TextStyle(
    shadow = Shadow(color = Color(0xE6000000), offset = Offset(0f, 2f), blurRadius = 8f)
)

/** Dark glass background used behind HUD chips/pills. */
internal val HudGlass = Color(0x9903070E)

/**
 * Main Home Workspace: "STARK OS — JARVIS Edition".
 *
 * Layout is intentionally minimal: clock on top, Arc Reactor in the centre,
 * two telemetry chips + two action pills below it, and the dock at the bottom.
 * Nothing floats beside the reactor, so the wallpaper and the core stay clear.
 */
@Composable
fun WorkspaceScreen(
    dockApps: ImmutableList<AppItem>,
    telemetry: SystemTelemetryState,
    progressProvider: () -> Float,
    onAppClick: (AppItem) -> Unit,
    onAppLongClick: (AppItem) -> Unit,
    onOpenDrawerClick: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenQuickControls: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val customization = LocalCustomization.current
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current

    val tap: (() -> Unit) -> () -> Unit = { action ->
        {
            if (customization.hapticFeedback) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            action()
        }
    }

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
        // Curved-edge ambient glow (left / right) — dynamically themed
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight(0.5f)
                .width(2.dp)
                .background(Brush.horizontalGradient(listOf(marvelTheme.colors.glow, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(0.5f)
                .width(2.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, marvelTheme.colors.glow)))
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP: Clock
            DigitalClock(
                telemetry = telemetry,
                modifier = Modifier.padding(top = 8.dp)
            )

            // CENTRE: All-In-One Marvel Command Core (Battery + RAM + Controls + Settings integrated)
            ReactorCore(
                telemetry = telemetry,
                onCoreClick = onOpenDiagnostics,
                onOpenQuickControls = onOpenQuickControls,
                onOpenCustomization = onOpenCustomization,
                size = 230.dp
            )

            // BOTTOM: Drawer trigger + dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HudPill(text = marvelTheme.dockStyle.pillText, onClick = tap(onOpenDrawerClick))

                Spacer(modifier = Modifier.height(10.dp))

                val dockShape = chamferedShape(cutSize = marvelTheme.dockStyle.cutCornerSize)
                val displayedDock = dockApps.take(6)
                val dockIconSize = if (displayedDock.size > 4) 40.dp else 46.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .clip(dockShape)
                        .background(marvelTheme.dockStyle.glassColor)
                        .border(1.dp, marvelTheme.colors.hudBorder, dockShape)
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (item in displayedDock) {
                            FuturisticAppIcon(
                                app = item,
                                onClick = tap { onAppClick(item) },
                                onLongClick = {
                                    if (customization.hapticFeedback) {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    }
                                    onAppLongClick(item)
                                },
                                iconSize = dockIconSize,
                                showLabel = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudPill(
    text: String,
    onClick: () -> Unit
) {
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .widthIn(min = 112.dp)
            .clip(shape)
            .background(HudGlass)
            .border(1.dp, marvelTheme.colors.hudBorderBright, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = marvelTheme.colors.bright,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            style = HudTextStyle
        )
    }
}
