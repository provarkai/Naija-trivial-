package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM ai_conversations WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC")
    fun observeAllByWorkspace(workspaceId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    fun observeById(id: String): Flow<ConversationEntity?>

    /**
     * Non-Flow read used by [com.ai4biz.app.data.repository.ConversationRepository.getOrCreateForWorkspace]
     * -- a one-shot check, not an observed UI state. Mirrors [WorkspaceDao.getActiveOnce].
     */
    @Query("SELECT * FROM ai_conversations WHERE workspaceId = :workspaceId ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestByWorkspaceOnce(workspaceId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteById(id: String)
}
