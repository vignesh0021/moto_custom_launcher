package com.motorola.launcher.ui.widgets

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.workspace.HudGlass
import com.motorola.launcher.ui.workspace.HudTextStyle

@Composable
fun CyberTelemetryWidget(
    telemetry: SystemTelemetryState,
    modifier: Modifier = Modifier,
    onOpenDiagnostics: () -> Unit = {}
) {
    val marvelTheme = LocalMarvelTheme.current
    val shape = chamferedShape(cutSize = 12f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(HudGlass)
            .border(1.dp, marvelTheme.colors.hudBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenDiagnostics
            )
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "QUANTUM CORE TELEMETRY",
                    color = marvelTheme.colors.bright,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    style = HudTextStyle
                )

                Text(
                    text = if (telemetry.isCharging) "CHARGING" else "BATTERY",
                    color = marvelTheme.colors.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Gauges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery Gauge
                Column {
                    Text(
                        text = "${telemetry.batteryLevel}%",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        style = HudTextStyle
                    )
                    Text(
                        text = "CORE CELL LEVEL",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Temp & RAM
                val ramUsedMb = if (telemetry.ramUsedBytes > 0) telemetry.ramUsedBytes / (1024 * 1024) else 0L
                val ramTotalMb = if (telemetry.ramTotalBytes > 0) telemetry.ramTotalBytes / (1024 * 1024) else 0L
                val tempText = if (telemetry.batteryTempCelsius.isNaN()) "--" else String.format(Locale.US, "%.1f", telemetry.batteryTempCelsius)
                val refreshRateText = if (telemetry.refreshRateHz > 0) "${telemetry.refreshRateHz}Hz" else "60Hz"

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$tempText°C",
                        color = marvelTheme.colors.primary,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = HudTextStyle
                    )
                    Text(
                        text = "$ramUsedMb MB / $ramTotalMb MB",
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Footer Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "VOLT: ${telemetry.batteryVoltageMv}mV",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "REFRESH: $refreshRateText",
                    color = marvelTheme.colors.secondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
