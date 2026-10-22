package com.motorola.launcher.ui.dialog

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.HudPanel
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkGold
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.StarkIceBlue
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import java.util.Locale

/**
 * Holographic JARVIS System Dashboard Modal.
 * Exposes real hardware telemetry and diagnostic links in a futuristic command console.
 */
@Composable
fun JarvisDashboardSheet(
    telemetry: SystemTelemetryState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        val shape = chamferedShape(cutSize = 24f)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(StarkDarkSurface)
                .border(1.5.dp, StarkHudBorderBright, shape)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Tactical Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (telemetry.isCharging) StarkEmerald else StarkCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "JARVIS TACTICAL // DIAGNOSTICS",
                            color = StarkCyan,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp
                        )
                        Text(
                            text = "HOST: MOTOROLA EDGE 50 FUSION // ANDROID 16",
                            color = TextDimHud,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.0.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1: Power Core Telemetry
                HudPanel(
                    title = "POWER CORE STATUS",
                    statusIndicator = if (telemetry.isCharging) "CHARGING" else "OPTIMAL",
                    statusColor = if (telemetry.isCharging) StarkEmerald else StarkCyan
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryMetricItem("CHARGE", "${telemetry.batteryLevel}%", StarkCyan)
                        TelemetryMetricItem("TEMP", String.format(Locale.US, "%.1f°C", telemetry.batteryTempCelsius), if (telemetry.batteryTempCelsius > 40f) StarkReactorRed else StarkIceBlue)
                        TelemetryMetricItem("VOLTAGE", "${telemetry.batteryVoltageMv} mV", TextPrimary)
                        TelemetryMetricItem("HEALTH", telemetry.batteryHealth.uppercase(Locale.US), StarkEmerald)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.batteryLevel / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (telemetry.isCharging) StarkEmerald else StarkCyan,
                        trackColor = Color(0x3300F0FF),
                        strokeCap = StrokeCap.Round,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Memory Banks (RAM)
                HudPanel(
                    title = "NEURAL MEMORY ALLOCATION",
                    statusIndicator = "${telemetry.ramPercent}% UTILIZED",
                    statusColor = if (telemetry.ramPercent > 85) StarkReactorRed else StarkCyan
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryMetricItem("RAM IN USE", telemetry.ramUsedFormatted, StarkCyan)
                        TelemetryMetricItem("TOTAL CAPACITY", telemetry.ramTotalFormatted, TextSecondary)
                        TelemetryMetricItem("SWAP / ZRAM", "ACTIVE", StarkEmerald)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.ramPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (telemetry.ramPercent > 85) StarkReactorRed else StarkIceBlue,
                        trackColor = Color(0x3300F0FF),
                        strokeCap = StrokeCap.Round,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Storage Vault
                HudPanel(
                    title = "LOCAL STORAGE VAULT",
                    statusIndicator = "${telemetry.storagePercentUsed}% FILLED",
                    statusColor = StarkGold
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryMetricItem("AVAILABLE", telemetry.storageFreeFormatted, StarkEmerald)
                        TelemetryMetricItem("CAPACITY", telemetry.storageTotalFormatted, TextSecondary)
                        TelemetryMetricItem("FTS5 INDEX", "ONLINE", StarkCyan)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { telemetry.storagePercentUsed / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = StarkGold,
                        trackColor = Color(0x33FFB800),
                        strokeCap = StrokeCap.Round,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 4: System Bus & Environment
                HudPanel(
                    title = "HARDWARE ENGINE",
                    statusIndicator = "144Hz CADENCE",
                    statusColor = StarkCyan
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryMetricItem("NETWORK", telemetry.networkType.uppercase(Locale.US), StarkCyan)
                        TelemetryMetricItem("REFRESH", "${telemetry.refreshRateHz} Hz", StarkCyanBright)
                        TelemetryMetricItem("UPTIME", telemetry.uptimeFormatted, TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tactical Control Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TacticalActionButton(
                        text = "POWER SETTINGS",
                        color = StarkGold,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            launchIntent(context, Settings.ACTION_BATTERY_SAVER_SETTINGS)
                        }
                    )
                    TacticalActionButton(
                        text = "STORAGE",
                        color = StarkIceBlue,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            launchIntent(context, Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TacticalActionButton(
                    text = "DISMISS CONSOLE",
                    color = StarkCyan,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun TelemetryMetricItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            color = TextDimHud,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.0.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun TacticalActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

private fun launchIntent(context: Context, action: String) {
    try {
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    } catch (_: Throwable) {
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
        } catch (_: Throwable) {
        }
    }
}
