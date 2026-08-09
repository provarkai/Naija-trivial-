package com.ai4biz.app.ai

import com.ai4biz.app.data.repository.ConversationRepository
import com.ai4biz.app.data.repository.MessageRepository
import com.ai4biz.app.model.Conversation
import com.ai4biz.app.model.Message
import com.ai4biz.app.model.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

private const val NEW_CONVERSATION_TITLE = "New conversation"
private const val MAX_HISTORY_TURNS = 10

/**
 * Orchestrates the AI Assistant (Phase 2 Sprints 5-6, see
 * docs/PHASE2_ARCHITECTURE.md): persistence + business context + the
 * actual [AssistantService] call, kept out of [com.ai4biz.app.ui.assistant.AssistantViewModel]
 * to keep it thin -- mirrors how [com.ai4biz.app.ui.generator.GeneratorViewModel]
 * doesn't do context-merging itself either.
 */
class AssistantRepository(
    private val assistantService: AssistantService,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val businessContextService: BusinessContextService
) {

    suspend fun getOrCreateConversation(workspaceId: String): Conversation =
        conversationRepository.getOrCreateForWorkspace(workspaceId)

    fun observeMessages(conversationId: String): Flow<List<Message>> =
        messageRepository.observeAllByConversation(conversationId)

    /**
     * Persists the user's message immediately, calls the assistant, then
     * persists its reply. On failure the user's message stays saved and
     * visible -- retry is just "type again," no lost input.
     */
    suspend fun sendMessage(conversation: Conversation, userText: String): Result<Message> {
        val now = System.currentTimeMillis()
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            conversationId = conversation.id,
            role = MessageRole.USER,
            content = userText,
            suggestedToolIds = emptyList(),
            createdAt = now
        )
        messageRepository.save(userMessage)

        if (conversation.title == NEW_CONVERSATION_TITLE) {
            conversationRepository.save(conversation.copy(title = userText.take(60), updatedAt = now))
        }

        val priorTurns = messageRepository.observeAllByConversation(conversation.id).first()
            .dropLast(1) // exclude the one just saved -- sent separately as `message`
            .takeLast(MAX_HISTORY_TURNS)
            .map { ConversationTurn(it.role, it.content) }

        val contextText = businessContextService.formatForAssistant(businessContextService.getContext())

        return assistantService.sendMessage(userText, priorTurns, contextText)
            .mapCatching { response ->
                val assistantMessage = Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversation.id,
                    role = MessageRole.ASSISTANT,
                    content = response.reply,
                    suggestedToolIds = response.suggestedTools.map { it.toolId },
                    createdAt = System.currentTimeMillis()
                )
                messageRepository.save(assistantMessage)
                conversationRepository.save(conversation.copy(updatedAt = System.currentTimeMillis()))
                assistantMessage
            }
    }
}
