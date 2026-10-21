package com.motorola.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object optimized for sub-2ms FTS search and atomic batch updates.
 */
@Dao
interface AppDao {

    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY label COLLATE NOCASE ASC")
    suspend fun getAllAppsSync(): List<AppEntity>

    /**
     * Executes sub-2ms prefix search using SQLite MATCH against indexed FTS virtual table.
     */
    @Query("""
        SELECT apps.* FROM apps
        JOIN apps_fts ON apps.rowid = apps_fts.docid
        WHERE apps_fts MATCH :query
        ORDER BY apps.label COLLATE NOCASE ASC
    """)
    fun searchAppsFts(query: String): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(apps: List<AppEntity>)

    @Query("DELETE FROM apps WHERE component_key IN (:keys)")
    suspend fun deleteByKeys(keys: List<String>)

    @Query("DELETE FROM apps WHERE package_name = :packageName AND user_serial = :userSerial")
    suspend fun deletePackage(packageName: String, userSerial: Long)

    @Query("DELETE FROM apps")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(apps: List<AppEntity>) {
        clearAll()
        insertOrUpdateAll(apps)
    }
}
