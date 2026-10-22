package com.motorola.launcher.data.icon

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 20-byte binary header for validated icon cache disk entries.
 * Format:
 * [4B MAGIC: 'MOTO'] [4B VERSION: 1] [4B WIDTH] [4B HEIGHT] [4B PAYLOAD_SIZE]
 */
data class IconCacheHeader(
    val magic: Int = MAGIC,
    val version: Int = VERSION,
    val width: Int,
    val height: Int,
    val payloadSize: Int
) {
    companion object {
        const val MAGIC = 0x4D4F544F // ASCII "MOTO"
        const val VERSION = 1
        const val HEADER_SIZE = 20
        const val MAX_DIMENSION = 4096
        const val MAX_PAYLOAD_BYTES = 10 * 1024 * 1024 // 10 MB limit for single icon

        /**
         * Reads and validates an IconCacheHeader from the given stream.
         * Returns null if reading fails, magic/version mismatch, or dimensions/size are invalid.
         */
        fun readFrom(inputStream: InputStream): IconCacheHeader? {
            val bytes = ByteArray(HEADER_SIZE)
            var bytesRead = 0
            while (bytesRead < HEADER_SIZE) {
                val count = inputStream.read(bytes, bytesRead, HEADER_SIZE - bytesRead)
                if (count == -1) return null
                bytesRead += count
            }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            val magic = buffer.int
            val version = buffer.int
            val width = buffer.int
            val height = buffer.int
            val payloadSize = buffer.int

            val header = IconCacheHeader(magic, version, width, height, payloadSize)
            return if (header.isValid()) header else null
        }
    }

    /**
     * Checks if header values conform to format constraints.
     */
    fun isValid(): Boolean {
        if (magic != MAGIC) return false
        if (version != VERSION) return false
        if (width <= 0 || width > MAX_DIMENSION) return false
        if (height <= 0 || height > MAX_DIMENSION) return false
        if (payloadSize <= 0 || payloadSize > MAX_PAYLOAD_BYTES) return false
        return true
    }

    /**
     * Verifies that the actual file length matches exactly HEADER_SIZE + payloadSize.
     */
    fun matchesFileLength(fileLength: Long): Boolean {
        return fileLength == (HEADER_SIZE.toLong() + payloadSize.toLong())
    }

    /**
     * Writes this 20-byte header to the target output stream in BIG_ENDIAN format.
     */
    fun writeTo(outputStream: OutputStream) {
        val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(magic)
        buffer.putInt(version)
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.putInt(payloadSize)
        outputStream.write(buffer.array())
    }
}
