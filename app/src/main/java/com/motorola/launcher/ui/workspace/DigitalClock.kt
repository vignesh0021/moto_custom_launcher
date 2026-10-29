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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Header & clock for STARK OS — JARVIS Edition.
 * Centered branding, date, large time and one compact live-status line.
 * Only real data is shown (no weather — the launcher has no weather source).
 */
@Composable
fun DigitalClock(
    telemetry: SystemTelemetryState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themeColor = LocalCustomization.current.themeColor
    val customization = LocalCustomization.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Minute-cadence clock update (no per-frame polling)
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(60000L - (currentTime % 60000L))
        }
    }

    val is24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val timeFormat = remember(is24Hour) { SimpleDateFormat(if (is24Hour) "HH:mm" else "hh:mm", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dateFormat = remember { SimpleDateFormat("EEE, dd MMM", Locale.US) }

    val date = remember(currentTime) { Date(currentTime) }
    val timeText = remember(currentTime, timeFormat) { timeFormat.format(date) }
    val amPmText = remember(currentTime) { if (!is24Hour) amPmFormat.format(date).uppercase(Locale.US) else "" }
    val dateText = remember(currentTime) { dateFormat.format(date).uppercase(Locale.US) }

    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = dateText,
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp,
            style = HudTextStyle
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = timeText,
                fontSize = 52.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                letterSpacing = (-1).sp,
                style = HudTextStyle
            )
            if (amPmText.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmText,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = marvelTheme.colors.primary,
                    modifier = Modifier.padding(bottom = 10.dp),
                    style = HudTextStyle
                )
            }
        }

        if (customization.showSystemInfo) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(HudGlass)
                    .border(1.dp, marvelTheme.colors.hudBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (telemetry.isCharging) StarkEmerald else marvelTheme.colors.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${marvelTheme.hudConfig.statusBadge} · ${telemetry.refreshRateFormatted} · ${telemetry.networkType.uppercase(Locale.US)}",
                    color = marvelTheme.colors.primary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
