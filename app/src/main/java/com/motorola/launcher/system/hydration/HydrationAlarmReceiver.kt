package com.motorola.launcher.system.hydration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled hydration reminder fires.
 */
class HydrationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HYDRATION_ALARM = "com.motorola.launcher.action.HYDRATION_ALARM"
        private const val TAG = "HydrationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val orchestrator = FamilyCompanionOrchestrator.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = orchestrator.repository.getSettingsDirect()
                val lastIntake = orchestrator.repository.getLastIntakeDirect()

                val allow = orchestrator.contextManager.shouldAllowReminder(
                    settings = settings,
                    lastIntakeTimestamp = lastIntake?.timestamp
                )

                if (allow) {
                    val dialogue = orchestrator.getNextDialogue()
                    val consumed = orchestrator.repository.getTodayTotalMlDirect()

                    HydrationNotificationHelper.showReminderNotification(
                        context = context,
                        dialogue = dialogue,
                        consumedMl = consumed,
                        targetMl = settings.dailyGoalMl
                    )
                    Log.i(TAG, "Hydration reminder notification posted successfully")
                } else {
                    Log.i(TAG, "Hydration reminder suppressed by context manager")
                }

                // Reschedule next regular reminder
                if (settings.enabled) {
                    orchestrator.scheduler.scheduleNextReminder(
                        settings = settings,
                        lastIntakeTimestamp = lastIntake?.timestamp
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing hydration alarm: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
