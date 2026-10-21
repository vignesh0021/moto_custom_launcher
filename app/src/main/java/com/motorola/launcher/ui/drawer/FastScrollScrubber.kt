package com.motorola.launcher.ui.drawer

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.TextTertiary

private val ALPHABET = ('A'..'Z').toList()

/**
 * High-speed vertical alphabetical fast-scroll indexer tuned for curved edge borders.
 */
@Composable
fun FastScrollScrubber(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier
            .padding(end = 6.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    val totalHeight = size.height
                    val itemHeight = totalHeight / ALPHABET.size.toFloat()
                    val touchY = change.position.y.coerceIn(0f, totalHeight - 1)
                    val letterIndex = (touchY / itemHeight).toInt().coerceIn(0, ALPHABET.size - 1)
                    val letter = ALPHABET[letterIndex]

                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onLetterSelected(letter)
                }
            },
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (char in ALPHABET) {
            Text(
                text = char.toString(),
                color = TextTertiary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
