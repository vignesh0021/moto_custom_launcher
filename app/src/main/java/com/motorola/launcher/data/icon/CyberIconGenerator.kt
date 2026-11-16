package com.motorola.launcher.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Procedural Vector Cyber HUD Icon Generator for Moto Marvel Launcher.
 * Generates razor-sharp, zero-allocation 192x192 holographic app icons for built-in themes.
 */
object CyberIconGenerator {

    const val BUILTIN_STARK_QUANTUM = "com.motorola.launcher.iconpack.stark_quantum"
    const val BUILTIN_JARVIS_TACTICAL = "com.motorola.launcher.iconpack.jarvis_tactical"
    const val BUILTIN_VIBRANIUM_STEALTH = "com.motorola.launcher.iconpack.vibranium_stealth"
    const val BUILTIN_ARC_REACTOR = "com.motorola.launcher.iconpack.arc_reactor"
    const val BUILTIN_CUSTOM_SDCARD = "com.motorola.launcher.iconpack.custom_sdcard"

    data class IconThemePalette(
        val primaryColor: Int,
        val secondaryColor: Int,
        val bgTopColor: Int,
        val bgBottomColor: Int,
        val gridColor: Int
    )

    fun getPalette(packType: String): IconThemePalette {
        return when (packType) {
            BUILTIN_JARVIS_TACTICAL -> IconThemePalette(
                primaryColor = 0xFF00B0FF.toInt(),     // Tactical Azure
                secondaryColor = 0xFFFF9100.toInt(),   // Amber
                bgTopColor = 0xFF071424.toInt(),
                bgBottomColor = 0xFF02070D.toInt(),
                gridColor = 0x2200B0FF.toInt()
            )
            BUILTIN_VIBRANIUM_STEALTH -> IconThemePalette(
                primaryColor = 0xFFBB86FC.toInt(),     // Amethyst
                secondaryColor = 0xFFECEFF1.toInt(),   // Platinum
                bgTopColor = 0xFF140D22.toInt(),
                bgBottomColor = 0xFF07040B.toInt(),
                gridColor = 0x22BB86FC.toInt()
            )
            BUILTIN_ARC_REACTOR -> IconThemePalette(
                primaryColor = 0xFFFF1744.toInt(),     // Scarlet
                secondaryColor = 0xFFFFEA00.toInt(),   // Solar Gold
                bgTopColor = 0xFF1C070A.toInt(),
                bgBottomColor = 0xFF080203.toInt(),
                gridColor = 0x22FF1744.toInt()
            )
            BUILTIN_CUSTOM_SDCARD -> IconThemePalette(
                primaryColor = 0xFF00E5FF.toInt(),     // Cyan
                secondaryColor = 0xFF76FF03.toInt(),   // Neon Lime
                bgTopColor = 0xFF0B1B2B.toInt(),
                bgBottomColor = 0xFF03070C.toInt(),
                gridColor = 0x2200E5FF.toInt()
            )
            else -> IconThemePalette( // BUILTIN_STARK_QUANTUM (Default)
                primaryColor = 0xFF00E5FF.toInt(),     // Stark Cyan
                secondaryColor = 0xFFFFD700.toInt(),   // Arc Gold
                bgTopColor = 0xFF0A192B.toInt(),
                bgBottomColor = 0xFF030810.toInt(),
                gridColor = 0x2200E5FF.toInt()
            )
        }
    }

    enum class AppCategory {
        PHONE, MESSAGE, CAMERA, BROWSER, GALLERY, SETTINGS, CLOCK, CALENDAR,
        CALCULATOR, CONTACTS, MAPS, YOUTUBE, MUSIC, FILES, STORE, CHAT, GENERIC
    }

