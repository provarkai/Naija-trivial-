package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE id = :id")
    fun observeById(id: String): Flow<WorkspaceEntity?>

    @Query("SELECT * FROM workspaces WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<WorkspaceEntity?>

    /**
     * Non-Flow read used by [com.ai4biz.app.data.repository.WorkspaceRepository.getOrCreateDefaultWorkspace]
     * -- a one-shot check on app start, not an observed UI state.
     */
    @Query("SELECT * FROM workspaces WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveOnce(): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteById(id: String)
}
