package com.ai4biz.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "generated_documents")
data class GeneratedDocumentEntity(
    @PrimaryKey val id: String,
    val toolId: String,
    val toolTitle: String,
    val title: String,
    val content: String,
    val createdAt: Long
)
