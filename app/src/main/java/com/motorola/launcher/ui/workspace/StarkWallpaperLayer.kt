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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.WallpaperPreset
import com.motorola.launcher.ui.theme.LocalCustomization
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

    val customization = LocalCustomization.current
    val hero = customization.heroProtocol

    Canvas(modifier = modifier.fillMaxSize()) {
        if (preset == WallpaperPreset.SYSTEM) {
            drawReadabilityScrim()
            return@Canvas
        }

        drawRect(Brush.verticalGradient(listOf(preset.top, preset.bottom)))

        when (preset) {
            WallpaperPreset.MARVEL_HERO -> {
                when (hero) {
                    MarvelHeroProtocol.IRON_MAN -> drawStarkArcWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.BLACK_PANTHER -> drawWakandaWeaveWallpaper(hero.primary)
                    MarvelHeroProtocol.CAPTAIN_AMERICA -> drawStarShieldWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.THOR -> drawBifrostAuroraWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.HULK -> drawGammaBiosphereWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.DOCTOR_STRANGE -> drawSanctumMandalaWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.SPIDER_MAN -> drawSpiderMatrixWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.BLACK_WIDOW -> drawWidowTacticalWallpaper(hero.primary, hero.secondary)
                    MarvelHeroProtocol.AVENGERS_UNIFIED -> drawAvengersUnifiedWallpaper(hero.primary, hero.secondary)
                }
            }
            WallpaperPreset.AVENGERS_TOWER -> {
                drawStars(stars, density = 0.4f)
                drawSkyline(accent)
            }
            WallpaperPreset.WAKANDA -> {
                drawWakandaWeaveWallpaper(Color(0xFFB388FF))
            }
            WallpaperPreset.BIFROST -> {
                drawStars(stars, density = 0.5f)
                drawBifrostAuroraWallpaper(Color(0xFFFFD700), Color(0xFF00E5FF))
            }
            WallpaperPreset.GAMMA_CORE -> {
                drawGammaBiosphereWallpaper(Color(0xFF16A34A), Color(0xFF9333EA))
            }
            WallpaperPreset.SPIDER_MATRIX -> {
                drawSpiderMatrixWallpaper(Color(0xFFFF1744), Color(0xFF00B0FF))
            }
            WallpaperPreset.SANCTUM -> {
                drawSanctumMandalaWallpaper(Color(0xFFFF6D00), Color(0xFF00E676))
            }
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

/** Iron Man: Arc reactor blueprint concentric circles & high-tech HUD grid */
private fun DrawScope.drawStarkArcWallpaper(primary: Color, secondary: Color) {
    val c = Offset(size.width / 2f, size.height * 0.46f)
    val maxR = size.width * 0.75f

    // Concentric blueprint circles
    for (i in 1..6) {
        val r = maxR * (i / 6f)
        drawCircle(
            color = primary.copy(alpha = 0.08f + i * 0.02f),
            radius = r,
            center = c,
            style = Stroke(
                width = 1.2f,
                pathEffect = if (i % 2 == 0) PathEffect.dashPathEffect(floatArrayOf(12f, 8f)) else null
            )
        )
    }

    // Radial targeting axes
    for (angleDeg in listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)) {
        val rad = angleDeg * (PI.toFloat() / 180f)
        val end = Offset(c.x + cos(rad) * maxR, c.y + sin(rad) * maxR)
        drawLine(
            color = primary.copy(alpha = 0.12f),
            start = c,
            end = end,
            strokeWidth = 1f
        )
    }

    // Secondary hot-rod crimson ambient glow in the lower half
    drawCircle(
        brush = Brush.radialGradient(
            listOf(secondary.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width / 2f, size.height * 0.85f),
            radius = size.width * 0.8f
        ),
        radius = size.width * 0.8f,
        center = Offset(size.width / 2f, size.height * 0.85f)
    )
}

/** Black Panther: Vibranium kinetic purple weave with tribal chevron pattern */
private fun DrawScope.drawWakandaWeaveWallpaper(accent: Color) {
    val c = Offset(size.width / 2f, size.height * 0.46f)
    val r = size.width * 0.65f

    // Vibranium kinetic energy waves
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.28f), Color(0x22170928), Color.Transparent),
            center = c,
            radius = r * 1.2f
        ),
        radius = r * 1.2f,
        center = c
    )

    // Geometric tribal diamond lattice
    val step = 42f
    val lineColor = accent.copy(alpha = 0.14f)
    var y = 0f
    while (y < size.height) {
        drawLine(lineColor, Offset(0f, y), Offset(size.width, y + size.width * 0.5f), strokeWidth = 1f)
        drawLine(lineColor, Offset(0f, y + size.width * 0.5f), Offset(size.width, y), strokeWidth = 1f)
        y += step * 2.5f
    }

    // Tribal necklace tooth ring
    val toothCount = 12
    for (i in 0 until toothCount) {
        val angle = (i * 360f / toothCount) * (PI.toFloat() / 180f)
        val p1 = Offset(c.x + cos(angle) * (r * 0.7f), c.y + sin(angle) * (r * 0.7f))
        val p2 = Offset(c.x + cos(angle) * (r * 0.82f), c.y + sin(angle) * (r * 0.82f))
        drawLine(accent.copy(alpha = 0.4f), p1, p2, strokeWidth = 2.5f)
    }
}

