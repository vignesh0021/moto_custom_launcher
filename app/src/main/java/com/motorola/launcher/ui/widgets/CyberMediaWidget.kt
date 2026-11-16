package com.motorola.launcher.ui.widgets

import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.components.chamferedShape
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.workspace.HudGlass
import com.motorola.launcher.ui.workspace.HudTextStyle

@Composable
fun CyberMediaWidget(
    modifier: Modifier = Modifier,
    onLaunchMusicApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val marvelTheme = LocalMarvelTheme.current
    val shape = chamferedShape(cutSize = 12.dp)
    var isPlaying by remember { mutableStateOf(false) }

    fun sendMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val eventTime = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(HudGlass)
            .border(1.dp, marvelTheme.colors.hudBorder, shape)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLaunchMusicApp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "STARK AUDIO SYSTEM",
                    color = marvelTheme.colors.bright,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    style = HudTextStyle
                )

                // Equalizer Bar Animation indicator
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                    val heights = listOf(10.dp, 16.dp, 12.dp, 18.dp, 8.dp)
                    for (h in heights) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(h)
                                .clip(RoundedCornerShape(1.dp))
                                .background(marvelTheme.colors.primary)
                        )
                    }
                }
            }

            // Track info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLaunchMusicApp
                    )
            ) {
                Text(
                    text = if (isPlaying) "QUANTUM AUDIO STREAM" else "TAP PLAY TO RESUME",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    style = HudTextStyle
                )
                Text(
                    text = "JARVIS NEURAL MUSIC CORE",
                    color = marvelTheme.colors.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            // Media Control Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // PREV
                MediaPillButton(text = "|<") {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }

                // PLAY / PAUSE
                MediaPillButton(text = if (isPlaying) "||" else "|>", isHighlighted = true) {
                    isPlaying = !isPlaying
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }

                // NEXT
                MediaPillButton(text = ">|") {
                    sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
                }
            }
        }
    }
}

@Composable
private fun MediaPillButton(
    text: String,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val marvelTheme = LocalMarvelTheme.current
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width = 54.dp, height = 32.dp)
            .clip(shape)
            .background(if (isHighlighted) marvelTheme.colors.primary.copy(alpha = 0.25f) else Color(0x33000000))
            .border(
                width = 1.dp,
                color = if (isHighlighted) marvelTheme.colors.primary else marvelTheme.colors.hudBorder,
                shape = shape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isHighlighted) marvelTheme.colors.bright else TextSecondary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            style = HudTextStyle
        )
    }
}
