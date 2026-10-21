package com.motorola.launcher.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable entity representing an installed launcher activity.
 */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey
    @ColumnInfo(name = "component_key")
    val componentKey: String, // "package/activity#userSerial"

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    @ColumnInfo(name = "user_serial")
    val userSerial: Long,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "install_time")
    val installTime: Long,

    @ColumnInfo(name = "icon_cache_key")
    val iconCacheKey: String
)