/** Captain America: Star Shield concentric rings and tactical navy-red glow */
private fun DrawScope.drawStarShieldWallpaper(primary: Color, secondary: Color) {
    val c = Offset(size.width / 2f, size.height * 0.46f)
    val maxR = size.width * 0.7f

    // Outer Red Ring
    drawCircle(secondary.copy(alpha = 0.16f), radius = maxR, center = c, style = Stroke(width = 24f))
    // Silver Gap
    drawCircle(Color.White.copy(alpha = 0.12f), radius = maxR * 0.82f, center = c, style = Stroke(width = 16f))
    // Inner Red Ring
    drawCircle(secondary.copy(alpha = 0.18f), radius = maxR * 0.65f, center = c, style = Stroke(width = 20f))
    // Blue Core
    drawCircle(
        brush = Brush.radialGradient(
            listOf(primary.copy(alpha = 0.35f), primary.copy(alpha = 0.15f)),
            center = c,
            radius = maxR * 0.48f
        ),
        radius = maxR * 0.48f,
        center = c
    )

    // Star watermark path
    val starPath = Path()
    val starR = maxR * 0.36f
    val innerStarR = starR * 0.38f
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) starR else innerStarR
        val angle = (i * 36f - 90f) * (PI.toFloat() / 180f)
        val x = c.x + cos(angle) * radius
        val y = c.y + sin(angle) * radius
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()
    drawPath(starPath, color = Color.White.copy(alpha = 0.18f))
}

/** Thor: Bifrost cosmic star-bridge with celestial gold and cyan auroras */
private fun DrawScope.drawBifrostAuroraWallpaper(gold: Color, cyan: Color) {
    // Vertical Bifrost Light Beam
    val beamWidth = size.width * 0.38f
    val left = size.width / 2f - beamWidth / 2f
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                cyan.copy(alpha = 0.22f),
                gold.copy(alpha = 0.30f),
                cyan.copy(alpha = 0.22f),
                Color.Transparent
            ),
            startX = left,
            endX = left + beamWidth
        ),
        topLeft = Offset(left, 0f),
        size = Size(beamWidth, size.height)
    )

    // Lightning diagonal crackles
    val lightningColor = gold.copy(alpha = 0.25f)
    val path = Path()
    path.moveTo(size.width * 0.35f, 0f)
    path.lineTo(size.width * 0.42f, size.height * 0.25f)
    path.lineTo(size.width * 0.38f, size.height * 0.45f)
    path.lineTo(size.width * 0.52f, size.height * 0.70f)
    path.lineTo(size.width * 0.48f, size.height)
    drawPath(path, color = lightningColor, style = Stroke(width = 1.5f))

    // Asgardian celestial radial glow
    val c = Offset(size.width / 2f, size.height * 0.46f)
    drawCircle(
        brush = Brush.radialGradient(listOf(gold.copy(alpha = 0.22f), Color.Transparent), center = c, radius = size.width * 0.6f),
        radius = size.width * 0.6f,
        center = c
    )
}

