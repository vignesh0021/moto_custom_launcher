package com.motorola.launcher.data.repository

import android.content.Context
import com.motorola.launcher.data.db.AppFolderDao
import com.motorola.launcher.data.db.AppFolderEntity
import com.motorola.launcher.data.db.LauncherDatabase
import com.motorola.launcher.data.db.WorkspaceItemDao
import com.motorola.launcher.data.db.WorkspaceItemEntity
import com.motorola.launcher.ui.model.AppItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages home screen multi-page workspace items and app drawer folders.
 * Supports native Android app widgets, cyber HUD widgets, folders, and app shortcuts.
 */
class WorkspaceRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val db = LauncherDatabase.getInstance(context)
    private val workspaceDao: WorkspaceItemDao = db.workspaceItemDao()
    private val folderDao: AppFolderDao = db.appFolderDao()

    val workspaceItems: StateFlow<List<WorkspaceItemEntity>> = workspaceDao.getAllItems()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val appFolders: StateFlow<List<AppFolderEntity>> = folderDao.getAllFolders()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun getItemsForPage(pageIndex: Int) = workspaceDao.getItemsForPage(pageIndex)

    suspend fun addWorkspaceItem(
        pageIndex: Int,
        cellX: Int,
        cellY: Int,
        spanX: Int = 1,
        spanY: Int = 1,
        itemType: String,
        componentKey: String = "",
        widgetId: Int = -1,
        customTitle: String = "",
        extraData: String = ""
    ): Long = withContext(Dispatchers.IO) {
        workspaceDao.insert(
            WorkspaceItemEntity(
                pageIndex = pageIndex,
                cellX = cellX,
                cellY = cellY,
                spanX = spanX,
                spanY = spanY,
                itemType = itemType,
                componentKey = componentKey,
                widgetId = widgetId,
                customTitle = customTitle,
                extraData = extraData
            )
        )
    }

    suspend fun updateWorkspaceItem(item: WorkspaceItemEntity) = withContext(Dispatchers.IO) {
        workspaceDao.update(item)
    }

    suspend fun removeWorkspaceItem(id: Long) = withContext(Dispatchers.IO) {
        workspaceDao.delete(id)
    }

    suspend fun clearPage(pageIndex: Int) = withContext(Dispatchers.IO) {
        workspaceDao.clearPage(pageIndex)
    }

    suspend fun createFolder(name: String, color: Int = 0, initialKeys: List<String> = emptyList()): Long = withContext(Dispatchers.IO) {
        folderDao.insert(
            AppFolderEntity(
                name = name,
                iconColor = color,
                itemKeys = initialKeys.joinToString(",")
            )
        )
    }

    suspend fun updateFolder(folder: AppFolderEntity) = withContext(Dispatchers.IO) {
        folderDao.update(folder)
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        folderDao.delete(folderId)
    }

    suspend fun addAppToFolder(folderId: Long, componentKey: String) = withContext(Dispatchers.IO) {
        val folder = appFolders.value.firstOrNull { it.id == folderId } ?: return@withContext
        folderDao.update(folder.withAddedKey(componentKey))
    }

    suspend fun removeAppFromFolder(folderId: Long, componentKey: String) = withContext(Dispatchers.IO) {
        val folder = appFolders.value.firstOrNull { it.id == folderId } ?: return@withContext
        folderDao.update(folder.withRemovedKey(componentKey))
    }

    /**
     * Seeds initial workspace items if the database is currently empty.
     * Page 0: Productivity (Cyber Weather & Calendar widgets + quick apps)
     * Page 1: Marvel Command Core (Digital Clock, Arc Reactor, Dock - rendered natively)
     * Page 2: Media & Entertainment (Cyber Media Widget + Mini Reactor & Hydration)
     */
    suspend fun seedDefaultsIfEmpty(installedApps: List<AppItem>) = withContext(Dispatchers.IO) {
        if (workspaceItems.value.isNotEmpty()) return@withContext

        // Page 0: Overview & Productivity
        addWorkspaceItem(
            pageIndex = 0,
            cellX = 0,
            cellY = 0,
            spanX = 5,
            spanY = 2,
            itemType = WorkspaceItemEntity.TYPE_WIDGET_BUILTIN,
            extraData = WorkspaceItemEntity.BUILTIN_WEATHER,
            customTitle = "SECTOR WEATHER"
        )
        addWorkspaceItem(
            pageIndex = 0,
            cellX = 0,
            cellY = 2,
            spanX = 5,
            spanY = 2,
            itemType = WorkspaceItemEntity.TYPE_WIDGET_BUILTIN,
            extraData = WorkspaceItemEntity.BUILTIN_CALENDAR,
            customTitle = "MISSION AGENDA"
        )

        // Seed up to 5 apps on Page 0 row 4
        installedApps.take(5).forEachIndexed { index, app ->
            addWorkspaceItem(
                pageIndex = 0,
                cellX = index,
                cellY = 4,
                spanX = 1,
                spanY = 1,
                itemType = WorkspaceItemEntity.TYPE_APP,
                componentKey = app.componentKey,
                customTitle = app.label
            )
        }

        // Page 2: Media & Entertainment
        addWorkspaceItem(
            pageIndex = 2,
            cellX = 0,
            cellY = 0,
            spanX = 5,
            spanY = 2,
            itemType = WorkspaceItemEntity.TYPE_WIDGET_BUILTIN,
            extraData = WorkspaceItemEntity.BUILTIN_MEDIA,
            customTitle = "STARK AUDIO SYSTEM"
        )
        addWorkspaceItem(
            pageIndex = 2,
            cellX = 0,
            cellY = 2,
            spanX = 5,
            spanY = 2,
            itemType = WorkspaceItemEntity.TYPE_WIDGET_BUILTIN,
            extraData = WorkspaceItemEntity.BUILTIN_TELEMETRY,
            customTitle = "QUANTUM CORE TELEMETRY"
        )

        // Seed default folders
        if (appFolders.value.isEmpty()) {
            val games = installedApps.filter { it.label.contains("game", true) || it.packageName.contains("game", true) }
            if (games.isNotEmpty()) {
                createFolder("Games", 0xFFE53935.toInt(), games.map { it.componentKey })
            }
            val tools = installedApps.filter {
                it.packageName.contains("chrome", true) ||
                it.packageName.contains("settings", true) ||
                it.packageName.contains("calc", true) ||
                it.packageName.contains("camera", true)
            }
            if (tools.isNotEmpty()) {
                createFolder("Tools", 0xFF00E5FF.toInt(), tools.map { it.componentKey })
            }
        }
    }
}
