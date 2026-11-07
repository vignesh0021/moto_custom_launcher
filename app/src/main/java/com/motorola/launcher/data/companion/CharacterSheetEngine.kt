package com.motorola.launcher.data.companion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.util.LruCache
import com.motorola.launcher.R
import com.motorola.launcher.ui.model.CompanionStyle
import java.io.InputStream
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Poses sliced from a character sheet turnaround row.
 */
enum class CharacterPose {
    PORTRAIT,   // Close-up facial headshot
    FRONT,      // Full body front-facing pose
    QUARTER,    // 3/4 turn pose (walking / ready)
    SIDE,       // Side profile walking pose
    BACK        // Back turnaround view
}

/**
 * Facial expressions sliced from the bottom row of the character sheet.
 */
enum class CharacterExpression {
    HAPPY,          // Smiling / Laughing / Celebrating
    CURIOUS,        // Pondering / Inquiring / Finger on chin
    SURPRISED,      // Wide-eyed / Shocked
    MISCHIEVOUS,    // Smirk / Witty
    CONFUSED,       // Skeptical / Puzzled
    UPSET           // Mildly Upset / Stern / Warning finger
}

/**
 * Collection of sliced poses and expressions for an animated character companion.
 */
data class CharacterCompanionSprites(
    val poses: Map<CharacterPose, Bitmap>,
    val expressions: Map<CharacterExpression, Bitmap>
)

/**
 * Engine that slices and background-removes multi-frame sprites from character sheets.
 * Supports bundled sheets (Mithra, Mithra's Mom) and dynamic user-uploaded character sheets.
 * Provides real multi-frame walk cycle frames, dynamic facial expression transitions, and
 * edge-seeded transparency flood-fill.
 */
class CharacterSheetEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CharacterSheetEngine"

        @Volatile
        private var instance: CharacterSheetEngine? = null

        fun getInstance(context: Context): CharacterSheetEngine {
            return instance ?: synchronized(this) {
                instance ?: CharacterSheetEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    // In-memory cache of sliced sprite collections (keyed by style or URI)
    private val spriteCache = LruCache<String, CharacterCompanionSprites>(8)

    /**
     * Retrieves or slices the sprites for the given CompanionStyle or custom URI.
     */
    fun getSprites(style: CompanionStyle, customUri: String = ""): CharacterCompanionSprites? {
        val cacheKey = if (style == CompanionStyle.CUSTOM_SHEET) "custom_$customUri" else style.id
        spriteCache.get(cacheKey)?.let { return it }

        val sheetBitmap: Bitmap? = when (style) {
            CompanionStyle.MITHRA -> loadDrawableBitmap(R.drawable.sheet_mithra)
            CompanionStyle.MITHRA_MOM -> loadDrawableBitmap(R.drawable.sheet_mithra_mom)
            CompanionStyle.CUSTOM_SHEET -> {
                if (customUri.isNotBlank()) loadUriBitmap(customUri) else null
            }
            else -> null
        } ?: return null

        val sprites = sliceSheet(style, sheetBitmap)
        if (sprites != null) {
            spriteCache.put(cacheKey, sprites)
        }
        return sprites
    }

    /**
     * Returns the walking sprite frame for the current walk phase (0 to 2π).
     * Cycles smoothly between SIDE profile stride and 3/4 turn stride for a real walk cycle.
     */
    fun getWalkFrame(sprites: CharacterCompanionSprites, walkPhase: Float): Bitmap? {
        val normalizedPhase = (walkPhase % (Math.PI.toFloat() * 2f) + (Math.PI.toFloat() * 2f)) % (Math.PI.toFloat() * 2f)
        val isSideStride = normalizedPhase < Math.PI.toFloat()
        return if (isSideStride) {
            sprites.poses[CharacterPose.SIDE] ?: sprites.poses[CharacterPose.QUARTER] ?: sprites.poses[CharacterPose.FRONT]
        } else {
            sprites.poses[CharacterPose.QUARTER] ?: sprites.poses[CharacterPose.SIDE] ?: sprites.poses[CharacterPose.FRONT]
        }
    }

    private fun loadDrawableBitmap(drawableResId: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            BitmapFactory.decodeResource(context.resources, drawableResId, options)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed loading drawable $drawableResId: ${e.message}")
            null
        }
    }

    private fun loadUriBitmap(uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val stream: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = true
            }
            val bitmap = BitmapFactory.decodeStream(stream, null, options)
            stream?.close()
            bitmap
        } catch (e: Throwable) {
            Log.e(TAG, "Failed loading URI $uriString: ${e.message}")
            null
        }
    }

    /**
     * Slices the master character sheet into full-body turnaround poses and 6 facial expressions.
     */
    private fun sliceSheet(style: CompanionStyle, master: Bitmap): CharacterCompanionSprites? {
        val w = master.width
        val h = master.height
        if (w <= 0 || h <= 0) return null

        val poses = mutableMapOf<CharacterPose, Bitmap>()
        val expressions = mutableMapOf<CharacterExpression, Bitmap>()

        val splitY = (h * 0.60f).toInt()
        val bottomH = h - splitY

        try {
            // 1. Slice bottom 6 facial expressions (equally spaced across sheet width)
            val colW = w / 6
            val exprList = listOf(
                CharacterExpression.HAPPY,
                CharacterExpression.CURIOUS,
                CharacterExpression.SURPRISED,
                CharacterExpression.MISCHIEVOUS,
                CharacterExpression.CONFUSED,
                CharacterExpression.UPSET
            )

            for (i in 0 until 6) {
                val startX = (i * colW).coerceIn(0, w - 1)
                val cellW = colW.coerceAtMost(w - startX)
                if (cellW > 0 && bottomH > 0) {
                    val rawCell = Bitmap.createBitmap(master, startX, splitY, cellW, bottomH)
                    val cleanedCell = removeBackground(rawCell, tolerance = 28)
                    expressions[exprList[i]] = cleanedCell
                }
            }

            // 2. Slice top poses and portrait
            if (style == CompanionStyle.MITHRA) {
                // Mithra layout:
                // Left: Portrait (0 .. 30% width)
                val portraitW = (w * 0.30f).toInt()
                val rawPortrait = Bitmap.createBitmap(master, 0, 0, portraitW, splitY)
                poses[CharacterPose.PORTRAIT] = removeBackground(rawPortrait, tolerance = 28)

                // Poses: Quarter (3/4), Side (walk), Front
                val remainingW = w - portraitW
                val poseW = remainingW / 3
                val qX = portraitW
                val sX = portraitW + poseW
                val fX = portraitW + poseW * 2

                val rawQuarter = Bitmap.createBitmap(master, qX, 0, poseW, splitY)
                val rawSide = Bitmap.createBitmap(master, sX, 0, poseW, splitY)
                val rawFront = Bitmap.createBitmap(master, fX, 0, w - fX, splitY)

                poses[CharacterPose.QUARTER] = removeBackground(rawQuarter, tolerance = 28)
                poses[CharacterPose.SIDE] = removeBackground(rawSide, tolerance = 28)
                poses[CharacterPose.FRONT] = removeBackground(rawFront, tolerance = 28)

            } else {
                // Mithra Mom / Custom layout:
                // Left: Portrait (0 .. 34% width)
                val portraitW = (w * 0.34f).toInt()
                val rawPortrait = Bitmap.createBitmap(master, 0, 0, portraitW, splitY)
                poses[CharacterPose.PORTRAIT] = removeBackground(rawPortrait, tolerance = 28)

                // 4 Turnaround Poses: Front, Quarter, Side, Back
                val remainingW = w - portraitW
                val poseW = remainingW / 4
                val p0X = portraitW
                val p1X = portraitW + poseW
                val p2X = portraitW + poseW * 2
                val p3X = portraitW + poseW * 3

                val rawFront = Bitmap.createBitmap(master, p0X, 0, poseW, splitY)
                val rawQuarter = Bitmap.createBitmap(master, p1X, 0, poseW, splitY)
                val rawSide = Bitmap.createBitmap(master, p2X, 0, poseW, splitY)
                val rawBack = Bitmap.createBitmap(master, p3X, 0, w - p3X, splitY)

                poses[CharacterPose.FRONT] = removeBackground(rawFront, tolerance = 28)
                poses[CharacterPose.QUARTER] = removeBackground(rawQuarter, tolerance = 28)
                poses[CharacterPose.SIDE] = removeBackground(rawSide, tolerance = 28)
                poses[CharacterPose.BACK] = removeBackground(rawBack, tolerance = 28)
            }

            return CharacterCompanionSprites(poses = poses, expressions = expressions)
        } catch (e: Throwable) {
            Log.e(TAG, "Error slicing character sheet: ${e.message}")
            return null
        }
    }

    /**
     * Edge-seeded flood-fill to make background transparent on any sliced sprite or custom sheet.
     * Samples background color at borders and makes connected matching pixels transparent.
     */
    fun removeBackground(src: Bitmap, tolerance: Int = 28): Bitmap {
        val width = src.width
        val height = src.height
        if (width <= 0 || height <= 0) return src

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample background color from top-left corner
        val sampleColor = pixels[0]
        val sa = (sampleColor ushr 24) and 0xFF
        val sr = (sampleColor ushr 16) and 0xFF
        val sg = (sampleColor ushr 8) and 0xFF
        val sb = sampleColor and 0xFF

        // If already transparent, return early
        if (sa < 30) return src

        fun isBg(color: Int): Boolean {
            val a = (color ushr 24) and 0xFF
            if (a < 30) return true
            val r = (color ushr 16) and 0xFF
            val g = (color ushr 8) and 0xFF
            val b = color and 0xFF
            return abs(r - sr) <= tolerance && abs(g - sg) <= tolerance && abs(b - sb) <= tolerance
        }

        val visited = BooleanArray(width * height)
        val queue = ArrayDeque<Int>()

        // Seed with outer perimeter edges
        for (x in 0 until width) {
            val topIdx = x
            if (isBg(pixels[topIdx])) {
                visited[topIdx] = true
                queue.add(topIdx)
            }
            val btmIdx = (height - 1) * width + x
            if (isBg(pixels[btmIdx])) {
                visited[btmIdx] = true
                queue.add(btmIdx)
            }
        }
        for (y in 0 until height) {
            val leftIdx = y * width
            if (!visited[leftIdx] && isBg(pixels[leftIdx])) {
                visited[leftIdx] = true
                queue.add(leftIdx)
            }
            val rightIdx = y * width + (width - 1)
            if (!visited[rightIdx] && isBg(pixels[rightIdx])) {
                visited[rightIdx] = true
                queue.add(rightIdx)
            }
        }

        // BFS flood-fill transparency
        while (!queue.isEmpty()) {
            val idx = queue.poll() ?: break
            pixels[idx] = 0 // Transparent ARGB_8888
            val cx = idx % width
            val cy = idx / width

            if (cx > 0) {
                val nIdx = idx - 1
                if (!visited[nIdx] && isBg(pixels[nIdx])) {
                    visited[nIdx] = true
                    queue.add(nIdx)
                }
            }
            if (cx < width - 1) {
                val nIdx = idx + 1
                if (!visited[nIdx] && isBg(pixels[nIdx])) {
                    visited[nIdx] = true
                    queue.add(nIdx)
                }
            }
            if (cy > 0) {
                val nIdx = idx - width
                if (!visited[nIdx] && isBg(pixels[nIdx])) {
                    visited[nIdx] = true
                    queue.add(nIdx)
                }
            }
            if (cy < height - 1) {
                val nIdx = idx + width
                if (!visited[nIdx] && isBg(pixels[nIdx])) {
                    visited[nIdx] = true
                    queue.add(nIdx)
                }
            }
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}
