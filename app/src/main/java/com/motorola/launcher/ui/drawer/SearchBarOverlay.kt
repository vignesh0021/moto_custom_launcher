package com.motorola.launcher.ui.drawer

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoElectricViolet
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary
import com.motorola.launcher.ui.theme.TextTertiary

/**
 * Cyber-luminous Frosted Glass Search Bar with real-time app telemetry badge.
 */
@Composable
fun SearchBarOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    appCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x331E293B),
                        Color(0x260F172A)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MotoCyanAccent.copy(alpha = 0.5f),
                        MotoElectricViolet.copy(alpha = 0.35f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing Cyber Search Lens Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MotoCyanAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌕",
                    color = MotoCyanAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(MotoCyanAccent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps and shortcuts...",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearQuery
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (query.isEmpty() && appCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$appCount",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