/** Doctor Strange: Sanctum Sanctorum mystic Tao mandala spell circle */
private fun DrawScope.drawSanctumMandalaWallpaper(amber: Color, emerald: Color) {
    val c = Offset(size.width / 2f, size.height * 0.46f)
    val maxR = size.width * 0.65f

    // Outer spell circle
    drawCircle(amber.copy(alpha = 0.26f), radius = maxR, center = c, style = Stroke(width = 2f))
    drawCircle(amber.copy(alpha = 0.18f), radius = maxR * 0.92f, center = c, style = Stroke(width = 1f))

    // Octagram / 8-pointed mystic star
    val path = Path()
    for (i in 0 until 8) {
        val a1 = (i * 45f) * (PI.toFloat() / 180f)
        val a2 = ((i * 45f) + 135f) * (PI.toFloat() / 180f)
        drawLine(
            amber.copy(alpha = 0.20f),
            Offset(c.x + cos(a1) * maxR * 0.9f, c.y + sin(a1) * maxR * 0.9f),
            Offset(c.x + cos(a2) * maxR * 0.9f, c.y + sin(a2) * maxR * 0.9f),
            strokeWidth = 1.2f
        )
    }

    // Time stone emerald core
    drawCircle(
        brush = Brush.radialGradient(listOf(emerald.copy(alpha = 0.28f), Color.Transparent), center = c, radius = maxR * 0.45f),
        radius = maxR * 0.45f,
        center = c
    )
}

/** Spider-Man: 8-axis web matrix targeting grid with Iron Spider crimson & blue */
private fun DrawScope.drawSpiderMatrixWallpaper(primary: Color, secondary: Color) {
    val c = Offset(size.width / 2f, size.height * 0.46f)
    val maxR = size.width * 0.72f

    // 8-axis radial spokes
    val axes = 8
    for (i in 0 until axes) {
        val rad = (i * 360f / axes) * (PI.toFloat() / 180f)
        drawLine(
            primary.copy(alpha = 0.22f),
            c,
            Offset(c.x + cos(rad) * maxR, c.y + sin(rad) * maxR),
            strokeWidth = 1.2f
        )
    }

    // Concentric web polygons
    val rings = 5
    for (rIdx in 1..rings) {
        val ringR = maxR * (rIdx / rings.toFloat())
        val webPath = Path()
        for (i in 0 until axes) {
            val rad = (i * 360f / axes) * (PI.toFloat() / 180f)
            val pt = Offset(c.x + cos(rad) * ringR, c.y + sin(rad) * ringR)
            if (i == 0) webPath.moveTo(pt.x, pt.y) else webPath.lineTo(pt.x, pt.y)
        }
        webPath.close()
        drawPath(
            webPath,
            color = if (rIdx % 2 == 0) secondary.copy(alpha = 0.18f) else primary.copy(alpha = 0.18f),
            style = Stroke(width = 1.2f)
        )
    }

    // Central nano spider ambient glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.25f), secondary.copy(alpha = 0.10f), Color.Transparent),
            center = c,
            radius = maxR * 0.5f
        ),
        radius = maxR * 0.5f,
        center = c
    )
}

