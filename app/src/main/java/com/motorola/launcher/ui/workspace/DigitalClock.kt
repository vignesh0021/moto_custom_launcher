package com.motorola.launcher.ui.workspace

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
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
import com.motorola.launcher.ui.theme.StarkEmerald
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
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

    // High-precision live second cadence for supreme HUD animations
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L - (currentTime % 1000L))
        }
    }

    val is24Hour = remember(context) { android.text.format.DateFormat.is24HourFormat(context) }
    val hourFormat = remember(is24Hour) { SimpleDateFormat(if (is24Hour) "HH" else "hh", Locale.US) }
    val minFormat = remember { SimpleDateFormat("mm", Locale.US) }
    val secFormat = remember { SimpleDateFormat("ss", Locale.US) }
    val amPmFormat = remember { SimpleDateFormat("a", Locale.US) }
    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale.US) }
    val dayMonthFormat = remember { SimpleDateFormat("dd MMM", Locale.US) }
    val yearFormat = remember { SimpleDateFormat("yyyy", Locale.US) }

    val date = remember(currentTime) { Date(currentTime) }
    val hoursText = remember(currentTime, hourFormat) { hourFormat.format(date) }
    val minsText = remember(currentTime, minFormat) { minFormat.format(date) }
    val secsText = remember(currentTime, secFormat) { secFormat.format(date) }
    val amPmText = remember(currentTime) { if (!is24Hour) amPmFormat.format(date).uppercase(Locale.US) else "" }
    val dayOfWeek = remember(currentTime) { dayOfWeekFormat.format(date).uppercase(Locale.US) }
    val dayMonth = remember(currentTime) { dayMonthFormat.format(date).uppercase(Locale.US) }
    val yearText = remember(currentTime) { yearFormat.format(date) }

    val currentSecond = remember(currentTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTime }
        cal.get(Calendar.SECOND)
    }

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
        // Supreme Cybernetic Frame Card
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
                .border(1.dp, borderColor.copy(alpha = 0.65f), panelShape)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Cybernetic Corner HUD Reticles Canvas
            Canvas(modifier = Modifier.matchParentSize()) {
                val bracketLen = 14.dp.toPx()
                val strokeW = 1.5.dp.toPx()
                val color = primaryColor.copy(alpha = 0.85f)

                // Top-Left Bracket
                drawLine(color, Offset(0f, 0f), Offset(bracketLen, 0f), strokeWidth = strokeW, cap = StrokeCap.Square)
                drawLine(color, Offset(0f, 0f), Offset(0f, bracketLen), strokeWidth = strokeW, cap = StrokeCap.Square)

                // Top-Right Bracket
                drawLine(color, Offset(size.width - bracketLen, 0f), Offset(size.width, 0f), strokeWidth = strokeW, cap = StrokeCap.Square)
                drawLine(color, Offset(size.width, 0f), Offset(size.width, bracketLen), strokeWidth = strokeW, cap = StrokeCap.Square)

                // Bottom-Left Bracket
                drawLine(color, Offset(0f, size.height - bracketLen), Offset(0f, size.height), strokeWidth = strokeW, cap = StrokeCap.Square)
                drawLine(color, Offset(0f, size.height), Offset(bracketLen, size.height), strokeWidth = strokeW, cap = StrokeCap.Square)

                // Bottom-Right Bracket
                drawLine(color, Offset(size.width - bracketLen, size.height), Offset(size.width, size.height), strokeWidth = strokeW, cap = StrokeCap.Square)
                drawLine(color, Offset(size.width, size.height - bracketLen), Offset(size.width, size.height), strokeWidth = strokeW, cap = StrokeCap.Square)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Futuristic Date Header Bar (Unwanted header prefix removed)
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
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dayOfWeek,
                            color = brightColor,
                            fontSize = 9.sp,
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
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Supreme Digital Numerals Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val digitsStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 54.sp,
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
                            fontSize = 48.sp,
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

                    Spacer(modifier = Modifier.width(10.dp))

                    // Seconds Telemetry & AM/PM Column
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (amPmText.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(primaryColor.copy(alpha = 0.15f))
                                    .border(0.8.dp, primaryColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = amPmText,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Live Second Badge
                        Text(
                            text = "${secsText}S",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = brightColor,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // 3. Cybernetic 60-Second HUD Progress Line
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(3.dp)
                ) {
                    val trackColor = Color(0x33FFFFFF)
                    val activeBarWidth = size.width * (currentSecond / 59f)

                    // Background Track
                    drawLine(
                        color = trackColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Active Glowing Second Progress Bar
                    if (activeBarWidth > 0f) {
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.4f),
                                    brightColor
                                ),
                                endX = activeBarWidth
                            ),
                            start = Offset(0f, size.height / 2f),
                            end = Offset(activeBarWidth, size.height / 2f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 4. Live Hardware & Telemetry Capsule
                if (customization.showSystemInfo) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33000000))
                            .border(0.8.dp, borderColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (telemetry.isCharging) StarkEmerald else primaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${marvelTheme.hudConfig.statusBadge} · ${telemetry.refreshRateFormatted} · ${telemetry.networkType.uppercase(Locale.US)}",
                            color = primaryColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }
    }
}
