package com.motorola.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationSettingsDao {

    @Query("SELECT * FROM hydration_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<HydrationSettingsEntity?>

    @Query("SELECT * FROM hydration_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): HydrationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: HydrationSettingsEntity)
}
