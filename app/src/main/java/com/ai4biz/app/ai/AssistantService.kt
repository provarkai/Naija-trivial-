package com.ai4biz.app.ai

import com.ai4biz.app.model.MessageRole

/** One prior turn of a conversation, sent as context for a new message. */
data class ConversationTurn(val role: MessageRole, val content: String)

/** A tool the assistant thinks fits the conversation -- never executed automatically, only suggested. */
data class SuggestedTool(val toolId: String, val reason: String)

data class AssistantResponse(val reply: String, val suggestedTools: List<SuggestedTool>)

/**
 * The AI Assistant (Phase 2 Sprints 5-6, see docs/PHASE2_ARCHITECTURE.md)
 * -- a conversational advisor that chats and, when the conversation shows
 * real intent, suggests one of the 5 existing generator tools by id. It
 * deliberately never generates a document itself; the client hands a
 * suggestion off to that tool's normal [com.ai4biz.app.ui.generator.GeneratorScreen].
 *
 * Its own interface (not an [AiGeneratorService] extension) since the
 * request/response shapes are materially different -- conversation
 * history in, structured reply+suggestions out.
 */
interface AssistantService {
    suspend fun sendMessage(
        message: String,
        history: List<ConversationTurn>,
        businessContext: String?
    ): Result<AssistantResponse>
}
