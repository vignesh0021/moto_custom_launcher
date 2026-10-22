package com.motorola.launcher.ui.workspace

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoElectricViolet
import com.motorola.launcher.ui.theme.MotoEmerald
import com.motorola.launcher.ui.theme.MotoGlassBorder
import com.motorola.launcher.ui.theme.MotoGlassBorderActive
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Flagship Motorola Edge Curved Smart Clock & Glanceable Information Widget.
 * Features dual-tone gradient typography, dynamic battery telemetry, and time-of-day greeting.
 */
@Composable
fun ClockWidget(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    // Live Clock Cadence (updates once per minute precisely on minute rollover)
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            val millisToNextMinute = 60000L - (currentTime % 60000L)
            delay(millisToNextMinute)
        }
    }

    // Dynamic Hardware Battery Telemetry
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent != null) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level >= 0 && scale > 0) {
                        batteryLevel = (level * 100) / scale
                    }
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = context.registerReceiver(receiver, filter)
        if (initialIntent != null) {
            val level = initialIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = initialIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryLevel = (level * 100) / scale
            }
            val status = initialIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        }
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Throwable) {
                // receiver unregister safety
            }
        }
    }

    val is24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val timePattern = if (is24Hour) "HH:mm" else "h:mm"
    val timeFormat = remember(timePattern) { SimpleDateFormat(timePattern, Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    val date = remember(currentTime) { Date(currentTime) }
    val timeText = remember(currentTime, timeFormat) { timeFormat.format(date) }
    val dateText = remember(currentTime) { dateFormat.format(date) }

    val greeting = remember(currentTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Dynamic Greeting Pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, MotoGlassBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isCharging) MotoEmerald else MotoCyanAccent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = greeting,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Cyber-Gradient Digital Clock
        Text(
            text = timeText,
            fontSize = 76.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-3).sp,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        TextPrimary,
                        MotoCyanAccent.copy(alpha = 0.95f),
                        MotoElectricViolet.copy(alpha = 0.85f)
                    )
                )
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Date and Real Battery Telemetry Glance Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x14FFFFFF))
                .border(1.dp, Color(0x2E00E5FF), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp)
        ) {
            Text(
                text = dateText.uppercase(Locale.getDefault()),
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Glowing Separator Dot
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MotoCyanAccent)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Real Battery Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isCharging) "⚡" else "🔋",
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryLevel%",
                    color = if (isCharging) MotoEmerald else if (batteryLevel <= 20) Color(0xFFFF5252) else MotoCyanAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
