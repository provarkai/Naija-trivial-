package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ai4biz.app.model.MessageRole

@Entity(tableName = "ai_messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    // Comma-joined com.ai4biz.app.model.ToolType ids, null/blank if none.
    // At most 5 possible values, never queried/filtered by SQL, only ever
    // round-tripped whole -- a join table or embedded JSON would be
    // ceremony this doesn't need.
    val suggestedToolIds: String?,
    val createdAt: Long
)
