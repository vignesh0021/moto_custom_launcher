package com.motorola.launcher.ui.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.motorola.launcher.ui.model.WallpaperPreset
import java.util.Random

/**
 * Full-screen background layer drawn behind the home workspace.
 *
 * - [WallpaperPreset.SYSTEM]: stays transparent so the user's Android wallpaper shows through,
 *   but adds a top/bottom scrim and a soft vignette behind the reactor so HUD text stays readable
 *   on bright photos.
 * - Other presets: static, procedurally drawn backgrounds (no animation → zero idle battery cost).
 */
@Composable
fun StarkWallpaperLayer(
    preset: WallpaperPreset,
    accent: Color,
    modifier: Modifier = Modifier
) {
    // Deterministic star field, generated once.
    val stars = remember {
        val rnd = Random(0x57A2C)
        List(140) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()) }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (preset == WallpaperPreset.SYSTEM) {
            drawReadabilityScrim()
            return@Canvas
        }

        drawRect(Brush.verticalGradient(listOf(preset.top, preset.bottom)))

        when (preset) {
            WallpaperPreset.EARTH -> {
                drawStars(stars, density = 0.6f)
                drawEarthLimb(accent)
            }
            WallpaperPreset.CYBER_GRID -> {
                drawStars(stars, density = 0.25f)
                drawPerspectiveGrid(accent)
            }
            WallpaperPreset.DEEP_SPACE -> {
                drawNebula(accent)
                drawStars(stars, density = 1.0f)
            }
            WallpaperPreset.STARK_TOWER -> {
                drawStars(stars, density = 0.4f)
                drawSkyline(accent)
            }
            WallpaperPreset.SYSTEM -> Unit
        }

        // Subtle vignette keeps edges calm and the centre focused.
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x99000000)),
                center = center,
                radius = size.maxDimension * 0.75f
            )
        )
    }
}

private fun DrawScope.drawReadabilityScrim() {
    drawRect(
        Brush.verticalGradient(
            0.00f to Color(0xB3000000),
            0.25f to Color(0x4D000000),
            0.55f to Color(0x33000000),
            0.78f to Color(0x80000000),
            1.00f to Color(0xD9000000)
        )
    )
    val c = Offset(size.width / 2f, size.height * 0.47f)
    val r = size.width * 0.6f
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0x8C000000), Color.Transparent), center = c, radius = r),
        radius = r,
        center = c
    )
}

private fun DrawScope.drawStars(stars: List<FloatArray>, density: Float) {
    val count = (stars.size * density).toInt()
    for (i in 0 until count) {
        val s = stars[i]
        drawCircle(
            color = Color.White.copy(alpha = 0.15f + s[2] * 0.55f),
            radius = 0.6f + s[2] * 1.6f,
            center = Offset(s[0] * size.width, s[1] * size.height)
        )
    }
}

private fun DrawScope.drawEarthLimb(accent: Color) {
    val c = Offset(size.width / 2f, size.height * 1.18f)
    val r = size.width * 0.95f
    // Atmosphere glow
    drawCircle(
        brush = Brush.radialGradient(
            0.80f to Color.Transparent,
            0.93f to accent.copy(alpha = 0.35f),
            1.00f to Color.Transparent,
            center = c,
            radius = r * 1.1f
        ),
        radius = r * 1.1f,
        center = c
    )
    // Planet body
    drawCircle(
        brush = Brush.radialGradient(listOf(Color(0xFF06223A), Color(0xFF020A14)), center = c, radius = r),
        radius = r,
        center = c
    )
    // Holographic latitude rings
    for (i in 1..4) {
        drawCircle(
            color = accent.copy(alpha = 0.10f),
            radius = r * (1f - i * 0.12f),
            center = c,
            style = Stroke(width = 1f)
        )
    }
    drawCircle(color = accent.copy(alpha = 0.6f), radius = r, center = c, style = Stroke(width = 2f))
}

private fun DrawScope.drawPerspectiveGrid(accent: Color) {
    val horizon = size.height * 0.62f
    val vanish = Offset(size.width / 2f, horizon)
    val lineColor = accent.copy(alpha = 0.22f)

    // Horizon glow
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, accent.copy(alpha = 0.18f), Color.Transparent),
            startY = horizon - 120f,
            endY = horizon + 120f
        ),
        topLeft = Offset(0f, horizon - 120f),
        size = Size(size.width, 240f)
    )
    // Converging verticals
    val cols = 14
    for (i in -cols..cols) {
        val x = size.width / 2f + i * size.width / 6f
        drawLine(lineColor, vanish, Offset(x, size.height), strokeWidth = 1.2f)
    }
    // Receding horizontals (geometric spacing)
    var t = 0.02f
    while (t < 1f) {
        val y = horizon + (size.height - horizon) * t
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.2f)
        t *= 1.55f
    }
}

private fun DrawScope.drawNebula(accent: Color) {
    val blobs = listOf(
        Triple(Offset(size.width * 0.2f, size.height * 0.3f), size.width * 0.7f, Color(0x55B388FF)),
        Triple(Offset(size.width * 0.85f, size.height * 0.65f), size.width * 0.6f, accent.copy(alpha = 0.22f)),
        Triple(Offset(size.width * 0.5f, size.height * 0.9f), size.width * 0.5f, Color(0x33FF3366))
    )
    for ((c, r, color) in blobs) {
        drawCircle(
            brush = Brush.radialGradient(listOf(color, Color.Transparent), center = c, radius = r),
            radius = r,
            center = c
        )
    }
}

private fun DrawScope.drawSkyline(accent: Color) {
    val rnd = Random(42)
    val base = size.height
    val bodyColor = Color(0xFF02080C)
    var x = 0f
    while (x < size.width) {
        val w = size.width * (0.06f + rnd.nextFloat() * 0.08f)
        val h = size.height * (0.10f + rnd.nextFloat() * 0.18f)
        drawRect(bodyColor, topLeft = Offset(x, base - h), size = Size(w, h))
        // Lit windows
        var wy = base - h + 12f
        while (wy < base - 8f) {
            var wx = x + 6f
            while (wx < x + w - 6f) {
                if (rnd.nextFloat() > 0.72f) {
                    drawRect(accent.copy(alpha = 0.35f), topLeft = Offset(wx, wy), size = Size(4f, 6f))
                }
                wx += 12f
            }
            wy += 16f
        }
        x += w + 4f
    }
    // Central tower
    val tw = size.width * 0.12f
    val th = size.height * 0.42f
    val tx = size.width / 2f - tw / 2f
    drawRect(bodyColor, topLeft = Offset(tx, base - th), size = Size(tw, th))
    drawLine(accent.copy(alpha = 0.7f), Offset(tx, base - th), Offset(tx, base), strokeWidth = 2f)
    drawLine(accent.copy(alpha = 0.7f), Offset(tx + tw, base - th), Offset(tx + tw, base), strokeWidth = 2f)
    drawCircle(accent, radius = 5f, center = Offset(size.width / 2f, base - th - 14f))
    drawCircle(
        brush = Brush.radialGradient(
            listOf(accent.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(size.width / 2f, base - th - 14f),
            radius = 60f
        ),
        radius = 60f,
        center = Offset(size.width / 2f, base - th - 14f)
    )
}
