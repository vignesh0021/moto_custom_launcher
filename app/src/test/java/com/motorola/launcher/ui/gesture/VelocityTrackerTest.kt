package com.motorola.launcher.ui.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for VelocityTracker360Hz circular buffer and regression calculations.
 */
class VelocityTrackerTest {

    private lateinit var tracker: VelocityTracker360Hz

    @Before
    fun setUp() {
        tracker = VelocityTracker360Hz(historySize = 16)
    }

    @Test
    fun testEmptyTrackerReturnsZeroVelocity() {
        val velocity = tracker.calculateVelocity(1000L)
        assertEquals(0f, velocity, 0.001f)
    }

    @Test
    fun testSingleSampleReturnsZeroVelocity() {
        tracker.addPoint(1000L, 500f)
        val velocity = tracker.calculateVelocity(1000L)
        assertEquals(0f, velocity, 0.001f)
    }

    @Test
    fun testLinearFlingVelocityCalculation() {
        // Simulating a downward swipe at 1000 px/sec over 50ms
        // At 360Hz, sample interval is ~2.77ms
        val startTime = 1000L
        for (i in 0..15) {
            val t = startTime + (i * 3L) // every 3ms
            val pos = 100f + (i * 3f) // 1px per ms = 1000 px/sec
            tracker.addPoint(t, pos)
        }

        val lastTime = startTime + (15 * 3L)
        val velocity = tracker.calculateVelocity(lastTime)

        // Expected ~1000 px/s with regression tolerance
        assertTrue("Velocity should be positive and close to 1000 px/s: $velocity", velocity in 900f..1100f)
    }

    @Test
    fun testFlingUpNegativeVelocity() {
        // Simulating an upward swipe (decreasing Y) at -2000 px/sec
        val startTime = 2000L
        for (i in 0..10) {
            val t = startTime + (i * 4L)
            val pos = 800f - (i * 8f) // -2px per ms = -2000 px/sec
            tracker.addPoint(t, pos)
        }

        val lastTime = startTime + (10 * 4L)
        val velocity = tracker.calculateVelocity(lastTime)

        assertTrue("Velocity should be negative and close to -2000 px/s: $velocity", velocity in -2200f..-1800f)
    }

    @Test
    fun testCircularBufferWrapAround() {
        // Add 30 points into 16-size buffer to test wrap-around integrity
        val startTime = 5000L
        for (i in 0..30) {
            val t = startTime + (i * 2L)
            val pos = 50f + (i * 4f) // 2000 px/s
            tracker.addPoint(t, pos)
        }

        val lastTime = startTime + (30 * 2L)
        val velocity = tracker.calculateVelocity(lastTime)

        assertTrue("Wrapped circular buffer should still compute accurate velocity: $velocity", velocity in 1800f..2200f)
    }

    @Test
    fun testResetClearsVelocity() {
        tracker.addPoint(1000L, 100f)
        tracker.addPoint(1010L, 200f)
        tracker.reset()

        val velocity = tracker.calculateVelocity(1010L)
        assertEquals(0f, velocity, 0.001f)
    }

    @Test
    fun testOldSamplesBeyond100msWindowIgnored() {
        // Add sample 200ms ago, and one 150ms ago
        tracker.addPoint(1000L, 100f)
        tracker.addPoint(1050L, 200f)

        // Query at 1300ms (both samples older than 100ms)
        val velocity = tracker.calculateVelocity(1300L)
        assertEquals("Samples older than 100ms must be discarded", 0f, velocity, 0.001f)
    }
}
