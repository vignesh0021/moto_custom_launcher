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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.FuturisticAppIcon
import com.motorola.launcher.ui.components.QuickControlsPanel
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkElectricBlue
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.collections.immutable.ImmutableList

/**
 * Main Home Workspace Composable: "STARK OS — JARVIS Edition".
 * Features Central Arc Reactor with live battery mapping, holographic digital clock,
 * system telemetry readouts, quick settings control bar, and tactical application dock.
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
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

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
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            StarkCyan.copy(alpha = 0.5f),
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
                .width(3.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            StarkElectricBlue.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // Primary Vertical Command Center Layout
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Digital Holographic Clock & Protocol Header
            DigitalClock(
                telemetry = telemetry,
                modifier = Modifier.padding(top = 8.dp)
            )

            // CENTER SECTION: Interactive Procedural Arc Reactor Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ReactorCore(
                    telemetry = telemetry,
                    onCoreClick = onOpenDiagnostics,
                    size = 230.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tactical System Metrics Status Strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x1F050B16))
                        .border(1.dp, StarkHudBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "MEM: ${telemetry.ramUsedFormatted}",
                        color = StarkCyanBright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "•", color = TextDimHud, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STORAGE: ${telemetry.storageFreeFormatted} FREE",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // BOTTOM SECTION: Quick Controls, Swipe Pill & Tactical Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick System Settings Bar
                QuickControlsPanel(
                    onOpenDiagnostics = onOpenDiagnostics,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Holographic Drawer Invocation Banner
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F00F0FF))
                        .border(1.dp, StarkHudBorder, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onOpenDrawerClick()
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "▲",
                        color = StarkCyan,
                        fontSize = 9.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALL APPLICATIONS",
                        color = StarkCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Floating Tactical Dock Container
                val dockShape = chamferedShape(cutSize = 20f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(dockShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x33060C18),
                                    Color(0x5903060C)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = StarkHudBorderBright,
                            shape = dockShape
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (item in dockApps.take(5)) {
                            FuturisticAppIcon(
                                app = item,
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onAppClick(item)
                                },
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    onAppLongClick(item)
                                },
                                iconSize = 46.dp,
                                showLabel = false
                            )
                        }
                    }
                }
            }
        }
    }
}
