package com.motorola.launcher.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.theme.MotoCyanAccent
import com.motorola.launcher.ui.theme.MotoDarkSurface
import com.motorola.launcher.ui.theme.MotoGlassBorder
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextTertiary

/**
 * Low-latency search input bar directly querying Room SQLite FTS5 table.
 */
@Composable
fun SearchBarOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(MotoDarkSurface)
            .border(1.dp, MotoGlassBorder, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cyan Search Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MotoCyanAccent)
            )

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(MotoCyanAccent),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps and shortcuts…",
                            color = TextTertiary,
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            )

            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClearQuery),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = TextTertiary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
