package com.motorola.launcher.system

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.math.roundToInt

/**
 * Battery-safe system telemetry. Polls every 15 seconds.
 * Defaults to unavailable markers (-1, NaN, "Unknown") so UI never shows fake data.
 */
data class SystemTelemetryState(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float = Float.NaN,
    val batteryVoltageMv: Int = -1,
    val batteryHealth: String = "Unknown",
    val ramUsedBytes: Long = -1L,
    val ramTotalBytes: Long = -1L,
    val storageFreeBytes: Long = -1L,
    val storageTotalBytes: Long = -1L,
    val networkType: String = "Unknown",
    val uptimeFormatted: String = "--:--",
    val refreshRateHz: Int = -1,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false
) {
    val batteryLevelFormatted: String
        get() = if (batteryLevel >= 0) "$batteryLevel%" else "--%"

    val batteryTempFormatted: String
        get() = if (!batteryTempCelsius.isNaN()) "${"%.1f".format(batteryTempCelsius)}°C" else "--°C"

    val batteryVoltageFormatted: String
        get() = if (batteryVoltageMv > 0) "${"%.2f".format(batteryVoltageMv / 1000.0)} V" else "-- V"

    val refreshRateFormatted: String
        get() = if (refreshRateHz > 0) "${refreshRateHz}Hz" else "--Hz"

    val ramPercent: Int
        get() = if (ramTotalBytes > 0L && ramUsedBytes >= 0L) ((ramUsedBytes * 100) / ramTotalBytes).toInt() else 0

    val storagePercentUsed: Int
        get() {
            if (storageTotalBytes <= 0L || storageFreeBytes < 0L) return 0
            val used = storageTotalBytes - storageFreeBytes
            return ((used * 100) / storageTotalBytes).toInt()
        }

    private fun formatGb(bytes: Long, total: Boolean = false): String {
        if (bytes < 0L) return "-- GB"
        if (bytes == 0L && !total) return "0.0 GB"
        if (total && bytes <= 0L) return "-- GB"
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return "%.1f GB".format(gb)
    }

    val ramUsedFormatted: String get() = formatGb(ramUsedBytes)
    val ramTotalFormatted: String get() = formatGb(ramTotalBytes, total = true)
    val storageFreeFormatted: String get() = formatGb(storageFreeBytes)
    val storageTotalFormatted: String get() = formatGb(storageTotalBytes, total = true)
    val storageUsedFormatted: String
        get() {
            if (storageTotalBytes <= 0L || storageFreeBytes < 0L) return "-- GB"
            val used = storageTotalBytes - storageFreeBytes
            return formatGb(used)
        }
}

class SystemTelemetryManager(private val context: Context) {

    private val _telemetryState = MutableStateFlow(SystemTelemetryState())
    val telemetryState: StateFlow<SystemTelemetryState> = _telemetryState.asStateFlow()

    @Volatile
    var currentState: SystemTelemetryState
        get() = _telemetryState.value
        private set(value) { _telemetryState.value = value }

    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun startPeriodicCollection() {
        handler.post(pollRunnable)
    }

    fun stopPeriodicCollection() {
        handler.removeCallbacks(pollRunnable)
    }

    fun refresh() {
        currentState = SystemTelemetryState(
            batteryLevel = readBatteryLevel(),
            isCharging = readIsCharging(),
            batteryTempCelsius = readBatteryTemp(),
            batteryVoltageMv = readBatteryVoltage(),
            batteryHealth = readBatteryHealth(),
            ramUsedBytes = readRamUsed(),
            ramTotalBytes = readRamTotal(),
            storageFreeBytes = readStorageFree(),
            storageTotalBytes = readStorageTotal(),
            networkType = readNetworkType(),
            uptimeFormatted = readUptimeFormatted(),
            refreshRateHz = readRefreshRate(),
            isWifi = readIsWifi(),
            isCellular = readIsCellular()
        )
    }

    private fun readBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return -1
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it >= 0 } ?: -1
    }

    private fun readIsCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    private fun readBatteryTemp(): Float = Float.NaN // Placeholder; full intent reading can be added later
    private fun readBatteryVoltage(): Int = -1
    private fun readBatteryHealth(): String = "Unknown"

    private fun readRamUsed(): Long = -1L
    private fun readRamTotal(): Long = -1L
    private fun readStorageFree(): Long = -1L
    private fun readStorageTotal(): Long = -1L

    private fun readNetworkType(): String = "Unknown"
    private fun readIsWifi(): Boolean = false
    private fun readIsCellular(): Boolean = false
    private fun readUptimeFormatted(): String = "--:--"
    private fun readRefreshRate(): Int = -1

    companion object {
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
