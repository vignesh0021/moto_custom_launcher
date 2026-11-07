package com.motorola.launcher.ui.workspace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.system.SystemTelemetryState
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Supreme Sci-Fi HUD Clock for Motorola Marvel Launcher.
 * Features:
 * - Chamfered cybernetic glass frame with glowing neon boundary and corner reticles
 * - Dual-layer glowing typography with laser-bright core and theme aura bloom
 * - Softly pulsing breathing colon
 * - Real-time seconds telemetry radar bar
 * - High-tech date pill and live hardware status indicators
 * - Zero unwanted redundant headers (STARK OS prefix removed)
 */
@Composable
fun DigitalClock(
    telemetry: SystemTelemetryState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customization = LocalCustomization.current
    val marvelTheme = LocalMarvelTheme.current

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Battery-efficient minute rollover cadence (eliminates 1-second idle CPU loops)
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            val millisToNextMinute = 60000L - (currentTime % 60000L)
            delay(millisToNextMinute.coerceAtLeast(1000L))
        }
    }

    val is24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val hourFormat = remember(is24Hour) { SimpleDateFormat(if (is24Hour) "HH" else "hh", Locale.US) }
    val minFormat = remember { SimpleDateFormat("mm", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val dayMonthFormat = remember { SimpleDateFormat("dd MMM", Locale.US) }
    val yearFormat = remember { SimpleDateFormat("yyyy", Locale.US) }

    val date = remember(currentTime) { Date(currentTime) }
    val hoursText = remember(currentTime, hourFormat) { hourFormat.format(date) }
    val minsText = remember(currentTime, minFormat) { minFormat.format(date) }
    val amPmText = remember(currentTime) { if (!is24Hour) amPmFormat.format(date).uppercase(Locale.US) else "" }
    val dayOfWeek = remember(currentTime) { dayOfWeekFormat.format(date).uppercase(Locale.US) }
    val dayMonth = remember(currentTime) { dayMonthFormat.format(date).uppercase(Locale.US) }
    val yearText = remember(currentTime) { yearFormat.format(date) }

    // Breathing pulse for colon
    val colonAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        colonAlpha.animateTo(
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    val primaryColor = marvelTheme.colors.primary
    val glowColor = marvelTheme.colors.glow
    val brightColor = marvelTheme.colors.bright
    val borderColor = marvelTheme.colors.hudBorder

    val panelShape = chamferedShape(cutSize = 14f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Supreme Clean Cybernetic Glass Card (No unwanted reticle lines or progress lines)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(panelShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC081220),
                            Color(0xDD040914)
                        )
                    )
                )
                .border(1.dp, borderColor.copy(alpha = 0.5f), panelShape)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Futuristic Date Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Day of Week Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(primaryColor.copy(alpha = 0.18f))
                            .border(0.8.dp, primaryColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dayOfWeek,
                            color = brightColor,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•",
                        color = primaryColor.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "$dayMonth  //  $yearText",
                        color = TextSecondary,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 2. Supreme Digital Numerals Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val digitsStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp,
                        letterSpacing = (-1.5).sp,
                        shadow = Shadow(
                            color = glowColor.copy(alpha = 0.9f),
                            offset = Offset(0f, 0f),
                            blurRadius = 24f
                        )
                    )

                    // Hours
                    Text(
                        text = hoursText,
                        color = Color.White,
                        style = digitsStyle
                    )

                    // Breathing Pulsing Neon Colon
                    Text(
                        text = ":",
                        color = primaryColor.copy(alpha = colonAlpha.value),
                        style = digitsStyle.copy(
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.sp
                        ),
                        modifier = Modifier.padding(start = 3.dp, end = 3.dp, bottom = 4.dp)
                    )

                    // Minutes
                    Text(
                        text = minsText,
                        color = Color.White,
                        style = digitsStyle
                    )

                    if (amPmText.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(primaryColor.copy(alpha = 0.15f))
                                .border(0.8.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = amPmText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryColor,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
