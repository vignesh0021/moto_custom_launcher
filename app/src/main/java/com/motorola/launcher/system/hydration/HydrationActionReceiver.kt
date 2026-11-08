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
 * BroadcastReceiver that processes notification action clicks
 * (e.g. Drink 200 ml, Drink 250 ml, Snooze) with duplicate prevention.
 */
class HydrationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DRINK_200 = "com.motorola.launcher.action.DRINK_200"
        const val ACTION_DRINK_250 = "com.motorola.launcher.action.DRINK_250"
        const val ACTION_SNOOZE = "com.motorola.launcher.action.SNOOZE"

        const val EXTRA_AMOUNT_ML = "extra_amount_ml"
        const val EXTRA_ACTION_TOKEN = "extra_action_token"

        private const val TAG = "HydrationActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val orchestrator = FamilyCompanionOrchestrator.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.action
                val token = intent.getStringExtra(EXTRA_ACTION_TOKEN)

                when (action) {
                    ACTION_DRINK_200, ACTION_DRINK_250 -> {
                        val amount = intent.getIntExtra(EXTRA_AMOUNT_ML, 250)
                        val source = if (amount == 200) "NOTIFICATION_200" else "NOTIFICATION_250"

                        val success = orchestrator.recordWaterIntake(
                            amountMl = amount,
                            source = source,
                            actionToken = token
                        )
                        if (success) {
                            Log.i(TAG, "Processed notification intake: $amount ml")
                        } else {
                            Log.w(TAG, "Ignored duplicate notification intake: $amount ml")
                        }
                    }
                    ACTION_SNOOZE -> {
                        orchestrator.snoozeReminder()
                        Log.i(TAG, "Snoozed reminder from notification")
                    }
                }

                // Dismiss notification once user interacts
                HydrationNotificationHelper.dismissNotification(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling hydration action: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
