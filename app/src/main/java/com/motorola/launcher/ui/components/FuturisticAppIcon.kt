package com.motorola.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motorola.launcher.ui.model.AppItem
import com.motorola.launcher.ui.theme.LocalMarvelTheme
import com.motorola.launcher.ui.theme.TextPrimary
import com.motorola.launcher.ui.theme.TextSecondary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FuturisticAppIcon(
    badgeCount: Int = 0,
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    iconSize: Dp = 46.dp,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val marvelTheme = LocalMarvelTheme.current
    val density = LocalDensity.current
    val px = with(density) { iconSize.toPx() }

    val bitmap = remember(app.iconBitmap, app.packageName, px) {
        app.iconBitmap?.asAndroidBitmap() ?: renderIcon(app, px.toInt())
    }

    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    // Subtle global rotation / perspective for depth
                    rotationY = 4f
                    rotationX = -2f
                    cameraDistance = 12f * density.density
                }
        ) {
            Canvas(modifier = Modifier.size(iconSize)) {
                val shape = chamferedShape(cutSize = 8.dp.toPx())
                // Glass background
                drawRoundRect(
                    color = marvelTheme.colors.glassSurface,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
                // Border
                drawRoundRect(
                    color = marvelTheme.colors.hudBorder,
                    topLeft = Offset.Zero,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )
                // Icon bitmap
                bitmap?.let { bmp ->
                    val padding = 6.dp.toPx()
                    val maxSize = (size.width - padding * 2).coerceAtLeast(1f)
                    val scale = minOf(maxSize / bmp.width, maxSize / bmp.height)
                    val drawW = (bmp.width * scale).toInt().coerceAtLeast(1)
                    val drawH = (bmp.height * scale).toInt().coerceAtLeast(1)
                    val offsetX = ((size.width - drawW) / 2f).toInt()
                    val offsetY = ((size.height - drawH) / 2f).toInt()
                    drawImage(
                        image = bmp.asImageBitmap(),
                        dstOffset = androidx.compose.ui.unit.IntOffset(offsetX, offsetY),
                        dstSize = androidx.compose.ui.unit.IntSize(drawW, drawH)
                    )
                }
                // Glow ring on top edge
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(marvelTheme.colors.glow, Color.Transparent, marvelTheme.colors.glow)
                    ),
                    start = Offset(4.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 4.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .background(Color.Red, CircleShape)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun renderIcon(app: AppItem, sizePx: Int): Bitmap? {
    return try {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = android.graphics.Color.WHITE
            textSize = sizePx * 0.45f
        }
        val rect = RectF(2f, 2f, sizePx - 2f, sizePx - 2f)
        canvas.drawRoundRect(rect, 12f, 12f, paint.apply {
            color = android.graphics.Color.argb(40, 0, 240, 255)
            style = Paint.Style.FILL
        })
        val initial = app.label.firstOrNull()?.uppercaseChar() ?: '?'
        canvas.drawText(initial.toString(), sizePx / 2f, sizePx / 2f + sizePx * 0.15f, paint.apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        })
        bmp
    } catch (_: Exception) {
        null
    }

}
