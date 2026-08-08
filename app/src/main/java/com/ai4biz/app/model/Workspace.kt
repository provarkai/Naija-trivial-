package com.ai4biz.app.model

/**
 * A business the user manages inside Business Edge AI. One user may
 * eventually own more than one workspace (consultants, agencies, holding
 * companies) -- see docs/PHASE2_ARCHITECTURE.md -- so every business-related
 * record (profile, products, brand, goals, customers, documents) is scoped
 * by [Workspace.id], not by user directly.
 */
data class Workspace(
    val id: String,
    val ownerUserId: String,
    val name: String,
    val businessProfileId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean
)
