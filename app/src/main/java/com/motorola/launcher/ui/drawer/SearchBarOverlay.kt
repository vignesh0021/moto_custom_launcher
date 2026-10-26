package com.motorola.launcher.ui.drawer

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
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
    val themeColor = customization.themeColor
    val pillShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(pillShape)
            .background(Color(0x1F060C18))
            .border(
                width = 1.dp,
                color = if (query.isNotEmpty()) themeColor.bright else StarkHudBorder,
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
                color = themeColor.primary,
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
                cursorBrush = SolidColor(themeColor.primary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps, contacts, web...",
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
                        .background(Color(0x3300F0FF))
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

            // Microphone Voice Search Trigger if search is empty
            if (query.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (customization.hapticFeedback) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                launchVoiceSearch(context)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎙",
                        color = themeColor.primary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

private fun launchVoiceSearch(context: Context) {
    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        // Fallback gracefully if speech service is unavailable
    }
}
