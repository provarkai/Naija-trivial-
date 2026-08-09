package com.ai4biz.app.model

/**
 * The AI Assistant's conversation for a [Workspace] (Phase 2 Sprints 5-6,
 * see docs/PHASE2_ARCHITECTURE.md). Today a workspace has exactly one --
 * there's no conversation list/switcher UI yet -- but this is modeled as
 * its own entity so a future multi-conversation UI is additive, not
 * another migration.
 */
data class Conversation(
    val id: String,
    val workspaceId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
