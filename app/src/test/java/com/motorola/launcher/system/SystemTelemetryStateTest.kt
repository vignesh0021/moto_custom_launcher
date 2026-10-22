package com.motorola.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SystemTelemetryState calculations and formatting.
 */
class SystemTelemetryStateTest {

    @Test
    fun testRamPercentageCalculation() {
        val total = 12L * 1024 * 1024 * 1024 // 12 GB
        val used = 6L * 1024 * 1024 * 1024  // 6 GB
        val state = SystemTelemetryState(
            ramUsedBytes = used,
            ramTotalBytes = total
        )

        assertEquals(50, state.ramPercent)
        assertEquals("6.0 GB", state.ramUsedFormatted)
        assertEquals("12.0 GB", state.ramTotalFormatted)
    }

    @Test
    fun testStoragePercentageCalculation() {
        val total = 256L * 1024 * 1024 * 1024 // 256 GB
        val free = 64L * 1024 * 1024 * 1024   // 64 GB free -> 192 GB used = 75%
        val state = SystemTelemetryState(
            storageFreeBytes = free,
            storageTotalBytes = total
        )

        assertEquals(75, state.storagePercentUsed)
        assertEquals("64.0 GB", state.storageFreeFormatted)
        assertEquals("256.0 GB", state.storageTotalFormatted)
    }

    @Test
    fun testZeroDivisionSafety() {
        val zeroState = SystemTelemetryState(
            ramUsedBytes = 0L,
            ramTotalBytes = 0L,
            storageFreeBytes = 0L,
            storageTotalBytes = 0L
        )

        assertEquals(0, zeroState.ramPercent)
        assertEquals(0, zeroState.storagePercentUsed)
    }

    @Test
    fun testBatteryStatusMapping() {
        val normalState = SystemTelemetryState(
            batteryLevel = 85,
            isCharging = false,
            batteryTempCelsius = 31.5f,
            batteryVoltageMv = 4200
        )

        assertEquals(85, normalState.batteryLevel)
        assertEquals(false, normalState.isCharging)
        assertTrue(normalState.batteryTempCelsius > 30f)
        assertEquals(4200, normalState.batteryVoltageMv)
    }
}
