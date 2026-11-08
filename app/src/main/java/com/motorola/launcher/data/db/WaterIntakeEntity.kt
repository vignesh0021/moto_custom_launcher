package com.motorola.launcher.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted entry of recorded water intake.
 * Supports dateKey indexing for high-performance sub-millisecond daily and weekly aggregations.
 */
@Entity(
    tableName = "water_intake",
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["timestamp"])
    ]
)
data class WaterIntakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "MANUAL", // "NOTIFICATION_200", "NOTIFICATION_250", "DASHBOARD_QUICK", "HOME_GLASS", "CUSTOM"
    val dateKey: String // Formatted "yyyy-MM-dd"
)
