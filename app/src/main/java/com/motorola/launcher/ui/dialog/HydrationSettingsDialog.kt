package com.motorola.launcher.ui.dialog

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.motorola.launcher.data.db.HydrationSettingsEntity
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator

/**
 * Settings configuration dialog for Smart Water Reminder and Companion.
 */
@Composable
fun HydrationSettingsDialog(
    orchestrator: FamilyCompanionOrchestrator,
    onDismiss: () -> Unit
) {
    val view = LocalView.current
    val currentSettings by orchestrator.settings.collectAsState()

    var enabled by remember(currentSettings) { mutableStateOf(currentSettings.enabled) }
    var dailyGoalMl by remember(currentSettings) { mutableIntStateOf(currentSettings.dailyGoalMl) }
    var reminderInterval by remember(currentSettings) { mutableIntStateOf(currentSettings.reminderIntervalMinutes) }
    var selectedCharacter by remember(currentSettings) { mutableStateOf(currentSettings.selectedCharacters) }
    var language by remember(currentSettings) { mutableStateOf(currentSettings.language) }
    var voiceEnabled by remember(currentSettings) { mutableStateOf(currentSettings.voiceEnabled) }
    var quietHoursEnabled by remember(currentSettings) { mutableStateOf(currentSettings.quietHoursEnabled) }
    var snoozeDuration by remember(currentSettings) { mutableIntStateOf(currentSettings.snoozeDurationMinutes) }
    var startTime by remember(currentSettings) { mutableStateOf(currentSettings.startTime) }
    var endTime by remember(currentSettings) { mutableStateOf(currentSettings.endTime) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF070F18))
                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            color = Color(0xFF070F18)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HYDRATION SETTINGS",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "நினைவூட்டல் அமைப்புகள்",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

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

                Spacer(modifier = Modifier.height(16.dp))

                // Master Toggle: Enable Reminders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x1F00E5FF))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("தண்ணீர் நினைவூட்டல்", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Enable daily smart reminders", color = Color(0xFF80DEEA), fontSize = 11.sp)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Target Goal Slider (1000ml to 4000ml)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("தினசரி இலக்கு (Daily Goal)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("${dailyGoalMl} ml", color = Color(0xFF00E5FF), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = dailyGoalMl.toFloat(),
                        onValueChange = { dailyGoalMl = (it / 100).toInt() * 100 },
                        valueRange = 1000f..4000f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF),
                            inactiveTrackColor = Color(0x3300E5FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder Interval Selector (30m, 45m, 60m, 90m, 120m)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("நினைவூட்டல் இடைவெளி (Frequency)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(30, 45, 60, 90, 120).forEach { mins ->
                            SelectableChip(
                                label = "${mins}m",
                                selected = reminderInterval == mins,
                                modifier = Modifier.weight(1f)
                            ) {
                                reminderInterval = mins
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Character Selection: Mithra, Amma, Both
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("குரல் கதாப்பாத்திரம் (Companion)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectableChip(
                            label = "மித்ரா (Mithra)",
                            selected = selectedCharacter.equals("MITHRA", ignoreCase = true),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedCharacter = "MITHRA"
                        }
                        SelectableChip(
                            label = "அம்மா (Amma)",
                            selected = selectedCharacter.equals("AMMA", ignoreCase = true),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedCharacter = "AMMA"
                        }
                        SelectableChip(
                            label = "இருவரும் (Both)",
                            selected = selectedCharacter.equals("BOTH", ignoreCase = true),
                            modifier = Modifier.weight(1f)
                        ) {
                            selectedCharacter = "BOTH"
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Waking Window & Quiet Hours
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("அமைதியான நேரம் (Quiet Hours)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("$startTime முதல் $endTime வரை மட்டுமே", color = Color(0xFF80DEEA), fontSize = 11.sp)
                    }
                    Switch(
                        checked = quietHoursEnabled,
                        onCheckedChange = { quietHoursEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Voice Announcements Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("குரல் அறிவிப்பு (Voice Reminder)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Speak Tamil dialogue when opening", color = Color(0xFF80DEEA), fontSize = 11.sp)
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { voiceEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00E5FF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save Changes Button
                Button(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        val updated = currentSettings.copy(
                            enabled = enabled,
                            dailyGoalMl = dailyGoalMl,
                            reminderIntervalMinutes = reminderInterval,
                            selectedCharacters = selectedCharacter,
                            language = language,
                            voiceEnabled = voiceEnabled,
                            quietHoursEnabled = quietHoursEnabled,
                            snoozeDurationMinutes = snoozeDuration,
                            startTime = startTime,
                            endTime = endTime
                        )
                        orchestrator.saveSettings(updated)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "சேமி (SAVE SETTINGS)",
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color(0xFF00E5FF) else Color(0x2600E5FF))
            .border(1.dp, if (selected) Color(0xFF80F7FF) else Color(0x3300E5FF), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.SansSerif
        )
    }
}
