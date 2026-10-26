package com.motorola.launcher.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SystemTelemetryState calculations, unavailable defaults, and safe formatting.
 */
class SystemTelemetryStateTest {

    @Test
    fun testDefaultUnavailableState() {
        val defaultState = SystemTelemetryState()

        // Verify unavailable defaults (no fake 100% or 144Hz)
        assertEquals(-1, defaultState.batteryLevel)
        assertFalse(defaultState.isCharging)
        assertTrue(defaultState.batteryTempCelsius.isNaN())
        assertEquals(-1, defaultState.batteryVoltageMv)
        assertEquals("Unknown", defaultState.batteryHealth)
        assertEquals(-1L, defaultState.ramUsedBytes)
        assertEquals(-1L, defaultState.ramTotalBytes)
        assertEquals(-1L, defaultState.storageFreeBytes)
        assertEquals(-1L, defaultState.storageTotalBytes)
        assertEquals("Unknown", defaultState.networkType)
        assertEquals("--:--", defaultState.uptimeFormatted)
        assertEquals(-1, defaultState.refreshRateHz)

        // Verify formatted unavailable placeholders
        assertEquals("--%", defaultState.batteryLevelFormatted)
        assertEquals("--°C", defaultState.batteryTempFormatted)
        assertEquals("-- V", defaultState.batteryVoltageFormatted)
        assertEquals("--Hz", defaultState.refreshRateFormatted)
        assertEquals("-- GB", defaultState.ramUsedFormatted)
        assertEquals("-- GB", defaultState.ramTotalFormatted)
        assertEquals("-- GB", defaultState.storageFreeFormatted)
        assertEquals("-- GB", defaultState.storageTotalFormatted)
        assertEquals("-- GB", defaultState.storageUsedFormatted)
        assertEquals(0, defaultState.ramPercent)
        assertEquals(0, defaultState.storagePercentUsed)
    }

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
        assertEquals("192.0 GB", state.storageUsedFormatted)
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
        assertEquals("0.0 GB", zeroState.ramUsedFormatted)
        assertEquals("-- GB", zeroState.ramTotalFormatted)
        assertEquals("0.0 GB", zeroState.storageFreeFormatted)
        assertEquals("-- GB", zeroState.storageTotalFormatted)
    }

    @Test
    fun testPopulatedTelemetryFormatting() {
        val normalState = SystemTelemetryState(
            batteryLevel = 85,
            isCharging = true,
            batteryTempCelsius = 31.5f,
            batteryVoltageMv = 4200,
            batteryHealth = "Good",
            refreshRateHz = 120
        )

        assertEquals("85%", normalState.batteryLevelFormatted)
        assertTrue(normalState.isCharging)
        assertEquals("31.5°C", normalState.batteryTempFormatted)
        assertEquals("4.20 V", normalState.batteryVoltageFormatted)
        assertEquals("120Hz", normalState.refreshRateFormatted)
        assertEquals("Good", normalState.batteryHealth)
    }
}
