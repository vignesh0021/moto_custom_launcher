package com.motorola.launcher.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

/**
 * High-speed Full-Text Search virtual table backed by SQLite FTS.
 * Allows sub-2ms prefix searching across all installed applications and shortcuts.
 */
@Entity(tableName = "apps_fts")
@Fts4(contentEntity = AppEntity::class)
data class AppFtsEntity(
    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "package_name")
    val packageName: String
)
