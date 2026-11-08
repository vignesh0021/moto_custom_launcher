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
 * Reschedules hydration reminders upon system reboot, time adjustments, or timezone changes.
 */
class HydrationBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "HydrationBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Received system broadcast: $action")

        val pendingResult = goAsync()
        val orchestrator = FamilyCompanionOrchestrator.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = orchestrator.repository.getSettingsDirect()
                if (settings.enabled) {
                    val lastIntake = orchestrator.repository.getLastIntakeDirect()
                    orchestrator.scheduler.scheduleNextReminder(
                        settings = settings,
                        lastIntakeTimestamp = lastIntake?.timestamp
                    )
                    Log.i(TAG, "Hydration reminders rescheduled following $action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling reminders on boot: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
