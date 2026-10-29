package com.motorola.launcher.ui.drawer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.LocalCustomization
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.TextDimHud
import com.motorola.launcher.ui.theme.TextPrimary

/**
 * Holographic Cybernetic Search Bar for STARK OS Application Index.
 * Features rounded pill frame, magnifying glass search reticle, and voice search trigger.
 */
@Composable
fun SearchBarOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    appCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val customization = LocalCustomization.current
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current
    val themeColor = customization.themeColor
    val pillShape = RoundedCornerShape(24.dp)

    // Voice Assistance Speech Recognizer Launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenList?.firstOrNull()?.trim() ?: ""
            if (spokenText.isNotEmpty()) {
                onQueryChange(spokenText)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(pillShape)
            .background(Color(0x1F060C18))
            .border(
                width = 1.dp,
                color = if (query.isNotEmpty()) marvelTheme.colors.bright else marvelTheme.colors.hudBorder,
                shape = pillShape
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Search Magnifying Glass Reticle
            Text(
                text = "🔍",
                color = marvelTheme.colors.primary,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 8.dp)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.3.sp
                ),
                cursorBrush = SolidColor(marvelTheme.colors.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = marvelTheme.drawerStyle.searchPlaceholder,
                            color = TextDimHud,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 0.3.sp
                        )
                    }
                    innerTextField()
                }
            )

            // Clear button if search query active
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(marvelTheme.colors.glow)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearQuery
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = themeColor.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Marvel Voice Assistant Microphone Trigger
            if (query.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(themeColor.primary.copy(alpha = 0.18f))
                        .border(1.dp, themeColor.primary.copy(alpha = 0.5f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (customization.hapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                try {
                                    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search apps...")
                                    }
                                    voiceLauncher.launch(speechIntent)
                                } catch (_: Exception) {
                                    // Fallback to System Voice Assistant
                                    try {
                                        val assistIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(assistIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Voice assistance unavailable", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎙",
                        color = themeColor.bright,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
