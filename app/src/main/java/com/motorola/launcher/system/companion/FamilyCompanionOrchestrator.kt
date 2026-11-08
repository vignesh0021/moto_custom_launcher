package com.motorola.launcher.system.companion

import android.content.Context
import android.util.Log
import com.motorola.launcher.data.db.HydrationSettingsEntity
import com.motorola.launcher.data.repository.HydrationRepository
import com.motorola.launcher.system.hydration.HydrationReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Character type associated with a reminder.
 */
enum class CompanionCharacter {
    MITHRA,
    AMMA,
    SHARED
}

/**
 * Rich dialogue metadata containing Tamil speech, English translation, and character identity.
 */
data class CompanionDialogue(
    val character: CompanionCharacter,
    val title: String,
    val tamilText: String,
    val englishText: String,
    val isCelebration: Boolean = false
)

/**
 * Orchestrator integrating the V2H Family Companion with the Smart Water Reminder System.
 * Coordinates database tracking, context evaluation, Tamil dialogue rotation,
 * background alarm scheduling, and UI state flows.
 */
class FamilyCompanionOrchestrator private constructor(
    private val context: Context,
    val repository: HydrationRepository,
    val contextManager: CompanionContextManager,
    val scheduler: HydrationReminderScheduler
) {
    companion object {
        private const val TAG = "FamilyCompanionOrch"

        @Volatile
        private var INSTANCE: FamilyCompanionOrchestrator? = null

        fun getInstance(context: Context): FamilyCompanionOrchestrator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appContext = context.applicationContext
                    val repo = HydrationRepository.getInstance(appContext)
                    val ctxMgr = CompanionContextManager(appContext)
                    val sched = HydrationReminderScheduler.getInstance(appContext)
                    FamilyCompanionOrchestrator(appContext, repo, ctxMgr, sched).also {
                        INSTANCE = it
                    }
                }
            }
        }

        // Section B Dialogues
        val MITHRA_REMINDERS = listOf(
            CompanionDialogue(CompanionCharacter.MITHRA, "மித்ரா (மகள்)", "அப்பா! தண்ணி குடிச்சீங்களா?", "Daddy! Did you drink water?"),
            CompanionDialogue(CompanionCharacter.MITHRA, "மித்ரா (மகள்)", "அப்பா, தண்ணி குடிக்க மறந்துடாதீங்க!", "Daddy, don't forget to drink water!"),
            CompanionDialogue(CompanionCharacter.MITHRA, "மித்ரா (மகள்)", "அப்பா, வாங்க தண்ணி குடிக்கலாம்!", "Daddy, come let's drink water!"),
            CompanionDialogue(CompanionCharacter.MITHRA, "மித்ரா (மகள்)", "அப்பா, இன்னும் கொஞ்சம் தண்ணி குடிங்க!", "Daddy, drink a little more water!")
        )

        val AMMA_REMINDERS = listOf(
            CompanionDialogue(CompanionCharacter.AMMA, "அம்மா (மனைவி)", "ஏங்க, கொஞ்சம் தண்ணி குடிச்சிட்டு வேலையைப் பாருங்க.", "Dear, drink some water and continue your work."),
            CompanionDialogue(CompanionCharacter.AMMA, "அம்மா (மனைவி)", "நேரம் ஆகுது, தண்ணி குடிக்க மறந்துடாதீங்க.", "Time is passing, don't forget to drink water."),
            CompanionDialogue(CompanionCharacter.AMMA, "அம்மா (மனைவி)", "உடம்பை கவனிச்சுக்கோங்க. தண்ணி குடிங்க.", "Take care of your health. Drink water."),
            CompanionDialogue(CompanionCharacter.AMMA, "அம்மா (மனைவி)", "போதுமான அளவு தண்ணி குடிக்கிறீங்களான்னு கவனிச்சுக்கோங்க.", "Make sure you are drinking enough water.")
        )

        val SHARED_REMINDERS = listOf(
            CompanionDialogue(
                CompanionCharacter.SHARED,
                "மித்ரா & அம்மா",
                "மித்ரா: \"அம்மா, அப்பா தண்ணி குடிச்சாங்களா?\"\nஅம்மா: \"இன்னும் குடிக்கலையா? வாங்க, முதல்ல தண்ணி குடிக்கலாம்!\"",
                "Mithra: \"Amma, did Daddy drink water?\"\nAmma: \"Not yet? Come, let's drink water first!\""
            )
        )

        val CELEBRATION_DIALOGUES = listOf(
            CompanionDialogue(CompanionCharacter.MITHRA, "மித்ரா (மகள்)", "அப்பா சூப்பர்! இன்றைய இலக்கை முடிச்சிட்டீங்க! 🎉", "Daddy super! You completed today's goal! 🎉", isCelebration = true),
            CompanionDialogue(CompanionCharacter.AMMA, "அம்மா (மனைவி)", "ரொம்ப சந்தோஷம்ங்க! உடம்பை நல்லா பார்த்துக்கிட்டீங்க! 🌟", "So proud of you! You took great care of your health! 🌟", isCelebration = true)
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Idempotency token cache to prevent double-recording from rapid notification taps
    private val processedActionTokens = ConcurrentHashMap<String, Long>()

    // Dialogue rotation state
    private val mithraIndex = AtomicInteger(0)
    private val ammaIndex = AtomicInteger(0)
    private val sharedIndex = AtomicInteger(0)
    private var lastCharacterUsed = CompanionCharacter.MITHRA

    // Reactive StateFlows
    val settings: StateFlow<HydrationSettingsEntity> = repository.settings.stateIn(
        scope,
        SharingStarted.Eagerly,
        HydrationSettingsEntity()
    )

    val todayTotalMl: StateFlow<Int> = repository.todayTotalMl.stateIn(
        scope,
        SharingStarted.Eagerly,
        0
    )

    val remainingMl: StateFlow<Int> = combine(todayTotalMl, settings) { consumed, s ->
        (s.dailyGoalMl - consumed).coerceAtLeast(0)
    }.stateIn(scope, SharingStarted.Eagerly, 2000)

    val progressRatio: StateFlow<Float> = combine(todayTotalMl, settings) { consumed, s ->
        if (s.dailyGoalMl <= 0) 1f else (consumed.toFloat() / s.dailyGoalMl).coerceIn(0f, 1f)
    }.stateIn(scope, SharingStarted.Eagerly, 0f)

    val glassesCount: StateFlow<Int> = todayTotalMl.combine(settings) { consumed, _ ->
        (consumed / 250)
    }.stateIn(scope, SharingStarted.Eagerly, 0)

    val isGoalReached: StateFlow<Boolean> = combine(todayTotalMl, settings) { consumed, s ->
        consumed >= s.dailyGoalMl && s.dailyGoalMl > 0
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val _activeDialogue = MutableStateFlow(getNextDialogue())
    val activeDialogue: StateFlow<CompanionDialogue> = _activeDialogue.asStateFlow()

    init {
        // Automatically schedule or sync next alarm whenever settings change
        scope.launch {
            settings.collect { s ->
                if (s.enabled) {
                    val lastIntake = repository.getLastIntakeDirect()
                    scheduler.scheduleNextReminder(s, lastIntake?.timestamp)
                } else {
                    scheduler.cancelReminders()
                }
            }
        }
    }

    /**
     * Gets the next rotated Tamil dialogue based on user's character preferences (Mithra, Amma, or Both).
     */
    fun getNextDialogue(): CompanionDialogue {
        val s = settings.value
        val selection = s.selectedCharacters.uppercase()

        val character = when (selection) {
            "MITHRA" -> CompanionCharacter.MITHRA
            "AMMA" -> CompanionCharacter.AMMA
            else -> {
                // Rotate between Mithra, Amma, and occasional Shared interaction
                when (lastCharacterUsed) {
                    CompanionCharacter.MITHRA -> CompanionCharacter.AMMA
                    CompanionCharacter.AMMA -> if ((0..2).random() == 0) CompanionCharacter.SHARED else CompanionCharacter.MITHRA
                    CompanionCharacter.SHARED -> CompanionCharacter.MITHRA
                }
            }
        }
        lastCharacterUsed = character

        val dialogue = when (character) {
            CompanionCharacter.MITHRA -> {
                val idx = mithraIndex.getAndIncrement() % MITHRA_REMINDERS.size
                MITHRA_REMINDERS[idx]
            }
            CompanionCharacter.AMMA -> {
                val idx = ammaIndex.getAndIncrement() % AMMA_REMINDERS.size
                AMMA_REMINDERS[idx]
            }
            CompanionCharacter.SHARED -> {
                val idx = sharedIndex.getAndIncrement() % SHARED_REMINDERS.size
                SHARED_REMINDERS[idx]
            }
        }
        _activeDialogue.value = dialogue
        return dialogue
    }

    /**
     * Records water consumption with strict duplicate-action prevention.
     * Returns true if successfully recorded, false if ignored as duplicate.
     */
    suspend fun recordWaterIntake(
        amountMl: Int,
        source: String,
        actionToken: String? = null
    ): Boolean {
        val now = System.currentTimeMillis()

        // 1. Check idempotency token (e.g. from notification actions)
        if (actionToken != null) {
            val previousTime = processedActionTokens[actionToken]
            if (previousTime != null && (now - previousTime) < 5000L) {
                Log.w(TAG, "Duplicate intake action blocked for token: $actionToken")
                return false
            }
            processedActionTokens[actionToken] = now
            // Prune old tokens
            if (processedActionTokens.size > 50) {
                processedActionTokens.entries.removeIf { (now - it.value) > 60000L }
            }
        }

        // 2. Persist to Room Database
        repository.recordIntake(amountMl, source)
        Log.i(TAG, "Recorded $amountMl ml water from source: $source")

        // 3. Reset reminder timing after intake (Smart Scheduling Section G)
        val s = repository.getSettingsDirect()
        if (s.enabled) {
            scheduler.scheduleNextReminder(s, baseTimestamp = now)
        }

        // 4. Check goal completion celebration
        val total = repository.getTodayTotalMlDirect()
        if (total >= s.dailyGoalMl && (total - amountMl) < s.dailyGoalMl) {
            _activeDialogue.value = CELEBRATION_DIALOGUES.random()
        }

        return true
    }

    /**
     * Snooze reminder for the configured snooze duration (default 15 mins).
     */
    fun snoozeReminder() {
        val s = settings.value
        val snoozeMillis = s.snoozeDurationMinutes * 60 * 1000L
        scheduler.scheduleSnooze(snoozeMillis)
    }

    /**
     * Temporarily pause reminders for a specified duration in hours.
     */
    fun pauseReminders(hours: Int) {
        scope.launch {
            val pausedUntil = System.currentTimeMillis() + (hours * 3600 * 1000L)
            val updated = settings.value.copy(pausedUntilTimestamp = pausedUntil)
            repository.updateSettings(updated)
            scheduler.scheduleNextReminder(updated, baseTimestamp = pausedUntil)
        }
    }

    /**
     * Reset today's intake history.
     */
    fun resetToday() {
        scope.launch {
            repository.resetTodayIntake()
        }
    }

    /**
     * Updates hydration reminder settings.
     */
    fun saveSettings(newSettings: HydrationSettingsEntity) {
        scope.launch {
            repository.updateSettings(newSettings)
        }
    }
}
