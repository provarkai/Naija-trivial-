package com.ai4biz.app.model

/**
 * The persistent identity of a [Workspace]'s business -- what it's called,
 * what it does, where it operates. Phase 2's context engine (see
 * docs/PHASE2_ARCHITECTURE.md) reads this so the user never has to
 * re-describe their business on every AI generation.
 */
data class BusinessProfile(
    val id: String,
    val workspaceId: String,
    val businessName: String,
    val businessType: String,
    val industry: String,
    val description: String,
    val country: String,
    val state: String,
    val city: String,
    val address: String?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val whatsapp: String?,
    val currency: String,
    val taxNumber: String?,
    val createdAt: Long,
    val updatedAt: Long
)
