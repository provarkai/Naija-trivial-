package com.ai4biz.app.model

enum class MessageRole {
    USER,
    ASSISTANT
}

/**
 * One turn in an AI Assistant [Conversation] (Phase 2 Sprints 5-6, see
 * docs/PHASE2_ARCHITECTURE.md). [suggestedToolIds] is only ever populated
 * on ASSISTANT messages -- one or more [com.ai4biz.app.model.ToolType.id]s
 * the assistant thinks fit the conversation, rendered as tappable
 * suggestions that hand off to that tool's normal Generator screen. The
 * assistant never generates a document itself.
 */
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val suggestedToolIds: List<String>,
    val createdAt: Long
)
