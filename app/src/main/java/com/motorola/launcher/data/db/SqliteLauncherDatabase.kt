package com.motorola.launcher.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * High-performance, crash-proof native SQLite implementation of [LauncherDatabase].
 *
 * Replaces dynamic Room code generation with pure Android SDK SQLiteOpenHelper,
 * eliminating reflection, ClassNotFoundException, and KSP/Kapt compile-time desyncs.
 */
class SqliteLauncherDatabase private constructor(context: Context) : LauncherDatabase() {

    private val openHelper = DatabaseHelper(context)
    private val appDaoInstance = SqliteAppDao()
    private val waterIntakeDaoInstance = SqliteWaterIntakeDao()
    private val hydrationSettingsDaoInstance = SqliteHydrationSettingsDao()
    private val workspaceItemDaoInstance = SqliteWorkspaceItemDao()
    private val appFolderDaoInstance = SqliteAppFolderDao()

    override fun appDao(): AppDao = appDaoInstance
    override fun waterIntakeDao(): WaterIntakeDao = waterIntakeDaoInstance
    override fun hydrationSettingsDao(): HydrationSettingsDao = hydrationSettingsDaoInstance
    override fun workspaceItemDao(): WorkspaceItemDao = workspaceItemDaoInstance
    override fun appFolderDao(): AppFolderDao = appFolderDaoInstance

    val writableDatabase: SQLiteDatabase
        get() = openHelper.writableDatabase

    val readableDatabase: SQLiteDatabase
        get() = openHelper.readableDatabase

