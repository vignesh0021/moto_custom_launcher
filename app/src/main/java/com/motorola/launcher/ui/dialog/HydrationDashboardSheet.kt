package com.motorola.launcher.ui.dialog

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.motorola.launcher.data.repository.HydrationRepository
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Hydration Dashboard for the V2H Family Companion.
 * Displays daily progress ring, consumption breakdown, weekly bar chart,
 * quick-add water intake triggers, and reminder management.
 */
@Composable
fun HydrationDashboardSheet(
    orchestrator: FamilyCompanionOrchestrator,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val settings by orchestrator.settings.collectAsState()
    val consumedMl by orchestrator.todayTotalMl.collectAsState()
    val remainingMl by orchestrator.remainingMl.collectAsState()
    val progressRatio by orchestrator.progressRatio.collectAsState()
    val glassesCount by orchestrator.glassesCount.collectAsState()
    val isGoalReached by orchestrator.isGoalReached.collectAsState()
    val activeDialogue by orchestrator.activeDialogue.collectAsState()

    var showCustomAmountDialog by remember { mutableStateOf(false) }
    var customInputText by remember { mutableStateOf("") }

    // Load past 7 days hydration for the weekly chart
    val weeklyHistory by produceState<Map<String, Int>>(
        initialValue = emptyMap(),
        consumedMl
    ) {
        val keys = HydrationRepository.getPastDaysDateKeys(7)
        val startKey = keys.first()
        val endKey = keys.last()
        orchestrator.repository.getWeeklyIntakes(startKey, endKey).collect { entries ->
            val map = keys.associateWith { 0 }.toMutableMap()
            entries.forEach { entry ->
                val prev = map[entry.dateKey] ?: 0
                map[entry.dateKey] = prev + entry.amountMl
            }
            value = map
        }
    }

    // Last recorded intake time
    val lastIntakeEntry by orchestrator.repository.lastIntake.collectAsState(initial = null)
    val lastIntakeFormatted = remember(lastIntakeEntry) {
        lastIntakeEntry?.let {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(it.timestamp))
        } ?: "No records yet"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp),
            color = Color(0xF2070D14),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.verticalGradient(listOf(Color(0xFF00E5FF).copy(alpha = 0.5f), Color(0x1100E5FF)))
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Sheet Drag Bar
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(Color(0x6600E5FF))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Header Title with Settings Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SMART HYDRATION",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "தண்ணீர் நினைவூட்டல்",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Settings Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x3300E5FF))
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    onOpenSettings()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "⚙️ அமைப்பு",
                                color = Color(0xFFE0F7FA),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Close Button
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .clickable { onDismiss() }
                                .padding(8.dp)
                        ) {
                            Text("✕", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Goal Completion Celebration Banner
                AnimatedVisibility(
                    visible = isGoalReached,
                    enter = fadeIn() + scaleIn()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF00C853).copy(alpha = 0.25f), Color(0xFF00E5FF).copy(alpha = 0.25f))))
                            .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎉", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "இன்றைய இலக்கு முடிந்தது!",
                                    color = Color(0xFFE0F7FA),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mithra & Amma are so proud of you! Stay refreshed.",
                                    color = Color(0xFFB2EBF2),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Companion Dialogue Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x2600E5FF))
                        .border(1.dp, Color(0x4000E5FF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "💬 ${activeDialogue.title}",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${activeDialogue.tamilText}\"",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeDialogue.englishText,
                            color = Color(0xFF80DEEA),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section C: Daily Progress Ring
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val animatedAngle by animateFloatAsState(
                        targetValue = progressRatio * 360f,
                        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        label = "progress_ring"
                    )

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        val padding = strokeWidth / 2
                        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
                        val topLeft = Offset(padding, padding)

                        // Background track
                        drawArc(
                            color = Color(0x2600E5FF),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Progress arc
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF00B0FF),
                                    Color(0xFF80F7FF),
                                    Color(0xFF00E5FF)
                                )
                            ),
                            startAngle = -90f,
                            sweepAngle = animatedAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(progressRatio * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$consumedMl / ${settings.dailyGoalMl} ml",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$glassesCount டம்ளர்",
                            color = Color(0xFF80DEEA),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stats Metrics Row (Consumed, Target, Remaining)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "குடித்தது",
                        value = "${consumedMl}ml",
                        sub = "$glassesCount glasses",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "இலக்கு",
                        value = "${settings.dailyGoalMl}ml",
                        sub = "Daily Target",
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "மீதம்",
                        value = "${remainingMl}ml",
                        sub = "Remaining",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Auxiliary Telemetry (Last Intake & Next Reminder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1A00E5FF))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("கடைசி பதிவு", color = Color(0xFF80DEEA), fontSize = 10.sp)
                            Text(lastIntakeFormatted, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1A00E5FF))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("அடுத்த நினைவூட்டல்", color = Color(0xFF80DEEA), fontSize = 10.sp)
                            Text(
                                if (settings.enabled) "ஒவ்வொரு ${settings.reminderIntervalMinutes} நிமிடம்" else "முடக்கப்பட்டது",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Add Section Title
                Text(
                    text = "உடனடி பதிவு / QUICK ADD WATER",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Add Buttons: 100ml, 150ml, 200ml, 250ml, Custom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickAddButton("+100ml", Modifier.weight(1f)) {
                        coroutineScope.launch {
                            orchestrator.recordWaterIntake(100, "DASHBOARD_100")
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                    QuickAddButton("+150ml", Modifier.weight(1f)) {
                        coroutineScope.launch {
                            orchestrator.recordWaterIntake(150, "DASHBOARD_150")
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                    QuickAddButton("+200ml", Modifier.weight(1f)) {
                        coroutineScope.launch {
                            orchestrator.recordWaterIntake(200, "DASHBOARD_200")
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                    QuickAddButton("+250ml", Modifier.weight(1f)) {
                        coroutineScope.launch {
                            orchestrator.recordWaterIntake(250, "DASHBOARD_250")
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                    QuickAddButton("Custom", Modifier.weight(1.1f)) {
                        showCustomAmountDialog = true
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section C: Weekly Consumption Chart
                Text(
                    text = "வாராந்திர வரலாறு / WEEKLY HISTORY",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                WeeklyHistoryBarChart(
                    history = weeklyHistory,
                    goalMl = settings.dailyGoalMl
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reset and Snooze action row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            orchestrator.snoozeReminder()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300E5FF)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("15m தள்ளிவை (Snooze)", color = Color(0xFF80DEEA), fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            orchestrator.resetToday()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22FF5252)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("இன்றையதை அழி (Reset)", color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Modal Dialog for Custom Amount Entry
    if (showCustomAmountDialog) {
        AlertDialog(
            onDismissRequest = { showCustomAmountDialog = false },
            title = { Text("தண்ணீர் அளவு (ml)", color = Color(0xFF00E5FF)) },
            text = {
                Column {
                    Text("Enter custom water intake amount in millilitres:", color = Color.White, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customInputText,
                        onValueChange = { customInputText = it.filter { ch -> ch.isDigit() }.take(4) },
                        placeholder = { Text("e.g. 350", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = customInputText.toIntOrNull() ?: 0
                        if (amount > 0) {
                            coroutineScope.launch {
                                orchestrator.recordWaterIntake(amount, "CUSTOM")
                            }
                        }
                        showCustomAmountDialog = false
                        customInputText = ""
                    }
                ) {
                    Text("பதிவு செய் (Log)", color = Color(0xFF00E5FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAmountDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF0A141E)
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1F00E5FF))
            .border(1.dp, Color(0x3300E5FF), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(title, color = Color(0xFF80DEEA), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text(sub, color = Color(0x99FFFFFF), fontSize = 9.sp)
        }
    }
}

@Composable
private fun QuickAddButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x3300E5FF))
            .border(1.dp, Color(0x5500E5FF), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFE0F7FA),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun WeeklyHistoryBarChart(
    history: Map<String, Int>,
    goalMl: Int
) {
    val maxMl = remember(history, goalMl) {
        (history.values.maxOrNull() ?: 0).coerceAtLeast(goalMl).coerceAtLeast(2000)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1400E5FF))
            .border(1.dp, Color(0x2600E5FF), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            history.entries.forEach { (dateKey, ml) ->
                val dayLabel = try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = sdf.parse(dateKey)
                    val dayFormat = SimpleDateFormat("EEE", Locale.US)
                    dayFormat.format(date ?: Date())
                } catch (e: Exception) {
                    dateKey.takeLast(2)
                }

                val barRatio = (ml.toFloat() / maxMl).coerceIn(0.04f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (ml > 0) "${ml / 100}h" else "",
                        color = Color(0xFF80DEEA),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(65.dp * barRatio)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                if (ml >= goalMl) {
                                    Brush.verticalGradient(listOf(Color(0xFF80F7FF), Color(0xFF00E5FF)))
                                } else {
                                    Brush.verticalGradient(listOf(Color(0xFF0091EA), Color(0xFF01579B)))
                                }
                            )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayLabel,
                        color = if (dateKey == HydrationRepository.getTodayDateKey()) Color(0xFF00E5FF) else Color(0x99FFFFFF),
                        fontSize = 9.sp,
                        fontWeight = if (dateKey == HydrationRepository.getTodayDateKey()) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