    fun detectCategory(packageName: String, activityName: String): AppCategory {
        val lowerPkg = packageName.lowercase()
        val lowerAct = activityName.lowercase()
        val combined = "$lowerPkg/$lowerAct"

        return when {
            combined.contains("dialer") || combined.contains("phone") || combined.contains("telecom") -> AppCategory.PHONE
            combined.contains("messaging") || combined.contains("mms") || combined.contains("sms") -> AppCategory.MESSAGE
            combined.contains("camera") -> AppCategory.CAMERA
            combined.contains("chrome") || combined.contains("browser") || combined.contains("firefox") || combined.contains("opera") -> AppCategory.BROWSER
            combined.contains("gallery") || combined.contains("photo") -> AppCategory.GALLERY
            combined.contains("setting") -> AppCategory.SETTINGS
            combined.contains("clock") || combined.contains("alarm") -> AppCategory.CLOCK
            combined.contains("calendar") -> AppCategory.CALENDAR
            combined.contains("calc") -> AppCategory.CALCULATOR
            combined.contains("contact") || combined.contains("people") -> AppCategory.CONTACTS
            combined.contains("map") || combined.contains("navigation") || combined.contains("waze") -> AppCategory.MAPS
            combined.contains("youtube") || combined.contains("video") || combined.contains("vlc") -> AppCategory.YOUTUBE
            combined.contains("music") || combined.contains("audio") || combined.contains("spotify") || combined.contains("sound") -> AppCategory.MUSIC
            combined.contains("file") || combined.contains("document") || combined.contains("explorer") -> AppCategory.FILES
            combined.contains("vending") || combined.contains("store") || combined.contains("market") -> AppCategory.STORE
            combined.contains("whatsapp") || combined.contains("telegram") || combined.contains("signal") -> AppCategory.CHAT
            else -> AppCategory.GENERIC
        }
    }