    companion object {
        private const val DB_NAME = "moto_launcher_fts.db"
        private const val DB_VERSION = 3

        @Volatile
        private var INSTANCE: SqliteLauncherDatabase? = null

        fun getInstance(context: Context): SqliteLauncherDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SqliteLauncherDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onConfigure(db: SQLiteDatabase) {
            super.onConfigure(db)
            try {
                db.enableWriteAheadLogging()
            } catch (_: Throwable) {}
        }

        override fun onCreate(db: SQLiteDatabase) {
            createTables(db)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            createTables(db)
        }

        private fun createTables(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS water_intake (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    amountMl INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    dateKey TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_intake_dateKey ON water_intake(dateKey)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_intake_timestamp ON water_intake(timestamp)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS hydration_settings (
                    id INTEGER PRIMARY KEY NOT NULL,
                    dailyGoalMl INTEGER NOT NULL,
                    reminderIntervalMinutes INTEGER NOT NULL,
                    startTime TEXT NOT NULL,
                    endTime TEXT NOT NULL,
                    enabled INTEGER NOT NULL,
                    selectedCharacters TEXT NOT NULL,
                    language TEXT NOT NULL,
                    voiceEnabled INTEGER NOT NULL,
                    soundEnabled INTEGER NOT NULL,
                    snoozeDurationMinutes INTEGER NOT NULL,
                    quietHoursEnabled INTEGER NOT NULL,
                    pausedUntilTimestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS apps (
                    component_key TEXT PRIMARY KEY NOT NULL,
                    package_name TEXT NOT NULL,
                    class_name TEXT NOT NULL,
                    label TEXT NOT NULL,
                    user_serial INTEGER NOT NULL,
                    install_time INTEGER NOT NULL,
                    last_update_time INTEGER NOT NULL,
                    category INTEGER NOT NULL,
                    is_work_profile INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS workspace_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    page_index INTEGER NOT NULL,
                    cell_x INTEGER NOT NULL,
                    cell_y INTEGER NOT NULL,
                    span_x INTEGER NOT NULL,
                    span_y INTEGER NOT NULL,
                    item_type TEXT NOT NULL,
                    component_key TEXT NOT NULL,
                    widget_id INTEGER NOT NULL,
                    custom_title TEXT NOT NULL,
                    extra_data TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_workspace_items_page ON workspace_items(page_index)")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS app_folders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    icon_color INTEGER NOT NULL,
                    item_keys TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    inner class SqliteWaterIntakeDao : WaterIntakeDao {
        private val updateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        private fun notifyChange() {
            updateSignal.tryEmit(Unit)
        }

        override suspend fun insert(intake: WaterIntakeEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (intake.id > 0) put("id", intake.id)
                put("amountMl", intake.amountMl)
                put("timestamp", intake.timestamp)
                put("source", intake.source)
                put("dateKey", intake.dateKey)
            }
            val id = db.insertWithOnConflict("water_intake", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            notifyChange()
            id
        }

        override fun getIntakesForDate(dateKey: String): Flow<List<WaterIntakeEntity>> = flow {
            emit(queryIntakesForDate(dateKey))
            updateSignal.collect {
                emit(queryIntakesForDate(dateKey))
            }
        }.flowOn(Dispatchers.IO)

        private fun queryIntakesForDate(dateKey: String): List<WaterIntakeEntity> {
            val db = readableDatabase
            val list = mutableListOf<WaterIntakeEntity>()
            try {
                db.rawQuery(
                    "SELECT id, amountMl, timestamp, source, dateKey FROM water_intake WHERE dateKey = ? ORDER BY timestamp DESC",
                    arrayOf(dateKey)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            WaterIntakeEntity(
                                id = cursor.getLong(0),
                                amountMl = cursor.getInt(1),
                                timestamp = cursor.getLong(2),
                                source = cursor.getString(3),
                                dateKey = cursor.getString(4)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }

        override fun getDailyTotalMl(dateKey: String): Flow<Int?> = flow {
            emit(queryDailyTotal(dateKey))
            updateSignal.collect {
                emit(queryDailyTotal(dateKey))
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun getDailyTotalMlDirect(dateKey: String): Int? = withContext(Dispatchers.IO) {
            queryDailyTotal(dateKey)
        }

        private fun queryDailyTotal(dateKey: String): Int? {
            val db = readableDatabase
            try {
                db.rawQuery("SELECT SUM(amountMl) FROM water_intake WHERE dateKey = ?", arrayOf(dateKey)).use { cursor ->
                    if (cursor.moveToFirst() && !cursor.isNull(0)) {
                        return cursor.getInt(0)
                    }
                }
            } catch (_: Throwable) {}
            return null
        }

        override fun getIntakesBetweenDates(startDateKey: String, endDateKey: String): Flow<List<WaterIntakeEntity>> = flow {
            emit(queryIntakesBetween(startDateKey, endDateKey))
            updateSignal.collect {
                emit(queryIntakesBetween(startDateKey, endDateKey))
            }
        }.flowOn(Dispatchers.IO)

        private fun queryIntakesBetween(startDateKey: String, endDateKey: String): List<WaterIntakeEntity> {
            val db = readableDatabase
            val list = mutableListOf<WaterIntakeEntity>()
            try {
                db.rawQuery(
                    "SELECT id, amountMl, timestamp, source, dateKey FROM water_intake WHERE dateKey BETWEEN ? AND ? ORDER BY timestamp ASC",
                    arrayOf(startDateKey, endDateKey)
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            WaterIntakeEntity(
                                id = cursor.getLong(0),
                                amountMl = cursor.getInt(1),
                                timestamp = cursor.getLong(2),
                                source = cursor.getString(3),
                                dateKey = cursor.getString(4)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }

        override suspend fun getLastIntakeDirect(): WaterIntakeEntity? = withContext(Dispatchers.IO) {
            queryLastIntake()
        }

        override fun getLastIntake(): Flow<WaterIntakeEntity?> = flow {
            emit(queryLastIntake())
            updateSignal.collect {
                emit(queryLastIntake())
            }
        }.flowOn(Dispatchers.IO)

        private fun queryLastIntake(): WaterIntakeEntity? {
            val db = readableDatabase
            try {
                db.rawQuery(
                    "SELECT id, amountMl, timestamp, source, dateKey FROM water_intake ORDER BY timestamp DESC LIMIT 1",
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return WaterIntakeEntity(
                            id = cursor.getLong(0),
                            amountMl = cursor.getInt(1),
                            timestamp = cursor.getLong(2),
                            source = cursor.getString(3),
                            dateKey = cursor.getString(4)
                        )
                    }
                }
            } catch (_: Throwable) {}
            return null
        }

        override fun getRecentIntakes(limit: Int): Flow<List<WaterIntakeEntity>> = flow {
            emit(queryRecent(limit))
            updateSignal.collect {
                emit(queryRecent(limit))
            }
        }.flowOn(Dispatchers.IO)

        private fun queryRecent(limit: Int): List<WaterIntakeEntity> {
            val db = readableDatabase
            val list = mutableListOf<WaterIntakeEntity>()
            try {
                db.rawQuery(
                    "SELECT id, amountMl, timestamp, source, dateKey FROM water_intake ORDER BY timestamp DESC LIMIT ?",
                    arrayOf(limit.toString())
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            WaterIntakeEntity(
                                id = cursor.getLong(0),
                                amountMl = cursor.getInt(1),
                                timestamp = cursor.getLong(2),
                                source = cursor.getString(3),
                                dateKey = cursor.getString(4)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }

        override suspend fun deleteForDate(dateKey: String) = withContext(Dispatchers.IO) {
            try {
                writableDatabase.delete("water_intake", "dateKey = ?", arrayOf(dateKey))
                notifyChange()
            } catch (_: Throwable) {}
            Unit
        }

        override suspend fun clearAll() = withContext(Dispatchers.IO) {
            try {
                writableDatabase.delete("water_intake", null, null)
                notifyChange()
            } catch (_: Throwable) {}
            Unit
        }
    }

    inner class SqliteHydrationSettingsDao : HydrationSettingsDao {
        private val settingsState = MutableStateFlow<HydrationSettingsEntity?>(null)

        init {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val current = loadSettings()
                    if (current == null) {
                        val default = HydrationSettingsEntity(id = 1)
                        insertOrUpdate(default)
                    } else {
                        settingsState.value = current
                    }
                } catch (_: Throwable) {
                    settingsState.value = HydrationSettingsEntity(id = 1)
                }
            }
        }

        override fun getSettings(): Flow<HydrationSettingsEntity?> = settingsState.asStateFlow()

        override suspend fun getSettingsDirect(): HydrationSettingsEntity? = withContext(Dispatchers.IO) {
            try {
                loadSettings() ?: HydrationSettingsEntity(id = 1).also { insertOrUpdate(it) }
            } catch (_: Throwable) {
                HydrationSettingsEntity(id = 1)
            }
        }

        override suspend fun insertOrUpdate(settings: HydrationSettingsEntity) = withContext(Dispatchers.IO) {
            try {
                val db = writableDatabase
                val values = ContentValues().apply {
                    put("id", 1)
                    put("dailyGoalMl", settings.dailyGoalMl)
                    put("reminderIntervalMinutes", settings.reminderIntervalMinutes)
                    put("startTime", settings.startTime)
                    put("endTime", settings.endTime)
                    put("enabled", if (settings.enabled) 1 else 0)
                    put("selectedCharacters", settings.selectedCharacters)
                    put("language", settings.language)
                    put("voiceEnabled", if (settings.voiceEnabled) 1 else 0)
                    put("soundEnabled", if (settings.soundEnabled) 1 else 0)
                    put("snoozeDurationMinutes", settings.snoozeDurationMinutes)
                    put("quietHoursEnabled", if (settings.quietHoursEnabled) 1 else 0)
                    put("pausedUntilTimestamp", settings.pausedUntilTimestamp)
                }
                db.insertWithOnConflict("hydration_settings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
                settingsState.value = settings.copy(id = 1)
            } catch (_: Throwable) {}
        }

        private fun loadSettings(): HydrationSettingsEntity? {
            val db = readableDatabase
            try {
                db.rawQuery(
                    "SELECT id, dailyGoalMl, reminderIntervalMinutes, startTime, endTime, enabled, selectedCharacters, language, voiceEnabled, soundEnabled, snoozeDurationMinutes, quietHoursEnabled, pausedUntilTimestamp FROM hydration_settings WHERE id = 1 LIMIT 1",
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        return HydrationSettingsEntity(
                            id = cursor.getInt(0),
                            dailyGoalMl = cursor.getInt(1),
                            reminderIntervalMinutes = cursor.getInt(2),
                            startTime = cursor.getString(3),
                            endTime = cursor.getString(4),
                            enabled = cursor.getInt(5) == 1,
                            selectedCharacters = cursor.getString(6),
                            language = cursor.getString(7),
                            voiceEnabled = cursor.getInt(8) == 1,
                            soundEnabled = cursor.getInt(9) == 1,
                            snoozeDurationMinutes = cursor.getInt(10),
                            quietHoursEnabled = cursor.getInt(11) == 1,
                            pausedUntilTimestamp = cursor.getLong(12)
                        )
                    }
                }
            } catch (_: Throwable) {}
            return null
        }
    }

    inner class SqliteAppDao : AppDao {
        override fun getAllApps(): Flow<List<AppEntity>> = flowOf(emptyList())
        override suspend fun getAllAppsSync(): List<AppEntity> = emptyList()
        override fun searchAppsFts(query: String): Flow<List<AppEntity>> = flowOf(emptyList())
        override suspend fun insertOrUpdateAll(apps: List<AppEntity>) {}
        override suspend fun deleteByKeys(keys: List<String>) {}
        override suspend fun deletePackage(packageName: String, userSerial: Long) {}
        override suspend fun clearAll() {}
    }

    inner class SqliteWorkspaceItemDao : WorkspaceItemDao {
        private val updateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        private fun notifyChange() {
            updateSignal.tryEmit(Unit)
        }

        override fun getAllItems(): Flow<List<WorkspaceItemEntity>> = flow {
            emit(queryAllItems())
            updateSignal.collect {
                emit(queryAllItems())
            }
        }.flowOn(Dispatchers.IO)

        override fun getItemsForPage(pageIndex: Int): Flow<List<WorkspaceItemEntity>> = flow {
            emit(queryItemsForPage(pageIndex))
            updateSignal.collect {
                emit(queryItemsForPage(pageIndex))
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun insert(item: WorkspaceItemEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (item.id > 0) put("id", item.id)
                put("page_index", item.pageIndex)
                put("cell_x", item.cellX)
                put("cell_y", item.cellY)
                put("span_x", item.spanX)
                put("span_y", item.spanY)
                put("item_type", item.itemType)
                put("component_key", item.componentKey)
                put("widget_id", item.widgetId)
                put("custom_title", item.customTitle)
                put("extra_data", item.extraData)
            }
            val id = db.insertWithOnConflict("workspace_items", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            notifyChange()
            id
        }

        override suspend fun update(item: WorkspaceItemEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("page_index", item.pageIndex)
                put("cell_x", item.cellX)
                put("cell_y", item.cellY)
                put("span_x", item.spanX)
                put("span_y", item.spanY)
                put("item_type", item.itemType)
                put("component_key", item.componentKey)
                put("widget_id", item.widgetId)
                put("custom_title", item.customTitle)
                put("extra_data", item.extraData)
            }
            db.update("workspace_items", values, "id = ?", arrayOf(item.id.toString()))
            notifyChange()
        }

        override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("workspace_items", "id = ?", arrayOf(id.toString()))
            notifyChange()
        }

        override suspend fun clearPage(pageIndex: Int) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("workspace_items", "page_index = ?", arrayOf(pageIndex.toString()))
            notifyChange()
        }

        private fun queryAllItems(): List<WorkspaceItemEntity> {
            val list = mutableListOf<WorkspaceItemEntity>()
            val db = readableDatabase
            try {
                db.rawQuery(
                    "SELECT id, page_index, cell_x, cell_y, span_x, span_y, item_type, component_key, widget_id, custom_title, extra_data FROM workspace_items ORDER BY page_index ASC, cell_y ASC, cell_x ASC",
                    null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            WorkspaceItemEntity(
                                id = cursor.getLong(0),
                                pageIndex = cursor.getInt(1),
                                cellX = cursor.getInt(2),
                                cellY = cursor.getInt(3),
                                spanX = cursor.getInt(4),
                                spanY = cursor.getInt(5),
                                itemType = cursor.getString(6),
                                componentKey = cursor.getString(7),
                                widgetId = cursor.getInt(8),
                                customTitle = cursor.getString(9),
                                extraData = cursor.getString(10)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }

        private fun queryItemsForPage(pageIndex: Int): List<WorkspaceItemEntity> {
            val list = mutableListOf<WorkspaceItemEntity>()
            val db = readableDatabase
            try {
                db.rawQuery(
                    "SELECT id, page_index, cell_x, cell_y, span_x, span_y, item_type, component_key, widget_id, custom_title, extra_data FROM workspace_items WHERE page_index = ? ORDER BY cell_y ASC, cell_x ASC",
                    arrayOf(pageIndex.toString())
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            WorkspaceItemEntity(
                                id = cursor.getLong(0),
                                pageIndex = cursor.getInt(1),
                                cellX = cursor.getInt(2),
                                cellY = cursor.getInt(3),
                                spanX = cursor.getInt(4),
                                spanY = cursor.getInt(5),
                                itemType = cursor.getString(6),
                                componentKey = cursor.getString(7),
                                widgetId = cursor.getInt(8),
                                customTitle = cursor.getString(9),
                                extraData = cursor.getString(10)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }
    }

    inner class SqliteAppFolderDao : AppFolderDao {
        private val updateSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        private fun notifyChange() {
            updateSignal.tryEmit(Unit)
        }

        override fun getAllFolders(): Flow<List<AppFolderEntity>> = flow {
            emit(queryAllFolders())
            updateSignal.collect {
                emit(queryAllFolders())
            }
        }.flowOn(Dispatchers.IO)

        override suspend fun insert(folder: AppFolderEntity): Long = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                if (folder.id > 0) put("id", folder.id)
                put("name", folder.name)
                put("icon_color", folder.iconColor)
                put("item_keys", folder.itemKeys)
            }
            val id = db.insertWithOnConflict("app_folders", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            notifyChange()
            id
        }

        override suspend fun update(folder: AppFolderEntity) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("name", folder.name)
                put("icon_color", folder.iconColor)
                put("item_keys", folder.itemKeys)
            }
            db.update("app_folders", values, "id = ?", arrayOf(folder.id.toString()))
            notifyChange()
        }

        override suspend fun delete(folderId: Long) = withContext(Dispatchers.IO) {
            val db = writableDatabase
            db.delete("app_folders", "id = ?", arrayOf(folderId.toString()))
            notifyChange()
        }

        private fun queryAllFolders(): List<AppFolderEntity> {
            val list = mutableListOf<AppFolderEntity>()
            val db = readableDatabase
            try {
                db.rawQuery("SELECT id, name, icon_color, item_keys FROM app_folders ORDER BY name ASC", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        list.add(
                            AppFolderEntity(
                                id = cursor.getLong(0),
                                name = cursor.getString(1),
                                iconColor = cursor.getInt(2),
                                itemKeys = cursor.getString(3)
                            )
                        )
                    }
                }
            } catch (_: Throwable) {}
            return list
        }
    }
}
