package com.motorola.launcher.data.db

import kotlinx.coroutines.flow.Flow

interface WorkspaceItemDao {
    fun getAllItems(): Flow<List<WorkspaceItemEntity>>
    fun getItemsForPage(pageIndex: Int): Flow<List<WorkspaceItemEntity>>
    suspend fun insert(item: WorkspaceItemEntity): Long
    suspend fun update(item: WorkspaceItemEntity)
    suspend fun delete(id: Long)
    suspend fun clearPage(pageIndex: Int)
}