    /**
     * Synthesizes a high-resolution 192x192 vector cybernetic icon.
     */
    fun generateIcon(
        context: Context,
        packageName: String,
        activityName: String,
        packType: String,
        fallbackDrawable: Drawable? = null
    ): Drawable {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = getPalette(packType)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // 1. Draw Chamfered HUD Outer Plate
        val plateRect = RectF(12f, 12f, size - 12f, size - 12f)
        val cornerRadius = 36f

        fillPaint.shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            palette.bgTopColor, palette.bgBottomColor, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(plateRect, cornerRadius, cornerRadius, fillPaint)
        fillPaint.shader = null

        // 2. Draw Subtle Interior Cyber Mesh Grid Lines
        strokePaint.color = palette.gridColor
        strokePaint.strokeWidth = 1f
        canvas.drawLine(24f, 48f, size - 24f, 48f, strokePaint)
        canvas.drawLine(24f, size - 48f, size - 24f, size - 48f, strokePaint)
        canvas.drawLine(48f, 24f, 48f, size - 24f, strokePaint)
        canvas.drawLine(size - 48f, 24f, size - 48f, size - 24f, strokePaint)

        // 3. Draw Neon Outer Border
        strokePaint.color = palette.primaryColor
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(plateRect, cornerRadius, cornerRadius, strokePaint)

        // 4. Draw Precision Corner Tech Notches
        strokePaint.color = palette.secondaryColor
        strokePaint.strokeWidth = 3.5f
        // Top-Left notch
        canvas.drawLine(16f, 32f, 16f, 22f, strokePaint)
        canvas.drawLine(16f, 22f, 26f, 16f, strokePaint)
        canvas.drawLine(26f, 16f, 36f, 16f, strokePaint)
        // Bottom-Right notch
        canvas.drawLine(size - 36f, size - 16f, size - 26f, size - 16f, strokePaint)
        canvas.drawLine(size - 26f, size - 16f, size - 16f, size - 26f, strokePaint)
        canvas.drawLine(size - 16f, size - 26f, size - 16f, size - 36f, strokePaint)

        // Top-Right & Bottom-Left Accent Nodes
        fillPaint.color = palette.secondaryColor
        canvas.drawCircle(size - 22f, 22f, 2.5f, fillPaint)
        canvas.drawCircle(22f, size - 22f, 2.5f, fillPaint)

        // 5. Draw Glyphs
        val category = detectCategory(packageName, activityName)
        val cx = size / 2f
        val cy = size / 2f

        strokePaint.color = palette.primaryColor
        strokePaint.strokeWidth = 4f

        when (category) {
            AppCategory.PHONE -> {
                val path = Path().apply {
                    moveTo(cx - 24f, cy + 12f)
                    cubicTo(cx - 28f, cy - 8f, cx - 8f, cy - 28f, cx + 12f, cy - 24f)
                    lineTo(cx + 20f, cy - 16f)
                    lineTo(cx + 12f, cy - 8f)
                    lineTo(cx + 4f, cy - 12f)
                    cubicTo(cx - 8f, cy - 4f, cx - 4f, cy + 8f, cx + 12f, cy + 4f)
                    lineTo(cx + 8f, cy + 12f)
                    lineTo(cx + 16f, cy + 20f)
                    close()
                }
                canvas.drawPath(path, strokePaint)
                canvas.drawArc(RectF(cx - 36f, cy - 36f, cx + 36f, cy + 36f), -45f, 50f, false, strokePaint.apply { strokeWidth = 2.5f })
            }
            AppCategory.MESSAGE -> {
                val bubbleRect = RectF(cx - 28f, cy - 22f, cx + 28f, cy + 16f)
                canvas.drawRoundRect(bubbleRect, 10f, 10f, strokePaint)
                val tail = Path().apply {
                    moveTo(cx - 16f, cy + 16f)
                    lineTo(cx - 24f, cy + 28f)
                    lineTo(cx - 4f, cy + 16f)
                    close()
                }
                canvas.drawPath(tail, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx - 14f, cy - 3f, 3.5f, fillPaint)
                canvas.drawCircle(cx, cy - 3f, 3.5f, fillPaint)
                canvas.drawCircle(cx + 14f, cy - 3f, 3.5f, fillPaint)
            }
            AppCategory.CAMERA -> {
                val body = RectF(cx - 32f, cy - 16f, cx + 32f, cy + 26f)
                canvas.drawRoundRect(body, 8f, 8f, strokePaint)
                val topBump = Path().apply {
                    moveTo(cx - 16f, cy - 16f)
                    lineTo(cx - 10f, cy - 26f)
                    lineTo(cx + 10f, cy - 26f)
                    lineTo(cx + 16f, cy - 16f)
                }
                canvas.drawPath(topBump, strokePaint)
                canvas.drawCircle(cx, cy + 5f, 14f, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy + 5f, 5f, fillPaint)
                canvas.drawCircle(cx + 20f, cy - 6f, 2.5f, fillPaint)
            }
            AppCategory.BROWSER -> {
                canvas.drawCircle(cx, cy, 26f, strokePaint)
                canvas.drawOval(RectF(cx - 12f, cy - 26f, cx + 12f, cy + 26f), strokePaint.apply { strokeWidth = 2.5f })
                canvas.drawLine(cx - 26f, cy, cx + 26f, cy, strokePaint)
                canvas.drawLine(cx - 22f, cy - 12f, cx + 22f, cy - 12f, strokePaint)
                canvas.drawLine(cx - 22f, cy + 12f, cx + 22f, cy + 12f, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy, 4f, fillPaint)
            }
            AppCategory.GALLERY -> {
                val frame = RectF(cx - 28f, cy - 22f, cx + 28f, cy + 22f)
                canvas.drawRoundRect(frame, 6f, 6f, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx - 14f, cy - 8f, 5f, fillPaint)
                val mountain = Path().apply {
                    moveTo(cx - 24f, cy + 16f)
                    lineTo(cx - 6f, cy - 2f)
                    lineTo(cx + 8f, cy + 10f)
                    lineTo(cx + 16f, cy + 2f)
                    lineTo(cx + 24f, cy + 16f)
                }
                strokePaint.strokeWidth = 3f
                canvas.drawPath(mountain, strokePaint)
            }
            AppCategory.SETTINGS -> {
                canvas.drawCircle(cx, cy, 14f, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy, 6f, fillPaint)
                val teeth = 8
                strokePaint.strokeWidth = 4f
                for (i in 0 until teeth) {
                    val angle = (i * 360f / teeth) * (Math.PI / 180f)
                    val x1 = cx + (kotlin.math.cos(angle) * 16f).toFloat()
                    val y1 = cy + (kotlin.math.sin(angle) * 16f).toFloat()
                    val x2 = cx + (kotlin.math.cos(angle) * 26f).toFloat()
                    val y2 = cy + (kotlin.math.sin(angle) * 26f).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, strokePaint)
                }
            }
            AppCategory.CLOCK -> {
                canvas.drawCircle(cx, cy, 28f, strokePaint)
                canvas.drawLine(cx, cy, cx, cy - 18f, strokePaint.apply { strokeWidth = 3f })
                canvas.drawLine(cx, cy, cx + 14f, cy + 4f, strokePaint.apply { strokeWidth = 3f })
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy, 4f, fillPaint)
                canvas.drawCircle(cx, cy - 23f, 2f, fillPaint)
                canvas.drawCircle(cx + 23f, cy, 2f, fillPaint)
                canvas.drawCircle(cx, cy + 23f, 2f, fillPaint)
                canvas.drawCircle(cx - 23f, cy, 2f, fillPaint)
            }
            AppCategory.CALENDAR -> {
                val calRect = RectF(cx - 26f, cy - 20f, cx + 26f, cy + 26f)
                canvas.drawRoundRect(calRect, 6f, 6f, strokePaint)
                canvas.drawLine(cx - 26f, cy - 8f, cx + 26f, cy - 8f, strokePaint.apply { strokeWidth = 2.5f })
                // binder rings
                strokePaint.color = palette.secondaryColor
                canvas.drawLine(cx - 16f, cy - 26f, cx - 16f, cy - 18f, strokePaint.apply { strokeWidth = 3f })
                canvas.drawLine(cx + 16f, cy - 26f, cx + 16f, cy - 18f, strokePaint)
                // date matrix
                fillPaint.color = palette.primaryColor
                canvas.drawCircle(cx - 12f, cy, 3f, fillPaint)
                canvas.drawCircle(cx, cy, 3f, fillPaint)
                canvas.drawCircle(cx + 12f, cy, 3f, fillPaint)
                canvas.drawCircle(cx - 12f, cy + 12f, 3f, fillPaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy + 12f, 4f, fillPaint)
                canvas.drawCircle(cx + 12f, cy + 12f, 3f, fillPaint)
            }
            AppCategory.CALCULATOR -> {
                val box = RectF(cx - 24f, cy - 28f, cx + 24f, cy + 28f)
                canvas.drawRoundRect(box, 6f, 6f, strokePaint)
                canvas.drawLine(cx - 24f, cy - 12f, cx + 24f, cy - 12f, strokePaint.apply { strokeWidth = 2f })
                canvas.drawLine(cx, cy - 12f, cx, cy + 28f, strokePaint)
                canvas.drawLine(cx - 24f, cy + 8f, cx + 24f, cy + 8f, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx - 12f, cy - 2f, 2.5f, fillPaint)
                canvas.drawCircle(cx + 12f, cy - 2f, 2.5f, fillPaint)
                canvas.drawCircle(cx - 12f, cy + 18f, 2.5f, fillPaint)
                canvas.drawCircle(cx + 12f, cy + 18f, 2.5f, fillPaint)
            }
            AppCategory.MAPS -> {
                canvas.drawCircle(cx, cy, 26f, strokePaint)
                canvas.drawCircle(cx, cy, 14f, strokePaint.apply { strokeWidth = 2f })
                canvas.drawLine(cx - 28f, cy, cx + 28f, cy, strokePaint.apply { strokeWidth = 2f })
                canvas.drawLine(cx, cy - 28f, cx, cy + 28f, strokePaint.apply { strokeWidth = 2f })
                fillPaint.color = palette.secondaryColor
                val pin = Path().apply {
                    moveTo(cx, cy - 18f)
                    cubicTo(cx - 8f, cy - 18f, cx - 8f, cy - 4f, cx, cy + 4f)
                    cubicTo(cx + 8f, cy - 4f, cx + 8f, cy - 18f, cx, cy - 18f)
                    close()
                }
                canvas.drawPath(pin, fillPaint)
            }
            AppCategory.YOUTUBE -> {
                val frame = RectF(cx - 30f, cy - 20f, cx + 30f, cy + 20f)
                canvas.drawRoundRect(frame, 10f, 10f, strokePaint)
                fillPaint.color = palette.secondaryColor
                val play = Path().apply {
                    moveTo(cx - 8f, cy - 12f)
                    lineTo(cx + 12f, cy)
                    lineTo(cx - 8f, cy + 12f)
                    close()
                }
                canvas.drawPath(play, fillPaint)
            }
            AppCategory.MUSIC -> {
                val heights = floatArrayOf(12f, 24f, 32f, 20f, 14f)
                strokePaint.strokeWidth = 4f
                for (i in heights.indices) {
                    val x = (cx - 24f) + i * 12f
                    val h = heights[i]
                    strokePaint.color = if (i == 2) palette.secondaryColor else palette.primaryColor
                    canvas.drawLine(x, cy + h / 2f, x, cy - h / 2f, strokePaint)
                }
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy - 24f, 3f, fillPaint)
            }
            AppCategory.FILES -> {
                val folder = Path().apply {
                    moveTo(cx - 28f, cy - 16f)
                    lineTo(cx - 12f, cy - 16f)
                    lineTo(cx - 6f, cy - 8f)
                    lineTo(cx + 28f, cy - 8f)
                    lineTo(cx + 28f, cy + 20f)
                    lineTo(cx - 28f, cy + 20f)
                    close()
                }
                canvas.drawPath(folder, strokePaint)
                strokePaint.color = palette.secondaryColor
                canvas.drawLine(cx - 16f, cy + 6f, cx + 16f, cy + 6f, strokePaint.apply { strokeWidth = 2.5f })
            }
            AppCategory.STORE -> {
                val bag = Path().apply {
                    moveTo(cx - 22f, cy - 10f)
                    lineTo(cx + 22f, cy - 10f)
                    lineTo(cx + 18f, cy + 24f)
                    lineTo(cx - 18f, cy + 24f)
                    close()
                }
                canvas.drawPath(bag, strokePaint)
                canvas.drawArc(RectF(cx - 12f, cy - 22f, cx + 12f, cy - 2f), 180f, 180f, false, strokePaint)
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy + 6f, 4f, fillPaint)
            }
            AppCategory.CHAT -> {
                canvas.drawCircle(cx, cy, 26f, strokePaint)
                val phone = Path().apply {
                    moveTo(cx - 12f, cy + 6f)
                    cubicTo(cx - 16f, cy - 4f, cx - 4f, cy - 16f, cx + 6f, cy - 12f)
                    lineTo(cx + 10f, cy - 8f)
                    lineTo(cx + 6f, cy - 4f)
                    lineTo(cx + 2f, cy - 6f)
                    cubicTo(cx - 4f, cy - 2f, cx - 2f, cy + 4f, cx + 6f, cy + 2f)
                    lineTo(cx + 4f, cy + 6f)
                    lineTo(cx + 8f, cy + 10f)
                    close()
                }
                fillPaint.color = palette.secondaryColor
                canvas.drawPath(phone, fillPaint)
            }
            AppCategory.GENERIC -> {
                if (fallbackDrawable != null) {
                    val iconInnerSize = 100
                    val left = (size - iconInnerSize) / 2
                    val top = (size - iconInnerSize) / 2
                    fallbackDrawable.setBounds(left, top, left + iconInnerSize, top + iconInnerSize)
                    fallbackDrawable.draw(canvas)
                } else {
                    // Futuristic Monogram Glyphs
                    val firstChar = packageName.substringAfterLast('.').take(1).uppercase()
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.primaryColor
                        textSize = 54f
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    val yPos = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    canvas.drawText(firstChar, cx, yPos, textPaint)
                }
                // Center holographic ring
                canvas.drawCircle(cx, cy, 46f, strokePaint.apply { strokeWidth = 2f })
                fillPaint.color = palette.secondaryColor
                canvas.drawCircle(cx, cy - 46f, 3f, fillPaint)
                canvas.drawCircle(cx, cy + 46f, 3f, fillPaint)
            }
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
