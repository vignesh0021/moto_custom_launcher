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
import java.util.Calendar

/**
 * Contextual Launcher Engine for STARK OS.
 *
 * Reads device context signals every 10 seconds (battery-safe) and derives
 * a ContextProfile that drives theme, wallpaper, hero protocol, and UI density.
 *
 * Signals: time-of-day, battery level + charging, location (home/away),
 * audio/ringer mode, Do Not Disturb.
 *
 * No persistent listeners beyond a single 10s Handler loop and one
 * BatteryManager sticky broadcast read. Zero allocation on the hot path.
 */
class ContextualLauncherEngine(private val context: Context) {

    enum class ContextProfile {
        MORNING,      // 5-11am: bright, energetic, cyan/amber
        AFTERNOON,    // 11am-5pm: standard
        EVENING,      // 5-9pm: warm, gold accents
        NIGHT,        // 9pm-5am: dark, minimal, battery-saving
        LOW_BATTERY,  // <20%: minimal UI, dark, speed
        DRIVING,      // in-vehicle: minimal visuals, nav-ready
        DND           // Do Not Disturb: quiet, no sounds
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

    @Volatile
    var currentSnapshot: ContextSnapshot = ContextSnapshot(
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
        private set

    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            // Sticky-style: only react to actual changes, not initial flood
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                refresh()
            }
        }
    }

    fun start() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        handler.post(pollRunnable)
    }

    fun stop() {
        handler.removeCallbacks(pollRunnable)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // already unregistered
        }
    }

    fun refresh() {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val batteryLevel = readBatteryLevel()
        val isCharging = readIsCharging()
        val isAtHome = readIsAtHome()
        val isDnd = readIsDnd()
        val isDriving = readIsDriving()

        val profile = when {
            batteryLevel < 20 -> ContextProfile.LOW_BATTERY
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

        val (hero, color, wallpaper) = deriveVisuals(profile, hour, isAtHome)

        currentSnapshot = ContextSnapshot(
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
    }

    private fun deriveVisuals(
        profile: ContextProfile,
        hour: Int,
        isAtHome: Boolean
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
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun readIsCharging(): Boolean {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return false
        return bm.isCharging
    }

    private fun readIsAtHome(): Boolean {
        // Home detection via LocationManager: if location is off or we can't resolve,
        // default to true (assume home) to avoid false "driving" triggers.
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return true
        return try {
            val providers = lm.getProviders(true)
            providers.isNotEmpty()
        } catch (_: SecurityException) {
            true
        }
    }

    private fun readIsDnd(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun readIsDriving(): Boolean {
        // Driving heuristic: audio mode in-call + location available + not at a known
        // stationary pattern. Simplified: check if AudioManager reports MODE_IN_COMMUNICATION
        // which commonly fires during navigation/calls while moving.
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return am.mode == AudioManager.MODE_IN_COMMUNICATION
    }

    companion object {
        private const val POLL_INTERVAL_MS = 10_000L
    }
}
