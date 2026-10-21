package com.motorola.launcher.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoElectricViolet
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextTertiary

private val ALPHABET = ('A'..'Z').toList()

/**
 * Interactive A-Z Fast Scroll Indexer with Floating Holographic Magnifier Bubble.
 * Specially tuned for Motorola Edge 50 curved waterfall display margins.
 */
@Composable
fun FastScrollScrubber(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var activeOffsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxHeight(0.85f)
            .width(42.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Holographic Letter Bubble
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        x = (-56).dp.roundToPx(),
                        y = (activeOffsetY - 28.dp.toPx()).toInt().coerceAtLeast(0)
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xEE1E293B),
                                Color(0xEE0A0F1D)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(MotoCyanAccent, MotoElectricViolet)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter?.toString() ?: "",
                    color = MotoCyanAccent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Alphabet Index Column
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x1A000000))
                .pointerInput(Unit) {
                    // Unified pointer event: handles both single tap and continuous vertical drag
                    detectTapGestures(
                        onPress = { offset ->
                            val totalHeight = size.height
                            val itemHeight = totalHeight / ALPHABET.size.toFloat()
                            val letterIndex = (offset.y / itemHeight).toInt().coerceIn(0, ALPHABET.size - 1)
                            val letter = ALPHABET[letterIndex]

                            activeLetter = letter
                            activeOffsetY = offset.y
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onLetterSelected(letter)

                            tryAwaitRelease()
                            activeLetter = null
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val totalHeight = size.height
                            val itemHeight = totalHeight / ALPHABET.size.toFloat()
                            val letterIndex = (offset.y / itemHeight).toInt().coerceIn(0, ALPHABET.size - 1)
                            val letter = ALPHABET[letterIndex]

                            activeLetter = letter
                            activeOffsetY = offset.y
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onLetterSelected(letter)
                        },
                        onDragEnd = {
                            activeLetter = null
                        },
                        onDragCancel = {
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            val totalHeight = size.height
                            val itemHeight = totalHeight / ALPHABET.size.toFloat()
                            val touchY = change.position.y.coerceIn(0f, (totalHeight - 1).toFloat())
                            val letterIndex = (touchY / itemHeight).toInt().coerceIn(0, ALPHABET.size - 1)
                            val letter = ALPHABET[letterIndex]

                            if (activeLetter != letter) {
                                activeLetter = letter
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onLetterSelected(letter)
                            }
                            activeOffsetY = touchY
                            change.consume()
                        }
                    )
                }
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (char in ALPHABET) {
                val isSelected = activeLetter == char
                Text(
                    text = char.toString(),
                    color = if (isSelected) MotoCyanAccent else TextTertiary,
                    fontSize = if (isSelected) 11.sp else 9.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                )
            }
        }
    }
}
