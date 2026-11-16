package com.motorola.launcher.data.db

import kotlinx.coroutines.flow.Flow

interface AppFolderDao {
    fun getAllFolders(): Flow<List<AppFolderEntity>>
    suspend fun insert(folder: AppFolderEntity): Long
    suspend fun update(folder: AppFolderEntity)
    suspend fun delete(folderId: Long)
}
