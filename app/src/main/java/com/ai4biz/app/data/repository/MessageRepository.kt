package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.MessageDao
import com.ai4biz.app.data.local.MessageEntity
import com.ai4biz.app.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(private val dao: MessageDao) {

    fun observeAllByConversation(conversationId: String): Flow<List<Message>> =
        dao.observeAllByConversation(conversationId).map { entities -> entities.map { it.toDomain() } }

    suspend fun save(message: Message) {
        dao.upsert(message.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun MessageEntity.toDomain() = Message(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        suggestedToolIds = suggestedToolIds
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        createdAt = createdAt
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        suggestedToolIds = suggestedToolIds.takeIf { it.isNotEmpty() }?.joinToString(","),
        createdAt = createdAt
    )
}
