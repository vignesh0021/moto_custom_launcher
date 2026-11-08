package com.motorola.launcher.hydration

import com.motorola.launcher.data.db.HydrationSettingsEntity
import com.motorola.launcher.data.db.WaterIntakeEntity
import com.motorola.launcher.system.companion.CompanionCharacter
import com.motorola.launcher.system.companion.CompanionContextManager
import com.motorola.launcher.system.companion.FamilyCompanionOrchestrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HydrationReminderSystemTest {

    @Test
    fun testWaterIntakeCalculations() {
        val dailyGoal = 2000
        val entries = listOf(
            WaterIntakeEntity(amountMl = 200, dateKey = "2026-11-08"),
            WaterIntakeEntity(amountMl = 250, dateKey = "2026-11-08"),
            WaterIntakeEntity(amountMl = 300, dateKey = "2026-11-08")
        )

        val totalConsumed = entries.sumOf { it.amountMl }
        assertEquals(750, totalConsumed)

        val remaining = (dailyGoal - totalConsumed).coerceAtLeast(0)
        assertEquals(1250, remaining)

        val progressRatio = totalConsumed.toFloat() / dailyGoal
        assertEquals(0.375f, progressRatio, 0.001f)

        val progressPercentage = (progressRatio * 100).toInt()
        assertEquals(37, progressPercentage)

        val glassesCount = totalConsumed / 250
        assertEquals(3, glassesCount)
    }

    @Test
    fun testGoalCompletionBoundary() {
        val dailyGoal = 2000
        val consumedUnder = 1950
        val consumedEqual = 2000
        val consumedOver = 2250

        assertFalse(consumedUnder >= dailyGoal)
        assertTrue(consumedEqual >= dailyGoal)
        assertTrue(consumedOver >= dailyGoal)

        val remainingUnder = (dailyGoal - consumedUnder).coerceAtLeast(0)
        val remainingOver = (dailyGoal - consumedOver).coerceAtLeast(0)

        assertEquals(50, remainingUnder)
        assertEquals(0, remainingOver)
    }

    @Test
    fun testQuietHoursWakingWindow() {
        val contextManager = CompanionContextManager(createMockContext())

        // 1. Daytime Window: 08:00 to 22:00
        val startTime = "08:00"
        val endTime = "22:00"

        val cal10am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        assertTrue(contextManager.isWithinWakingHours(startTime, endTime, cal10am))

        val cal3am = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 3)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(contextManager.isWithinWakingHours(startTime, endTime, cal3am))

        val cal11pm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 15)
        }
        assertFalse(contextManager.isWithinWakingHours(startTime, endTime, cal11pm))

        // 2. Overnight Window: 21:00 to 06:00
        val nightStart = "21:00"
        val morningEnd = "06:00"

        val calMidnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
        }
        assertTrue(contextManager.isWithinWakingHours(nightStart, morningEnd, calMidnight))

        val calNoon = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
        }
        assertFalse(contextManager.isWithinWakingHours(nightStart, morningEnd, calNoon))
    }

    @Test
    fun testRecentDrinkCooldown() {
        val contextManager = CompanionContextManager(createMockContext())
        val settings = HydrationSettingsEntity(
            enabled = true,
            quietHoursEnabled = false
        )

        val now = System.currentTimeMillis()

        // 10 minutes ago -> should suppress
        val drank10mAgo = now - (10 * 60 * 1000L)
        assertFalse(contextManager.shouldAllowReminder(settings, drank10mAgo))

        // 45 minutes ago -> should allow
        val drank45mAgo = now - (45 * 60 * 1000L)
        assertTrue(contextManager.shouldAllowReminder(settings, drank45mAgo))

        // Never drank today -> should allow
        assertTrue(contextManager.shouldAllowReminder(settings, null))
    }

    @Test
    fun testTemporaryPauseSuppression() {
        val contextManager = CompanionContextManager(createMockContext())
        val now = System.currentTimeMillis()

        val pausedSettings = HydrationSettingsEntity(
            enabled = true,
            quietHoursEnabled = false,
            pausedUntilTimestamp = now + (2 * 3600 * 1000L)
        )
        assertFalse(contextManager.shouldAllowReminder(pausedSettings, null))

        val expiredPauseSettings = HydrationSettingsEntity(
            enabled = true,
            quietHoursEnabled = false,
            pausedUntilTimestamp = now - 1000L
        )
        assertTrue(contextManager.shouldAllowReminder(expiredPauseSettings, null))
    }

    @Test
    fun testNextReminderAdvancementAcrossSleepWindow() {
        val contextManager = CompanionContextManager(createMockContext())
        val settings = HydrationSettingsEntity(
            enabled = true,
            startTime = "08:00",
            endTime = "22:00",
            quietHoursEnabled = true
        )

        // If base is 21:50 and interval is 90 mins -> target is 23:20 (sleep hours)
        // Should advance to next morning 08:00
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 50)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextTimestamp = contextManager.calculateNextReminderTime(
            settings = settings,
            intervalMinutes = 90,
            baseTimestamp = baseCal.timeInMillis
        )

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextTimestamp }
        assertEquals(8, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testCharacterDialoguePools() {
        // Section B Dialogues
        assertTrue(FamilyCompanionOrchestrator.MITHRA_REMINDERS.size >= 4)
        assertTrue(FamilyCompanionOrchestrator.AMMA_REMINDERS.size >= 4)
        assertTrue(FamilyCompanionOrchestrator.SHARED_REMINDERS.isNotEmpty())

        val mithraDialogue1 = FamilyCompanionOrchestrator.MITHRA_REMINDERS[0]
        assertEquals("அப்பா! தண்ணி குடிச்சீங்களா?", mithraDialogue1.tamilText)
        assertEquals(CompanionCharacter.MITHRA, mithraDialogue1.character)

        val ammaDialogue1 = FamilyCompanionOrchestrator.AMMA_REMINDERS[0]
        assertEquals("ஏங்க, கொஞ்சம் தண்ணி குடிச்சிட்டு வேலையைப் பாருங்க.", ammaDialogue1.tamilText)
        assertEquals(CompanionCharacter.AMMA, ammaDialogue1.character)

        val sharedDialogue = FamilyCompanionOrchestrator.SHARED_REMINDERS[0]
        assertTrue(sharedDialogue.tamilText.contains("அம்மா, அப்பா தண்ணி குடிச்சாங்களா?"))
        assertEquals(CompanionCharacter.SHARED, sharedDialogue.character)
    }

    @Test
    fun testWeeklyAggregationLogic() {
        val goal = 2000
        val entries = listOf(
            WaterIntakeEntity(amountMl = 250, dateKey = "2026-11-02"),
            WaterIntakeEntity(amountMl = 500, dateKey = "2026-11-02"),
            WaterIntakeEntity(amountMl = 2000, dateKey = "2026-11-03"),
            WaterIntakeEntity(amountMl = 750, dateKey = "2026-11-04")
        )

        val grouped = entries.groupBy { it.dateKey }
            .mapValues { (_, list) -> list.sumOf { it.amountMl } }

        assertEquals(750, grouped["2026-11-02"])
        assertEquals(2000, grouped["2026-11-03"])
        assertEquals(750, grouped["2026-11-04"])
    }

    private fun createMockContext(): android.content.Context {
        // Return a mock or proxy Context for unit tests
        return java.lang.reflect.Proxy.newProxyInstance(
            android.content.Context::class.java.classLoader,
            arrayOf(android.content.Context::class.java)
        ) { _, _, _ -> null } as android.content.Context
    }
}
