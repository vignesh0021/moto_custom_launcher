package com.motorola.launcher.system.hydration

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.motorola.launcher.data.db.HydrationSettingsEntity
import com.motorola.launcher.system.companion.CompanionContextManager

/**
 * Reliable background reminder scheduler for the Smart Water Reminder System.
 * Uses Android AlarmManager setAndAllowWhileIdle to guarantee on-time delivery
 * through Doze Mode without wasting battery on permanent foreground services.
 */
class HydrationReminderScheduler(private val context: Context) {

    companion object {
        private const val TAG = "HydrationScheduler"
        const val ALARM_REQUEST_CODE = 4401
        const val SNOOZE_REQUEST_CODE = 4402

        @Volatile
        private var INSTANCE: HydrationReminderScheduler? = null

        fun getInstance(context: Context): HydrationReminderScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HydrationReminderScheduler(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val contextManager = CompanionContextManager(context)

    /**
     * Schedules the next reminder based on user interval, waking hours, and recent intake timestamp.
     */
    fun scheduleNextReminder(
        settings: HydrationSettingsEntity,
        lastIntakeTimestamp: Long? = null,
        baseTimestamp: Long = System.currentTimeMillis()
    ) {
        if (!settings.enabled) {
            cancelReminders()
            return
        }

        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available on this device")
            return
        }

        val targetTimestamp = contextManager.calculateNextReminderTime(
            settings = settings,
            intervalMinutes = settings.reminderIntervalMinutes,
            baseTimestamp = baseTimestamp
        )

        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_HYDRATION_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleRtcWakeupAlarm(targetTimestamp, pendingIntent)
        Log.i(TAG, "Next reminder scheduled successfully for timestamp: $targetTimestamp")
    }

    /**
     * Schedules a quick snooze reminder (e.g. 15 minutes).
     */
    fun scheduleSnooze(snoozeDurationMillis: Long) {
        if (alarmManager == null) return

        val targetTimestamp = System.currentTimeMillis() + snoozeDurationMillis
        val intent = Intent(context, HydrationAlarmReceiver::class.java).apply {
            action = HydrationAlarmReceiver.ACTION_HYDRATION_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleRtcWakeupAlarm(targetTimestamp, pendingIntent)
        Log.i(TAG, "Snooze alarm scheduled for timestamp: $targetTimestamp")
    }

    private fun scheduleRtcWakeupAlarm(targetTimestamp: Long, pendingIntent: PendingIntent) {
        val am = alarmManager ?: return
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                }
                else -> {
                    am.setExact(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                }
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, targetTimestamp, pendingIntent)
                }
            } catch (inner: Exception) {
                Log.e(TAG, "Fallback alarm failed: ${inner.message}", inner)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling alarm: ${e.message}", e)
        }
    }

    /**
     * Cancels any scheduled hydration reminder alarms.
     */
    fun cancelReminders() {
        if (alarmManager == null) return
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "All scheduled hydration alarms cancelled")
        }
    }
}
