package com.motorola.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.StarkCyan
import com.motorola.launcher.ui.theme.StarkCyanBright
import com.motorola.launcher.ui.theme.StarkDarkSurface
import com.motorola.launcher.ui.theme.StarkHudBorder
import com.motorola.launcher.ui.theme.StarkHudBorderBright
import com.motorola.launcher.ui.theme.TextSecondary

/**
 * Chamfered 45-degree tactical corner shape for futuristic HUD panels.
 */
fun chamferedShape(cutSize: Float = 16f) = GenericShape { size, _ ->
    moveTo(cutSize, 0f)
    lineTo(size.width - cutSize, 0f)
    lineTo(size.width, cutSize)
    lineTo(size.width, size.height - cutSize)
    lineTo(size.width - cutSize, size.height)
    lineTo(cutSize, size.height)
    lineTo(0f, size.height - cutSize)
    lineTo(0f, cutSize)
    close()
}

/**
 * Holographic HUD Glass Panel with cybernetic corner brackets and technical title bars.
 */
@Composable
fun HudPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    statusIndicator: String? = "ONLINE",
    statusColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val marvelTheme = com.motorola.launcher.ui.theme.LocalMarvelTheme.current
    val effectiveStatusColor = if (statusColor != Color.Unspecified) statusColor else marvelTheme.colors.primary
    val effectiveBorderColor = if (borderColor != Color.Unspecified) borderColor else marvelTheme.colors.hudBorder
    val effectiveBracketColor = marvelTheme.colors.hudBorderBright

    val shape = chamferedShape(cutSize = 24f)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x330B1426),
                        Color(0x59040812)
                    )
                )
            )
            .border(1.dp, effectiveBorderColor, shape)
    ) {
        // Technical Corner Reticle Brackets (Canvas overlay)
        Canvas(modifier = Modifier.matchParentSize()) {
            val bracketLen = 12.dp.toPx()
            val bracketStroke = 2.dp.toPx()

            // Top-Left bracket
            drawLine(effectiveBracketColor, Offset(0f, 0f), Offset(bracketLen, 0f), bracketStroke)
            drawLine(effectiveBracketColor, Offset(0f, 0f), Offset(0f, bracketLen), bracketStroke)

            // Top-Right bracket
            drawLine(effectiveBracketColor, Offset(size.width - bracketLen, 0f), Offset(size.width, 0f), bracketStroke)
            drawLine(effectiveBracketColor, Offset(size.width, 0f), Offset(size.width, bracketLen), bracketStroke)

            // Bottom-Left bracket
            drawLine(effectiveBracketColor, Offset(0f, size.height - bracketLen), Offset(0f, size.height), bracketStroke)
            drawLine(effectiveBracketColor, Offset(0f, size.height), Offset(bracketLen, size.height), bracketStroke)

            // Bottom-Right bracket
            drawLine(effectiveBracketColor, Offset(size.width - bracketLen, size.height), Offset(size.width, size.height), bracketStroke)
            drawLine(effectiveBracketColor, Offset(size.width, size.height - bracketLen), Offset(size.width, size.height), bracketStroke)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (title != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        color = effectiveStatusColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (statusIndicator != null) {
                        Text(
                            text = "● $statusIndicator",
                            color = effectiveStatusColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.0.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}
