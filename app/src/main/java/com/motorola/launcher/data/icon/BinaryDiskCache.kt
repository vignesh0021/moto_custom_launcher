package com.motorola.launcher.data.icon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * High-performance binary disk cache for hardware-backed launcher icons.
 * Format:
 * [4B MAGIC: 'MOTO'] [4B VERSION: 1] [4B WIDTH] [4B HEIGHT] [4B PAYLOAD_SIZE] [WEBP/PNG BYTES]
 */
class BinaryDiskCache(context: Context) {

    companion object {
        private const val TAG = "BinaryDiskCache"
        private const val CACHE_DIR_NAME = "hardware_icons_bin"
        private const val MAGIC = 0x4D4F544F // "MOTO"
        private const val VERSION = 1
    }

    private val cacheDir = File(context.cacheDir, CACHE_DIR_NAME).apply {
        if (!exists()) mkdirs()
    }

    /**
     * Attempts to read a cached icon directly into a Bitmap.Config.HARDWARE texture.
     */
    fun readHardwareBitmap(cacheKey: String): Bitmap? {
        val file = File(cacheDir, "${cacheKey.hashCode()}.bin")
        if (!file.exists() || file.length() < 20) return null

        try {
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    val header = ByteArray(20)
                    val read = bis.read(header)
                    if (read != 20) return null

                    val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
                    val magic = buffer.int
                    val version = buffer.int
                    val width = buffer.int
                    val height = buffer.int
                    val payloadSize = buffer.int

                    if (magic != MAGIC || version != VERSION || payloadSize <= 0) {
                        return null
                    }

                    // Decode directly into GPU HARDWARE memory
                    val options = BitmapFactory.Options().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            inPreferredConfig = Bitmap.Config.HARDWARE
                        }
                        inMutable = false
                    }

                    return BitmapFactory.decodeStream(bis, null, options)
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed reading binary cache for key=$cacheKey: ${e.message}")
            file.delete()
            return null
        }
    }

    /**
     * Serializes a rendered software bitmap to binary disk cache before conversion to HARDWARE.
     */
    fun writeSoftwareBitmap(cacheKey: String, bitmap: Bitmap) {
        val file = File(cacheDir, "${cacheKey.hashCode()}.bin")
        val tempFile = File(cacheDir, "${cacheKey.hashCode()}.tmp")

        try {
            val bos = BufferedOutputStream(FileOutputStream(tempFile))
            val header = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN).apply {
                putInt(MAGIC)
                putInt(VERSION)
                putInt(bitmap.width)
                putInt(bitmap.height)
                putInt(0) // placeholder for payload size
            }
            bos.write(header.array())

            // Compress as WebP Lossless (or PNG fallback)
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.PNG
            }

            bitmap.compress(format, 100, bos)
            bos.flush()
            bos.close()

            // Update payload size in header
            val totalLength = tempFile.length()
            val payloadSize = (totalLength - 20).toInt()
            if (payloadSize > 0) {
                java.io.RandomAccessFile(tempFile, "rw").use { raf ->
                    raf.seek(16)
                    raf.writeInt(payloadSize)
                }
                if (file.exists()) file.delete()
                tempFile.renameTo(file)
            } else {
                tempFile.delete()
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed writing binary cache for key=$cacheKey: ${e.message}")
            if (tempFile.exists()) tempFile.delete()
        }
    }

    fun evictAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
