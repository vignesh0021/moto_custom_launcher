package com.motorola.launcher.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Robust binary disk cache for launcher icons.
 * Features:
 * - SHA-256 collision-resistant file naming.
 * - 20-byte IconCacheHeader validation (magic, version, dimensions, payload size).
 * - Exact file length verification to reject truncated or corrupt payloads.
 * - Atomic writes via temporary files.
 * - Size-bounded LRU eviction to prevent unbounded disk growth.
 */
class BinaryDiskCache(
    context: Context,
    private val maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES
) {
    companion object {
        private const val TAG = "BinaryDiskCache"
        private const val CACHE_DIR_NAME = "hardware_icons_bin"
        const val DEFAULT_MAX_CACHE_BYTES = 50L * 1024 * 1024 // 50 MB limit
    }

    val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    /**
     * Converts a cache key into a deterministic SHA-256 hex string.
     */
    fun getFilenameForKey(cacheKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(cacheKey.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) } + ".bin"
    }

    /**
     * Reads and safely validates a cached bitmap from disk.
     * Returns null if the file is missing, corrupt, truncated, or incompatible.
     */
    fun readBitmap(cacheKey: String): Bitmap? {
        val file = File(cacheDir, getFilenameForKey(cacheKey))
        if (!file.exists() || !file.isFile) return null

        val fileLength = file.length()
        if (fileLength < IconCacheHeader.HEADER_SIZE) {
            file.delete()
            return null
        }

        try {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val header = IconCacheHeader.readFrom(bis)
                    if (header == null) {
                        Log.w(TAG, "Rejecting entry with invalid header: ${file.name}")
                        file.delete()
                        return null
                    }

                    if (!header.matchesFileLength(fileLength)) {
                        Log.w(TAG, "Rejecting truncated entry: expected ${header.payloadSize + IconCacheHeader.HEADER_SIZE} bytes, found $fileLength bytes")
                        file.delete()
                        return null
                    }

                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inMutable = false
                    }

                    val decoded = BitmapFactory.decodeStream(bis, null, options)
                    if (decoded == null) {
                        Log.w(TAG, "BitmapFactory failed to decode payload for ${file.name}")
                        file.delete()
                        return null
                    }

                    if (decoded.width != header.width || decoded.height != header.height) {
                        Log.w(TAG, "Dimension mismatch for ${file.name}: header=(${header.width}x${header.height}), decoded=(${decoded.width}x${decoded.height})")
                        file.delete()
                        return null
                    }

                    // Update last modified for LRU eviction tracking
                    file.setLastModified(System.currentTimeMillis())
                    return decoded
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed reading binary cache for key=$cacheKey: ${e.message}")
            try { file.delete() } catch (_: Throwable) {}
            return null
        }
    }

    /**
     * Atomically writes a software bitmap to the binary disk cache with header validation.
     */
    fun writeSoftwareBitmap(cacheKey: String, bitmap: Bitmap): Boolean {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return false

        val filename = getFilenameForKey(cacheKey)
        val targetFile = File(cacheDir, filename)
        val tempFile = File(cacheDir, "$filename.${System.nanoTime()}.tmp")

        try {
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.PNG
            }

            // Compress payload into memory buffer first to know exact payload size
            val payloadBaos = ByteArrayOutputStream()
            val compressed = bitmap.compress(format, 100, payloadBaos)
            if (!compressed) {
                return false
            }
            val payloadBytes = payloadBaos.toByteArray()
            if (payloadBytes.isEmpty()) {
                return false
            }

            val header = IconCacheHeader(
                width = bitmap.width,
                height = bitmap.height,
                payloadSize = payloadBytes.size
            )

            FileOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    header.writeTo(bos)
                    bos.write(payloadBytes)
                    bos.flush()
                }
            }

            // Atomic rename to ensure disk integrity
            if (targetFile.exists()) {
                targetFile.delete()
            }
            val success = tempFile.renameTo(targetFile)
            if (!success) {
                tempFile.delete()
                return false
            }

            trimToSizeIfNeeded()
            return true
        } catch (e: Throwable) {
            Log.w(TAG, "Failed writing binary cache for key=$cacheKey: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
            return false
        }
    }

    /**
     * Removes an individual icon entry by key.
     */
    fun delete(cacheKey: String): Boolean {
        val file = File(cacheDir, getFilenameForKey(cacheKey))
        return if (file.exists()) file.delete() else true
    }

    /**
     * Enforces bounded disk cache size by evicting oldest modified files.
     */
    @Synchronized
    fun trimToSizeIfNeeded() {
        val files = cacheDir.listFiles { file -> file.isFile && file.name.endsWith(".bin") } ?: return
        var totalSize = files.sumOf { it.length() }

        if (totalSize <= maxCacheBytes) return

        // Sort by lastModified ascending (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }
        for (file in sortedFiles) {
            val length = file.length()
            if (file.delete()) {
                totalSize -= length
                if (totalSize <= maxCacheBytes * 0.8) { // Trim to 80% watermark
                    break
                }
            }
        }
    }

    fun evictAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
