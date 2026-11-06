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
import java.io.File

/**
 * Battery-safe system telemetry. Polls every 15 seconds (not on a hot path).
 * Exposes RAM, storage, network, battery as an immutable snapshot.
 */
data class SystemTelemetryState(
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val totalRamMb: Long = 0L,
    val availableRamMb: Long = 0L,
    val totalStorageGb: Double = 0.0,
    val availableStorageGb: Double = 0.0,
    val isNetworkConnected: Boolean = false,
    val networkType: String = "NONE",
    val uptimeHours: Long = 0L,
    val batteryTempFormatted: String = "--",
    val ramTotalFormatted: String = "--",
    val ramPercent: Int = 0,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val refreshRateFormatted: String = "60Hz",
    val uptimeFormatted: String = "--"
) {
    val ramUsedMb: Long get() = (totalRamMb - availableRamMb).coerceAtLeast(0L)
}

class SystemTelemetryManager(private val context: Context) {

    @Volatile
    var currentState: SystemTelemetryState = SystemTelemetryState()
        private set

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

    fun stop() {
        handler.removeCallbacks(pollRunnable)
    }

    fun refresh() {
        val totalRam = readTotalRamMb()
        val availRam = readAvailableRamMb()
        val ramPct = if (totalRam > 0L) (((totalRam - availRam) * 100) / totalRam).toInt() else 0
        currentState = SystemTelemetryState(
            batteryLevel = readBatteryLevel(),
            isCharging = readIsCharging(),
            totalRamMb = totalRam,
            availableRamMb = availRam,
            totalStorageGb = readTotalStorageGb(),
            availableStorageGb = readAvailableStorageGb(),
            isNetworkConnected = readNetworkConnected(),
            networkType = readNetworkType(),
            uptimeHours = android.os.SystemClock.uptimeMillis() / 3_600_000L,
            batteryTempFormatted = readBatteryTempFormatted(),
            ramTotalFormatted = "${totalRam}MB",
            ramPercent = ramPct,
            isWifi = readNetworkType() == "WIFI",
            isCellular = readNetworkType() == "CELLULAR",
            refreshRateFormatted = readRefreshRateFormatted(),
            uptimeFormatted = formatUptime(android.os.SystemClock.uptimeMillis())
        )
    }

    private fun readBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun readIsCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    private fun readBatteryTempFormatted(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return "--"
            val temp = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10.0
            String.format("%.1f°C", temp)
        } catch (_: Exception) { "--" }
    }

    private fun readTotalRamMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024 * 1024)
    }

    private fun readAvailableRamMb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0L
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.availMem / (1024 * 1024)
    }

    private fun readTotalStorageGb(): Double {
        return try {
            val stat = StatFs(File("/data").absolutePath)
            stat.totalBytes / (1024.0 * 1024 * 1024)
        } catch (_: Exception) { 0.0 }
    }

    private fun readAvailableStorageGb(): Double {
        return try {
            val stat = StatFs(File("/data").absolutePath)
            stat.availableBytes / (1024.0 * 1024 * 1024)
        } catch (_: Exception) { 0.0 }
    }

    private fun readNetworkConnected(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun readNetworkType(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "NONE"
        val network = cm.activeNetwork ?: return "NONE"
        val caps = cm.getNetworkCapabilities(network) ?: return "NONE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
    }

    private fun readRefreshRateFormatted(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dm = context.resources.displayMetrics
                val refresh = context.display?.refreshRate ?: 60f
                "${refresh.toInt()}Hz"
            } else "60Hz"
        } catch (_: Exception) { "60Hz" }
    }

    private fun formatUptime(ms: Long): String {
        val hours = ms / 3_600_000L
        val minutes = (ms % 3_600_000L) / 60_000L
        return "${hours}h ${minutes}m"
    }

    companion object {
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