/** Hulk: Gamma radiation cellular bio-matrix with kinetic shockwave rings */
private fun DrawScope.drawGammaBiosphereWallpaper(primary: Color, secondary: Color) {
    val cx = size.width / 2f
    val cy = size.height * 0.44f
    val maxDim = size.maxDimension

    // Heavy gamma radiation hexagonal matrix
    val hexSize = 52f
    val cols = (size.width / hexSize).toInt() + 2
    val rows = (size.height / (hexSize * 0.866f)).toInt() + 2
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val hx = c * hexSize + (if (r % 2 == 1) hexSize / 2f else 0f)
            val hy = r * hexSize * 0.866f
            drawCircle(
                color = primary.copy(alpha = 0.06f),
                radius = 2.5f,
                center = Offset(hx, hy)
            )
        }
    }

    // Expanding Gamma Bio-Shockwave Rings
    listOf(0.20f, 0.35f, 0.50f, 0.68f).forEach { scale ->
        val ringR = maxDim * scale
        drawCircle(
            color = primary.copy(alpha = 0.10f),
            radius = ringR,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f), 0f))
        )
    }

    // Heavy Gamma Confinement Tri-blades
    for (i in 0 until 3) {
        val angleDeg = i * 120f - 90f
        val rad = (angleDeg * PI / 180f).toFloat()
        val startR = maxDim * 0.16f
        val endR = maxDim * 0.44f
        drawLine(
            color = secondary.copy(alpha = 0.14f),
            start = Offset(cx + startR * cos(rad), cy + startR * sin(rad)),
            end = Offset(cx + endR * cos(rad), cy + endR * sin(rad)),
            strokeWidth = 2.5f
        )
    }

    // Central radioactive green aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.22f), secondary.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxDim * 0.45f
        ),
        radius = maxDim * 0.45f,
        center = Offset(cx, cy)
    )
}

/** Black Widow: Covert tactical stealth grid with subtle precision reticles */
private fun DrawScope.drawWidowTacticalWallpaper(primary: Color, secondary: Color) {
    val cx = size.width / 2f
    val cy = size.height * 0.40f
    val maxDim = maxOf(size.width, size.height)

    // Deep tactical grid
    val step = 42f
    val lineCol = primary.copy(alpha = 0.04f)
    var x = 0f
    while (x <= size.width) {
        drawLine(lineCol, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y <= size.height) {
        drawLine(lineCol, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }

    // Precision Hourglass watermark behind center
    val hgR = maxDim * 0.24f
    val path = Path().apply {
        moveTo(cx - hgR, cy - hgR * 1.3f)
        lineTo(cx + hgR, cy - hgR * 1.3f)
        lineTo(cx, cy)
        lineTo(cx + hgR, cy + hgR * 1.3f)
        lineTo(cx - hgR, cy + hgR * 1.3f)
        lineTo(cx, cy)
        close()
    }
    drawPath(path, color = primary.copy(alpha = 0.05f))
    drawPath(path, color = primary.copy(alpha = 0.12f), style = Stroke(width = 1.5f))

    // Crimson tactical vignette
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.16f), secondary.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxDim * 0.42f
        ),
        radius = maxDim * 0.42f,
        center = Offset(cx, cy)
    )
}

/** Avengers Unified: Minimalist, clean multi-hero cosmic grid with subtle stylized chevron */
private fun DrawScope.drawAvengersUnifiedWallpaper(primary: Color, secondary: Color) {
    val cx = size.width / 2f
    val cy = size.height * 0.40f
    val maxDim = maxOf(size.width, size.height)

    // Clean subtle radial hero grid
    val spokes = 12
    for (i in 0 until spokes) {
        val aRad = (i * (360f / spokes) * PI / 180f).toFloat()
        drawLine(
            color = primary.copy(alpha = 0.04f),
            start = Offset(cx, cy),
            end = Offset(cx + maxDim * cos(aRad), cy + maxDim * sin(aRad)),
            strokeWidth = 1f
        )
    }

    // Concentric hero orbital rings
    listOf(0.18f, 0.32f, 0.48f, 0.65f).forEachIndexed { idx, scale ->
        val ringCol = if (idx % 2 == 0) primary.copy(alpha = 0.08f) else secondary.copy(alpha = 0.05f)
        drawCircle(
            color = ringCol,
            radius = maxDim * scale,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f))
        )
    }

    // Subtle radiant hero aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.18f), secondary.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(cx, cy),
            radius = maxDim * 0.48f
        ),
        radius = maxDim * 0.48f,
        center = Offset(cx, cy)
    )
}

