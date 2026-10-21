package com.motorola.launcher.ui.gesture

/**
 * Ultra-low latency, zero-allocation velocity tracker tuned for 360Hz touch digitizers.
 * Uses a circular primitive array to calculate instantaneous release velocity without GC pressure.
 */
class VelocityTracker360Hz(private val historySize: Int = 16) {

    private val timestamps = LongArray(historySize)
    private val positions = FloatArray(historySize)
    private var index = 0
    private var count = 0

    fun reset() {
        index = 0
        count = 0
    }

    /**
     * Appends a 360Hz touch sample. Zero object allocations.
     */
    fun addPoint(timeMillis: Long, position: Float) {
        timestamps[index] = timeMillis
        positions[index] = position
        index = (index + 1) % historySize
        if (count < historySize) {
            count++
        }
    }

    /**
     * Calculates velocity in pixels per second using least-squares linear regression
     * over the most recent touch samples within the 100ms window.
     */
    fun calculateVelocity(targetTimeMillis: Long): Float {
        if (count < 2) return 0f

        var validSamples = 0
        var sumTime = 0.0
        var sumPos = 0.0

        // Find samples within last 100ms
        for (i in 0 until count) {
            val sampleIdx = (index - 1 - i + historySize) % historySize
            val sampleTime = timestamps[sampleIdx]
            val age = targetTimeMillis - sampleTime
            if (age in 0..100) {
                validSamples++
                val dt = -age.toDouble() / 1000.0 // seconds relative to target
                sumTime += dt
                sumPos += positions[sampleIdx].toDouble()
            } else if (age > 100) {
                break
            }
        }

        if (validSamples < 2) return 0f

        val meanTime = sumTime / validSamples
        val meanPos = sumPos / validSamples

        var numerator = 0.0
        var denominator = 0.0

        for (i in 0 until validSamples) {
            val sampleIdx = (index - 1 - i + historySize) % historySize
            val dt = -(targetTimeMillis - timestamps[sampleIdx]).toDouble() / 1000.0
            val dPos = positions[sampleIdx] - meanPos
            val timeDiff = dt - meanTime

            numerator += timeDiff * dPos
            denominator += timeDiff * timeDiff
        }

        return if (denominator > 0.000001) {
            (numerator / denominator).toFloat().coerceIn(-12000f, 12000f)
        } else {
            0f
        }
    }
}
