package com.motorola.launcher.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkIceBlue
import com.motorola.launcher.ui.theme.StarkReactorRed
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Holographic Digital Command Clock for STARK OS / JARVIS.
 * Displays live high-contrast digital time with system 12/24-hour auto-detection,
 * military/HUD date formatting, and real-time core telemetry tags.
 */
@Composable
fun DigitalClock(
    telemetry: SystemTelemetryState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Efficient minute-cadence clock update (zero per-frame polling)
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            val millisToNextMinute = 60000L - (currentTime % 60000L)
            delay(millisToNextMinute)
        }
    }

    val is24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val timePattern = if (is24Hour) "HH:mm" else "hh:mm"
    val timeFormat = remember(timePattern) { SimpleDateFormat(timePattern, Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd // EEE", Locale.US) }

    val date = remember(currentTime) { Date(currentTime) }
    val timeText = remember(currentTime, timeFormat) { timeFormat.format(date) }
    val amPmText = remember(currentTime, amPmFormat) { if (!is24Hour) amPmFormat.format(date).uppercase(Locale.US) else "" }
    val dateText = remember(currentTime, dateFormat) { dateFormat.format(date).uppercase(Locale.US) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Holographic Protocol Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x1F00F0FF))
                .border(1.dp, StarkHudBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (telemetry.isCharging) StarkEmerald else StarkCyan)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "STARK OS // PROTOCOL: JARVIS // ONLINE",
                color = StarkCyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Cybernetic Monospace Digital Clock
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = timeText,
                fontSize = 64.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                letterSpacing = (-2).sp,
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TextPrimary,
                            StarkCyanBright,
                            StarkIceBlue
                        )
                    )
                )
            )
            if (amPmText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmText,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = StarkCyan,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // Tactical Date & Hardware Telemetry Strip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = dateText,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.0.sp
            )

            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "|", color = StarkHudBorder, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(10.dp))

            // Refresh Rate Badge
            Text(
                text = "${telemetry.refreshRateHz}Hz",
                color = StarkCyan,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.width(10.dp))
            Text(text = "|", color = StarkHudBorder, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(modifier = Modifier.width(10.dp))

            // Real Network Transport Badge
            Text(
                text = telemetry.networkType.uppercase(Locale.US),
                color = if (telemetry.isWifi) StarkCyan else if (telemetry.isCellular) StarkEmerald else TextDimHud,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
