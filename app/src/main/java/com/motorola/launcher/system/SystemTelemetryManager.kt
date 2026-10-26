package com.motorola.launcher.system

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.view.Display
import android.view.WindowManager
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 100% Immutable representation of live device telemetry.
 * All measurements reflect true Android system APIs with graceful unavailable states.
 * No hardcoded 144Hz or simulated battery values.
 */
@Immutable
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
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val uptimeFormatted: String = "--:--",
    val refreshRateHz: Int = -1
) {
    val ramPercent: Int
        get() = if (ramTotalBytes > 0 && ramUsedBytes >= 0) {
            ((ramUsedBytes * 100) / ramTotalBytes).toInt().coerceIn(0, 100)
        } else 0

    val storagePercentUsed: Int
        get() = if (storageTotalBytes > 0 && storageFreeBytes >= 0) {
            (((storageTotalBytes - storageFreeBytes) * 100) / storageTotalBytes).toInt().coerceIn(0, 100)
        } else 0

    val batteryLevelFormatted: String
        get() = if (batteryLevel >= 0) "$batteryLevel%" else "--%"

    val batteryTempFormatted: String
        get() = if (!batteryTempCelsius.isNaN()) String.format(Locale.US, "%.1f°C", batteryTempCelsius) else "--°C"

    val batteryVoltageFormatted: String
        get() = if (batteryVoltageMv > 0) String.format(Locale.US, "%.2f V", batteryVoltageMv / 1000f) else "-- V"

    val refreshRateFormatted: String
        get() = if (refreshRateHz > 0) "${refreshRateHz}Hz" else "--Hz"

    val ramUsedFormatted: String
        get() = if (ramUsedBytes >= 0) {
            String.format(Locale.US, "%.1f GB", ramUsedBytes / (1024.0 * 1024.0 * 1024.0))
        } else "-- GB"

    val ramTotalFormatted: String
        get() = if (ramTotalBytes > 0) {
            String.format(Locale.US, "%.1f GB", ramTotalBytes / (1024.0 * 1024.0 * 1024.0))
        } else "-- GB"

    val storageFreeFormatted: String
        get() = if (storageFreeBytes >= 0) {
            String.format(Locale.US, "%.1f GB", storageFreeBytes / (1024.0 * 1024.0 * 1024.0))
        } else "-- GB"

    val storageTotalFormatted: String
        get() = if (storageTotalBytes > 0) {
            String.format(Locale.US, "%.1f GB", storageTotalBytes / (1024.0 * 1024.0 * 1024.0))
        } else "-- GB"

    val storageUsedFormatted: String
        get() = if (storageTotalBytes > 0 && storageFreeBytes >= 0) {
            String.format(Locale.US, "%.1f GB", (storageTotalBytes - storageFreeBytes) / (1024.0 * 1024.0 * 1024.0))
        } else "-- GB"
}

/**
 * Lifecycle-aware real-time telemetry collector for STARK OS / JARVIS HUD.
 * Collects hardware signals efficiently with isolated fail-safes and lifecycle controls.
 */
class SystemTelemetryManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _telemetryState = MutableStateFlow(SystemTelemetryState())
    val telemetryState: StateFlow<SystemTelemetryState> = _telemetryState.asStateFlow()

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var periodicJob: Job? = null
    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent != null) {
                updateBatteryFromIntent(intent)
            }
        }
    }

    init {
        // Register sticky battery broadcast
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val initialIntent = context.registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
            if (initialIntent != null) {
                updateBatteryFromIntent(initialIntent)
            }
        } catch (_: Throwable) {
            // In case of restricted runtime or testing without Android environment
        }

        // Start initial collection
        startPeriodicCollection()
    }

    /**
     * Starts or resumes periodic background polling for telemetry.
     */
    fun startPeriodicCollection() {
        if (periodicJob?.isActive == true) return
        periodicJob = scope.launch {
            while (isActive) {
                refreshPeriodicTelemetry()
                delay(5000L)
            }
        }
    }

    /**
     * Pauses periodic polling when launcher activity is backgrounded to conserve battery.
     */
    fun stopPeriodicCollection() {
        periodicJob?.cancel()
        periodicJob = null
    }

    private fun updateBatteryFromIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)

        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val tempC = if (tempRaw != -1) tempRaw / 10.0f else Float.NaN
        val healthStr = when (healthRaw) {
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            else -> "Unknown"
        }

        _telemetryState.update { current ->
            current.copy(
                batteryLevel = if (pct >= 0) pct else current.batteryLevel,
                isCharging = charging,
                batteryTempCelsius = if (!tempC.isNaN()) tempC else current.batteryTempCelsius,
                batteryVoltageMv = if (voltage > 0) voltage else current.batteryVoltageMv,
                batteryHealth = healthStr
            )
        }
    }

    /**
     * Collects telemetry with independent try-catch blocks for each hardware subsystem.
     * A failure in network state or storage never blocks memory or display readings.
     */
    fun refreshPeriodicTelemetry() {
        // 1. RAM Measurement (Isolated)
        try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            if (memInfo.totalMem > 0) {
                val total = memInfo.totalMem
                val used = (memInfo.totalMem - memInfo.availMem).coerceAtLeast(0L)
                _telemetryState.update { it.copy(ramUsedBytes = used, ramTotalBytes = total) }
            }
        } catch (_: Throwable) {
            // Keep existing RAM state on failure
        }

        // 2. Storage Measurement (Isolated)
        try {
            val dataPath = Environment.getDataDirectory()
            val stat = StatFs(dataPath.path)
            val total = stat.totalBytes
            val free = stat.availableBytes
            if (total > 0) {
                _telemetryState.update { it.copy(storageFreeBytes = free, storageTotalBytes = total) }
            }
        } catch (_: Throwable) {
            // Keep existing storage state on failure
        }

        // 3. Network Type & Status (Isolated with ACCESS_NETWORK_STATE)
        try {
            var netType = "Offline"
            var isWifi = false
            var isCell = false
            val activeNetwork = connectivityManager?.activeNetwork
            val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
            if (caps != null) {
                when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        netType = "Wi-Fi"
                        isWifi = true
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        netType = "Cellular"
                        isCell = true
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        netType = "Ethernet"
                    }
                    else -> netType = "Connected"
                }
            }
            _telemetryState.update { it.copy(networkType = netType, isWifi = isWifi, isCellular = isCell) }
        } catch (_: Throwable) {
            // Keep existing network state on failure
        }

        // 4. System Uptime (Isolated)
        try {
            val uptimeMs = SystemClock.elapsedRealtime()
            val hours = (uptimeMs / (1000 * 60 * 60)) % 24
            val minutes = (uptimeMs / (1000 * 60)) % 60
            val formattedUptime = String.format(Locale.US, "%02d:%02d", hours, minutes)
            _telemetryState.update { it.copy(uptimeFormatted = formattedUptime) }
        } catch (_: Throwable) {
            // Keep existing uptime on failure
        }

        // 5. Display Refresh Rate (Isolated, dynamically read, never hardcoded)
        try {
            val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    context.display
                } catch (_: Throwable) {
                    @Suppress("DEPRECATION")
                    windowManager?.defaultDisplay
                }
            } else {
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay
            }
            val rate = display?.refreshRate?.toInt() ?: -1
            if (rate > 0) {
                _telemetryState.update { it.copy(refreshRateHz = rate) }
            }
        } catch (_: Throwable) {
            // Keep existing refresh rate
        }
    }

    /**
     * Safely releases receivers, cancels periodic background jobs, and terminates the scope.
     */
    fun release() {
        stopPeriodicCollection()
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
                isReceiverRegistered = false
            } catch (_: Throwable) {
            }
        }
        scope.cancel()
    }
}
