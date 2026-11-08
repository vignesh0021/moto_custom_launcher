package com.motorola.launcher.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterIntakeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(intake: WaterIntakeEntity): Long

    @Query("SELECT * FROM water_intake WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getIntakesForDate(dateKey: String): Flow<List<WaterIntakeEntity>>

    @Query("SELECT SUM(amountMl) FROM water_intake WHERE dateKey = :dateKey")
    fun getDailyTotalMl(dateKey: String): Flow<Int?>

    @Query("SELECT SUM(amountMl) FROM water_intake WHERE dateKey = :dateKey")
    suspend fun getDailyTotalMlDirect(dateKey: String): Int?

    @Query("SELECT * FROM water_intake WHERE dateKey BETWEEN :startDateKey AND :endDateKey ORDER BY timestamp ASC")
    fun getIntakesBetweenDates(startDateKey: String, endDateKey: String): Flow<List<WaterIntakeEntity>>

    @Query("SELECT * FROM water_intake ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastIntakeDirect(): WaterIntakeEntity?

    @Query("SELECT * FROM water_intake ORDER BY timestamp DESC LIMIT 1")
    fun getLastIntake(): Flow<WaterIntakeEntity?>

    @Query("SELECT * FROM water_intake ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentIntakes(limit: Int): Flow<List<WaterIntakeEntity>>

    @Query("DELETE FROM water_intake WHERE dateKey = :dateKey")
    suspend fun deleteForDate(dateKey: String)

    @Query("DELETE FROM water_intake")
    suspend fun clearAll()
}
