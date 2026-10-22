package com.motorola.launcher.data.icon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unit tests for BinaryDiskCache and IconCacheHeader validation and corruption safety.
 */
class BinaryDiskCacheTest {

    @Test
    fun testValidHeaderSerializationAndDeserialization() {
        val original = IconCacheHeader(
            magic = IconCacheHeader.MAGIC,
            version = IconCacheHeader.VERSION,
            width = 192,
            height = 192,
            payloadSize = 4096
        )

        assertTrue(original.isValid())

        val baos = ByteArrayOutputStream()
        original.writeTo(baos)
        val bytes = baos.toByteArray()

        assertEquals(IconCacheHeader.HEADER_SIZE, bytes.size)

        val bais = ByteArrayInputStream(bytes)
        val deserialized = IconCacheHeader.readFrom(bais)

        assertNotNull(deserialized)
        assertEquals(original.magic, deserialized?.magic)
        assertEquals(original.version, deserialized?.version)
        assertEquals(original.width, deserialized?.width)
        assertEquals(original.height, deserialized?.height)
        assertEquals(original.payloadSize, deserialized?.payloadSize)
    }

    @Test
    fun testRejectCorruptMagic() {
        val buffer = ByteBuffer.allocate(IconCacheHeader.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(0xDEADBEEF.toInt()) // Invalid magic
        buffer.putInt(1)
        buffer.putInt(192)
        buffer.putInt(192)
        buffer.putInt(1024)

        val bais = ByteArrayInputStream(buffer.array())
        val header = IconCacheHeader.readFrom(bais)
        assertNull("Corrupt magic must be rejected", header)
    }

    @Test
    fun testRejectInvalidVersion() {
        val buffer = ByteBuffer.allocate(IconCacheHeader.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(IconCacheHeader.MAGIC)
        buffer.putInt(99) // Invalid version
        buffer.putInt(192)
        buffer.putInt(192)
        buffer.putInt(1024)

        val bais = ByteArrayInputStream(buffer.array())
        val header = IconCacheHeader.readFrom(bais)
        assertNull("Invalid version must be rejected", header)
    }

    @Test
    fun testRejectInvalidDimensions() {
        // Zero or negative dimension
        val h1 = IconCacheHeader(width = 0, height = 192, payloadSize = 500)
        assertFalse(h1.isValid())

        val h2 = IconCacheHeader(width = 192, height = -1, payloadSize = 500)
        assertFalse(h2.isValid())

        // Oversized dimension (> 4096)
        val h3 = IconCacheHeader(width = 5000, height = 192, payloadSize = 500)
        assertFalse(h3.isValid())
    }

    @Test
    fun testRejectInvalidPayloadSize() {
        // Zero payload
        val h1 = IconCacheHeader(width = 192, height = 192, payloadSize = 0)
        assertFalse(h1.isValid())

        // Negative payload
        val h2 = IconCacheHeader(width = 192, height = 192, payloadSize = -100)
        assertFalse(h2.isValid())

        // Oversized payload (> 10MB)
        val h3 = IconCacheHeader(width = 192, height = 192, payloadSize = 15 * 1024 * 1024)
        assertFalse(h3.isValid())
    }

    @Test
    fun testMatchesFileLengthExactValidation() {
        val header = IconCacheHeader(width = 192, height = 192, payloadSize = 1000)
        val expectedLength = (IconCacheHeader.HEADER_SIZE + 1000).toLong()

        assertTrue(header.matchesFileLength(expectedLength))
        assertFalse(header.matchesFileLength(expectedLength - 1)) // Truncated
        assertFalse(header.matchesFileLength(expectedLength + 1)) // Appended junk
    }

    @Test
    fun testTruncatedStreamDuringHeaderRead() {
        // Provide only 10 bytes instead of 20
        val partialBytes = ByteArray(10) { 0x4D.toByte() }
        val bais = ByteArrayInputStream(partialBytes)
        val header = IconCacheHeader.readFrom(bais)
        assertNull("Truncated stream must safely return null", header)
    }
}
