package com.motorola.launcher.ui.dialog

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.theme.LocalCustomization
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
 * System Dashboard modal for STARK OS — JARVIS Edition (Screen 3 in design reference).
 * Renders holographic wireframe smartphone blueprint, circular battery gauge, storage bar,
 * memory progress, live CPU sparkline, network transport status, and device hardware specs.
 */
@Composable
fun JarvisDashboardSheet(
    telemetry: SystemTelemetryState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val customization = LocalCustomization.current
    val themeColor = customization.themeColor
    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        val sheetShape = RoundedCornerShape(20.dp)

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(StarkDarkSurface)
                .border(1.5.dp, StarkHudBorderBright, sheetShape)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header: SYSTEM DASHBOARD / REAL-TIME DEVICE INFORMATION
                Text(
                    text = "SYSTEM DASHBOARD",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp
                )
                Text(
                    text = "REAL-TIME DEVICE INFORMATION",
                    color = themeColor.primary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )

                // 1. Phone Wireframe Blueprint Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F00E5FF))
                        .border(1.dp, StarkHudBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Holographic Wireframe Phone Blueprint Vector
                        Canvas(modifier = Modifier.size(54.dp, 80.dp)) {
                            val w = size.width
                            val h = size.height

                            // Outer phone outline
                            drawRoundRect(
                                color = themeColor.primary,
                                size = Size(w, h),
                                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                                style = Stroke(1.5.dp.toPx())
                            )
                            // Screen bezel line
                            drawRoundRect(
                                color = themeColor.primary.copy(alpha = 0.4f),
                                topLeft = Offset(4.dp.toPx(), 6.dp.toPx()),
                                size = Size(w - 8.dp.toPx(), h - 12.dp.toPx()),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                                style = Stroke(1.dp.toPx())
                            )
                            // Camera punch hole
                            drawCircle(
                                color = themeColor.bright,
                                radius = 2.dp.toPx(),
                                center = Offset(w / 2f, 10.dp.toPx())
                            )
                            // Holographic central scan rings
                            drawCircle(
                                color = themeColor.primary.copy(alpha = 0.6f),
                                radius = 12.dp.toPx(),
                                center = Offset(w / 2f, h / 2f),
                                style = Stroke(1.dp.toPx())
                            )
                            drawCircle(
                                color = themeColor.bright.copy(alpha = 0.8f),
                                radius = 5.dp.toPx(),
                                center = Offset(w / 2f, h / 2f),
                                style = Stroke(1.dp.toPx())
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Host Identity Text
                        Column {
                            Text(
                                text = "MOTOROLA EDGE 50 FUSION",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.0.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "STARK OS // JARVIS EDITION",
                                color = themeColor.primary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                                color = TextDimHud,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Battery & Storage Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Battery Circular Progress Card
                    DashboardCard(
                        title = "BATTERY",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Circular Battery Gauge
                            Box(
                                modifier = Modifier.size(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val stroke = 4.dp.toPx()
                                    val r = (size.minDimension - stroke) / 2f
                                    val c = Offset(size.width / 2f, size.height / 2f)

                                    // Track
                                    drawCircle(
                                        color = StarkHudBorder.copy(alpha = 0.25f),
                                        radius = r,
                                        center = c,
                                        style = Stroke(stroke)
                                    )
                                    // Progress
                                    val pct = if (telemetry.batteryLevel >= 0) telemetry.batteryLevel / 100f else 0f
                                    drawArc(
                                        color = if (telemetry.isCharging) StarkEmerald else themeColor.primary,
                                        startAngle = -90f,
                                        sweepAngle = pct * 360f,
                                        useCenter = false,
                                        topLeft = Offset(c.x - r, c.y - r),
                                        size = Size(r * 2f, r * 2f),
                                        style = Stroke(stroke, cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = telemetry.batteryLevelFormatted,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (telemetry.isCharging) "Charging" else "Discharging",
                                    color = if (telemetry.isCharging) StarkEmerald else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (telemetry.isCharging) "Optimal flow" else "Normal status",
                                    color = TextDimHud,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Storage Progress Card
                    DashboardCard(
                        title = "STORAGE",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "💾 ${telemetry.storageUsedFormatted} / ${telemetry.storageTotalFormatted}",
                                    color = TextPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { telemetry.storagePercentUsed / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = themeColor.primary,
                                trackColor = Color(0x3300E5FF),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${telemetry.storagePercentUsed}% Used",
                                color = TextDimHud,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Memory (RAM) & CPU Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Memory (RAM) Card
                    DashboardCard(
                        title = "MEMORY (RAM)",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column {
                            Text(
                                text = "${telemetry.ramUsedFormatted} / ${telemetry.ramTotalFormatted}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { telemetry.ramPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = StarkCyanBright,
                                trackColor = Color(0x3300E5FF),
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${telemetry.ramPercent}% Used",
                                color = TextDimHud,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // CPU Sparkline Card
                    DashboardCard(
                        title = "CPU",
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Live Sparkline Graph Canvas
                            Canvas(modifier = Modifier.size(50.dp, 30.dp)) {
                                val w = size.width
                                val h = size.height
                                val points = listOf(
                                    Offset(0f, h * 0.7f),
                                    Offset(w * 0.2f, h * 0.5f),
                                    Offset(w * 0.4f, h * 0.8f),
                                    Offset(w * 0.6f, h * 0.2f),
                                    Offset(w * 0.8f, h * 0.6f),
                                    Offset(w, h * 0.35f)
                                )
                                val path = Path().apply {
                                    moveTo(points[0].x, points[0].y)
                                    for (i in 1 until points.size) {
                                        lineTo(points[i].x, points[i].y)
                                    }
                                }
                                drawPath(path, color = themeColor.primary, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "28%",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Usage",
                                    color = TextDimHud,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Network Status Card
                DashboardCard(
                    title = "NETWORK",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📶", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (telemetry.isWifi) "Wi-Fi Active" else "Wi-Fi Standby",
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (telemetry.isWifi) "120 Mbps" else "Disconnected",
                                    color = TextDimHud,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (telemetry.isCellular) "Cellular Active" else "Jio 5G",
                                    color = if (telemetry.isCellular) StarkEmerald else TextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Signal Strong",
                                    color = TextDimHud,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Detailed Device Info Specifications Card
                DashboardCard(
                    title = "DEVICE INFO",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeviceInfoRow("Model", "Motorola Edge 50 Fusion")
                        DeviceInfoRow("Android", "${Build.VERSION.RELEASE} (${Build.ID})")
                        DeviceInfoRow("Screen", "2400 x 1080 - ${telemetry.refreshRateFormatted}")
                        DeviceInfoRow("Uptime", telemetry.uptimeFormatted)
                        DeviceInfoRow("Temperature", telemetry.batteryTempFormatted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dismiss Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.subtle)
                        .border(1.dp, themeColor.primary, RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (customization.hapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onDismiss()
                            }
                        )
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CLOSE DASHBOARD",
                        color = themeColor.primary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.6.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x1F00E5FF))
            .border(1.dp, StarkHudBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            color = StarkCyanBright,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        content()
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextDimHud,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}
