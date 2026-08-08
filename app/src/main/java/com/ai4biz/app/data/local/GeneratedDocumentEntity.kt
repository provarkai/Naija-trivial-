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
    val createdAt: Long,
    // Added in MIGRATION_1_2 (AppDatabase). Defaulted here too so any
    // in-memory construction that omits it still compiles safely; the
    // migration backfills existing rows via SQL DEFAULT, not this value.
    val workspaceId: String = WorkspaceDefaults.DEFAULT_WORKSPACE_ID
)
