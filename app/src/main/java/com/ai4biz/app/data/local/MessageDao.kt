package com.ai4biz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeAllByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE id = :id")
    fun observeById(id: String): Flow<MessageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Query("DELETE FROM ai_messages WHERE id = :id")
    suspend fun deleteById(id: String)
}
