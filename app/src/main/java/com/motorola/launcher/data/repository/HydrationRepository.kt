package com.motorola.launcher.data.repository

import android.content.Context
import com.motorola.launcher.data.db.HydrationSettingsDao
import com.motorola.launcher.data.db.HydrationSettingsEntity
import com.motorola.launcher.data.db.LauncherDatabase
import com.motorola.launcher.data.db.WaterIntakeDao
import com.motorola.launcher.data.db.WaterIntakeEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Repository for managing water intake tracking and reminder settings.
 * Backed by Room SQLite database.
 */
class HydrationRepository(
    private val waterIntakeDao: WaterIntakeDao,
    private val settingsDao: HydrationSettingsDao
) {
    companion object {
        @Volatile
        private var INSTANCE: HydrationRepository? = null

        fun getInstance(context: Context): HydrationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val db = LauncherDatabase.getInstance(context)
                    HydrationRepository(db.waterIntakeDao(), db.hydrationSettingsDao()).also {
                        INSTANCE = it
                    }
                }
            }
        }

        fun getTodayDateKey(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun getDateKey(timestamp: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(timestamp))
        }

        fun getPastDaysDateKeys(days: Int): List<String> {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val list = mutableListOf<String>()
            val cal = Calendar.getInstance()
            for (i in (days - 1) downTo 0) {
                val tempCal = Calendar.getInstance()
                tempCal.add(Calendar.DAY_OF_YEAR, -i)
                list.add(sdf.format(tempCal.time))
            }
            return list
        }
    }

    val todayTotalMl: Flow<Int> = waterIntakeDao.getDailyTotalMl(getTodayDateKey())
        .map { it ?: 0 }

    val todayEntries: Flow<List<WaterIntakeEntity>> = waterIntakeDao.getIntakesForDate(getTodayDateKey())

    val settings: Flow<HydrationSettingsEntity> = settingsDao.getSettings()
        .map { it ?: HydrationSettingsEntity() }

    val lastIntake: Flow<WaterIntakeEntity?> = waterIntakeDao.getLastIntake()

    suspend fun getSettingsDirect(): HydrationSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettingsDirect() ?: HydrationSettingsEntity().also {
            settingsDao.insertOrUpdate(it)
        }
    }

    suspend fun getTodayTotalMlDirect(): Int = withContext(Dispatchers.IO) {
        waterIntakeDao.getDailyTotalMlDirect(getTodayDateKey()) ?: 0
    }

    suspend fun getLastIntakeDirect(): WaterIntakeEntity? = withContext(Dispatchers.IO) {
        waterIntakeDao.getLastIntakeDirect()
    }

    suspend fun recordIntake(amountMl: Int, source: String): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entry = WaterIntakeEntity(
            amountMl = amountMl,
            timestamp = now,
            source = source,
            dateKey = getTodayDateKey()
        )
        waterIntakeDao.insert(entry)
    }

    suspend fun updateSettings(settings: HydrationSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.insertOrUpdate(settings)
    }

    suspend fun resetTodayIntake() = withContext(Dispatchers.IO) {
        waterIntakeDao.deleteForDate(getTodayDateKey())
    }

    fun getWeeklyIntakes(startDateKey: String, endDateKey: String): Flow<List<WaterIntakeEntity>> {
        return waterIntakeDao.getIntakesBetweenDates(startDateKey, endDateKey)
    }
}
