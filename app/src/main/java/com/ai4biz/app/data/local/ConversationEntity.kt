package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
