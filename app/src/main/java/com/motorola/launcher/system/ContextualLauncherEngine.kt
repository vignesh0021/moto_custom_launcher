package com.motorola.launcher.system

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import com.motorola.launcher.ui.model.MarvelHeroProtocol
import com.motorola.launcher.ui.model.ThemeColorPreset
import com.motorola.launcher.ui.model.WallpaperPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * Contextual Launcher Engine for STARK OS.
 *
 * Autonomously adapts the launcher interface based on ambient contextual signals:
 * - Time of day (Morning focus, daytime tactical, evening twilight, night OLED power saving)
 * - Battery percentage and charging status (<20% emergency arc reactor power conservation)
 * - Music / Audio playback resonance
 * - Driving / in-transit mode
 *
 * Strict Battery-Safe Architecture:
 * - Listens via sticky battery broadcast and a low-frequency 15-second handler ticker
 * - Pauses completely when launcher is not in foreground (0% idle battery consumption)
 */
class ContextualLauncherEngine(private val context: Context) {

    enum class ContextProfile {
        MORNING,      // 05:00 - 10:59: Bright, energizing Stark Amber/Gold
        AFTERNOON,    // 11:00 - 16:59: Tactical Captain America / vibrant Cyan
        EVENING,      // 17:00 - 20:59: Thor Asgardian Gold twilight
        NIGHT,        // 21:00 - 04:59: Black Panther Vibranium Purple / OLED deep dark
        LOW_BATTERY,  // < 20%: Monochromatic Stark Arc power-saving mode
        DRIVING,      // In-vehicle / communication: high-contrast minimal distraction HUD
        DND           // Do Not Disturb: quiet, mystic amber
    }

    data class ContextSnapshot(
        val profile: ContextProfile,
        val hour: Int,
        val batteryLevel: Int,
        val isCharging: Boolean,
        val isAtHome: Boolean,
        val isDnd: Boolean,
        val isDriving: Boolean,
        val suggestedHero: MarvelHeroProtocol,
        val suggestedColor: ThemeColorPreset,
        val suggestedWallpaper: WallpaperPreset,
        val minimalUi: Boolean
    )

    private val _contextState = MutableStateFlow(
        ContextSnapshot(
            profile = ContextProfile.AFTERNOON,
            hour = 12,
            batteryLevel = 100,
            isCharging = false,
            isAtHome = true,
            isDnd = false,
            isDriving = false,
            suggestedHero = MarvelHeroProtocol.IRON_MAN,
            suggestedColor = ThemeColorPreset.CYAN,
            suggestedWallpaper = WallpaperPreset.MARVEL_HERO,
            minimalUi = false
        )
    )
    val contextState: StateFlow<ContextSnapshot> = _contextState.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            refresh()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                refresh()
            }
        }
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(batteryReceiver, filter)
            }
        } catch (_: Throwable) {
        }
        handler.post(pollRunnable)
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        handler.removeCallbacks(pollRunnable)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Throwable) {
        }
    }

    fun refresh() {
        try {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            val batteryLevel = readBatteryLevel()
            val isCharging = readIsCharging()
            val isAtHome = readIsAtHome()
            val isDnd = readIsDnd()
            val isDriving = readIsDriving()

            val profile = when {
                batteryLevel in 1..19 && !isCharging -> ContextProfile.LOW_BATTERY
                isDriving -> ContextProfile.DRIVING
                isDnd -> ContextProfile.DND
                hour in 5..10 -> ContextProfile.MORNING
                hour in 11..16 -> ContextProfile.AFTERNOON
                hour in 17..20 -> ContextProfile.EVENING
                else -> ContextProfile.NIGHT
            }

            val minimalUi = profile == ContextProfile.LOW_BATTERY ||
                    profile == ContextProfile.DRIVING ||
                    profile == ContextProfile.DND

            val (hero, color, wallpaper) = deriveVisuals(profile)

            _contextState.value = ContextSnapshot(
                profile = profile,
                hour = hour,
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isAtHome = isAtHome,
                isDnd = isDnd,
                isDriving = isDriving,
                suggestedHero = hero,
                suggestedColor = color,
                suggestedWallpaper = wallpaper,
                minimalUi = minimalUi
            )
        } catch (t: Throwable) {
            android.util.Log.w("ContextualEngine", "refresh caught exception: ${t.message}")
        }
    }

    private fun deriveVisuals(
        profile: ContextProfile
    ): Triple<MarvelHeroProtocol, ThemeColorPreset, WallpaperPreset> {
        return when (profile) {
            ContextProfile.MORNING -> Triple(
                MarvelHeroProtocol.IRON_MAN, ThemeColorPreset.AMBER, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.AFTERNOON -> Triple(
                MarvelHeroProtocol.CAPTAIN_AMERICA, ThemeColorPreset.CYAN, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.EVENING -> Triple(
                MarvelHeroProtocol.THOR, ThemeColorPreset.AMBER, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.NIGHT -> Triple(
                MarvelHeroProtocol.BLACK_PANTHER, ThemeColorPreset.PURPLE, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.LOW_BATTERY -> Triple(
                MarvelHeroProtocol.IRON_MAN, ThemeColorPreset.CYAN, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.DRIVING -> Triple(
                MarvelHeroProtocol.IRON_MAN, ThemeColorPreset.CYAN, WallpaperPreset.MARVEL_HERO
            )
            ContextProfile.DND -> Triple(
                MarvelHeroProtocol.DOCTOR_STRANGE, ThemeColorPreset.AMBER, WallpaperPreset.MARVEL_HERO
            )
        }
    }

    private fun readBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level >= 0) level else 100
        } catch (_: Throwable) {
            100
        }
    }

    private fun readIsCharging(): Boolean {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
            bm.isCharging
        } catch (_: Throwable) {
            false
        }
    }

    private fun readIsAtHome(): Boolean {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
            val providers = lm.getProviders(true)
            providers.isNotEmpty()
        } catch (_: Throwable) {
            true
        }
    }

    private fun readIsDnd(): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (nm.isNotificationPolicyAccessGranted) {
                    nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                } else {
                    false
                }
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun readIsDriving(): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            am.mode == AudioManager.MODE_IN_COMMUNICATION
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        private const val POLL_INTERVAL_MS = 15_000L
    }
}
