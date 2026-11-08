package com.motorola.launcher.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted configuration for Smart Water Reminder and Hydration Companion.
 * Uses a single-row constraint (id = 1) for atomic updates and reactive flows.
 */
@Entity(tableName = "hydration_settings")
data class HydrationSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val dailyGoalMl: Int = 2000,
    val reminderIntervalMinutes: Int = 90,
    val startTime: String = "08:00", // 24-hour HH:mm
    val endTime: String = "22:00",   // 24-hour HH:mm
    val enabled: Boolean = true,
    val selectedCharacters: String = "BOTH", // "MITHRA", "AMMA", "BOTH"
    val language: String = "TAMIL", // "TAMIL", "TANGLISH", "ENGLISH"
    val voiceEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val snoozeDurationMinutes: Int = 15,
    val quietHoursEnabled: Boolean = true,
    val pausedUntilTimestamp: Long = 0L
)
