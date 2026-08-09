package com.ai4biz.app.data.repository

import com.ai4biz.app.data.local.ConversationDao
import com.ai4biz.app.data.local.ConversationEntity
import com.ai4biz.app.model.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ConversationRepository(private val dao: ConversationDao) {

    fun observeAllByWorkspace(workspaceId: String): Flow<List<Conversation>> =
        dao.observeAllByWorkspace(workspaceId).map { entities -> entities.map { it.toDomain() } }

    fun observe(id: String): Flow<Conversation?> =
        dao.observeById(id).map { it?.toDomain() }

    suspend fun save(conversation: Conversation) {
        dao.upsert(conversation.toEntity())
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    /**
     * Returns the most recently updated conversation for [workspaceId] if
     * one exists, otherwise creates and returns a new empty one. Mirrors
     * [WorkspaceRepository.getOrCreateDefaultWorkspace] -- a workspace has
     * exactly one conversation today (no switcher UI), so this is always
     * "the" conversation.
     */
    suspend fun getOrCreateForWorkspace(workspaceId: String): Conversation {
        dao.getLatestByWorkspaceOnce(workspaceId)?.let { return it.toDomain() }

        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            workspaceId = workspaceId,
            title = "New conversation",
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(conversation.toEntity())
        return conversation
    }

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        workspaceId = workspaceId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Conversation.toEntity() = ConversationEntity(
        id = id,
        workspaceId = workspaceId,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
