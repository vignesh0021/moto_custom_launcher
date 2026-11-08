package com.motorola.launcher.system.companion

import android.content.Context
import android.util.Log
import com.motorola.launcher.data.db.HydrationSettingsEntity
import java.util.Calendar

/**
 * Context Manager for V2H Family Companion and Smart Water Reminder.
 * Handles:
 * - Waking and sleeping hours validation (e.g. 08:00 to 22:00)
 * - Quiet hours suppression
 * - Temporary pause checking
 * - Recent intake cooldown (prevents reminding immediately after drinking)
 * - Timezone and daylight saving resilience
 */
class CompanionContextManager(private val context: Context) {

    companion object {
        private const val TAG = "CompanionContextMgr"
        const val RECENT_DRINK_COOLDOWN_MILLIS = 30 * 60 * 1000L // 30 minutes cooldown
    }

    /**
     * Determines whether a hydration reminder is permitted to trigger right now.
     */
    fun shouldAllowReminder(
        settings: HydrationSettingsEntity,
        lastIntakeTimestamp: Long?
    ): Boolean {
        if (!settings.enabled) {
            Log.d(TAG, "Reminders disabled by user setting")
            return false
        }

        val now = System.currentTimeMillis()

        // 1. Check temporary pause
        if (settings.pausedUntilTimestamp > now) {
            Log.d(TAG, "Reminders currently paused until ${settings.pausedUntilTimestamp}")
            return false
        }

        // 2. Check recent drink cooldown (do not remind if user just drank)
        if (lastIntakeTimestamp != null && (now - lastIntakeTimestamp) < RECENT_DRINK_COOLDOWN_MILLIS) {
            Log.d(TAG, "User drank water recently (${(now - lastIntakeTimestamp) / 60000} mins ago). Suppressing reminder.")
            return false
        }

        // 3. Check quiet / sleeping hours
        if (settings.quietHoursEnabled && !isWithinWakingHours(settings.startTime, settings.endTime)) {
            Log.d(TAG, "Current time is outside waking hours (${settings.startTime} - ${settings.endTime}). Suppressing reminder.")
            return false
        }

        return true
    }

    /**
     * Checks if current local time is within the configured waking window (HH:mm - HH:mm).
     * Handles normal daytime intervals (e.g., 08:00 to 22:00) as well as overnight intervals.
     */
    fun isWithinWakingHours(
        startTime: String,
        endTime: String,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(startTime, defaultMinutes = 8 * 60)
        val endMinutes = parseTimeToMinutes(endTime, defaultMinutes = 22 * 60)

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Spans overnight (e.g. 20:00 to 06:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    /**
     * Calculates the next valid reminder time in milliseconds, ensuring it falls within
     * waking hours and accounts for the configured interval or snooze.
     */
    fun calculateNextReminderTime(
        settings: HydrationSettingsEntity,
        intervalMinutes: Int,
        baseTimestamp: Long = System.currentTimeMillis()
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = baseTimestamp }
        cal.add(Calendar.MINUTE, intervalMinutes)

        if (!settings.quietHoursEnabled) {
            return cal.timeInMillis
        }

        val startMinutes = parseTimeToMinutes(settings.startTime, defaultMinutes = 8 * 60)
        val endMinutes = parseTimeToMinutes(settings.endTime, defaultMinutes = 22 * 60)

        val targetMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val isWaking = if (startMinutes <= endMinutes) {
            targetMinutes in startMinutes..endMinutes
        } else {
            targetMinutes >= startMinutes || targetMinutes <= endMinutes
        }

        if (!isWaking) {
            // Target is during sleep hours; advance calendar to the start of the next waking window
            val wakeCal = Calendar.getInstance()
            wakeCal.set(Calendar.HOUR_OF_DAY, startMinutes / 60)
            wakeCal.set(Calendar.MINUTE, startMinutes % 60)
            wakeCal.set(Calendar.SECOND, 0)
            wakeCal.set(Calendar.MILLISECOND, 0)

            if (wakeCal.timeInMillis <= baseTimestamp) {
                // If the wake time for today already passed, schedule for tomorrow morning
                wakeCal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return wakeCal.timeInMillis
        }

        return cal.timeInMillis
    }

    private fun parseTimeToMinutes(timeStr: String, defaultMinutes: Int): Int {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            h * 60 + m
        } catch (e: Exception) {
            defaultMinutes
        }
    }
}
