package com.motorola.launcher.system.hydration

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import com.motorola.launcher.system.CompanionVoiceManager
import com.motorola.launcher.system.companion.CompanionCharacter
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * BroadcastReceiver triggered by AlarmManager when a scheduled hydration reminder fires.
 * Works reliably even when the phone is locked or in Doze mode.
 */
class HydrationAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_HYDRATION_ALARM = "com.motorola.launcher.action.HYDRATION_ALARM"
        private const val TAG = "HydrationAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val orchestrator = FamilyCompanionOrchestrator.getInstance(appContext)

        // Acquire a brief wake lock so lock-screen notification & optional voice reminder fire immediately when locked
        val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "moto_launcher:hydration_alarm_wakelock"
        )
        try {
            wakeLock?.acquire(10_000L)
        } catch (_: Throwable) {}

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

                    // 1. Post high-priority lock-screen public notification
                    HydrationNotificationHelper.showReminderNotification(
                        context = appContext,
                        dialogue = dialogue,
                        consumedMl = consumed,
                        targetMl = settings.dailyGoalMl
                    )

                    // 2. Queue in-app companion so it also greets the user when unlocked
                    orchestrator.triggerCompanionPrompt()

                    // 3. Speak Tamil voice reminder if enabled (even when phone is locked)
                    if (settings.voiceEnabled) {
                        speakBackgroundReminder(
                            context = appContext,
                            tamilText = dialogue.tamilText,
                            isMithra = dialogue.character == CompanionCharacter.MITHRA
                        )
                    }

                    Log.i(TAG, "Hydration reminder notification & lock-screen alert posted successfully")
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
                try {
                    if (wakeLock?.isHeld == true) wakeLock.release()
                } catch (_: Throwable) {}
                pendingResult.finish()
            }
        }
    }

    private suspend fun speakBackgroundReminder(
        context: Context,
        tamilText: String,
        isMithra: Boolean
    ) {
        var ttsInstance: TextToSpeech? = null
        try {
            ttsInstance = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = ttsInstance ?: return@TextToSpeech
                    val taLocale = Locale("ta", "IN")
                    val taAvail = try {
                        engine.isLanguageAvailable(taLocale)
                    } catch (_: Throwable) {
                        TextToSpeech.LANG_NOT_SUPPORTED
                    }
                    if (taAvail >= TextToSpeech.LANG_AVAILABLE) {
                        engine.language = taLocale
                    } else {
                        engine.language = Locale.US
                    }
                    engine.setPitch(if (isMithra) 1.65f else 1.08f)
                    engine.setSpeechRate(if (isMithra) 1.05f else 0.95f)
                    val cleanText = CompanionVoiceManager.sanitizeForSpeech(tamilText)
                    engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "LOCK_HYDRATION_VOICE")
                }
            }
            delay(5500L)
        } catch (_: Throwable) {
        } finally {
            try {
                ttsInstance?.stop()
                ttsInstance?.shutdown()
            } catch (_: Throwable) {}
        }
    }
}
