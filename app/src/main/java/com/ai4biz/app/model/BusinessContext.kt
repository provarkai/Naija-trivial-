package com.ai4biz.app.model

/**
 * A snapshot of a workspace's business data, assembled by
 * [com.ai4biz.app.ai.BusinessContextService] for injection into AI
 * prompts (Phase 2 Sprint 4, see docs/PHASE2_ARCHITECTURE.md). All fields
 * are nullable/empty-safe since a workspace may not have completed the
 * Business Setup wizard yet.
 */
data class BusinessContext(
    val businessProfile: BusinessProfile?,
    val brandSettings: BrandSettings?,
    val products: List<ProductService>,
    val goals: List<BusinessGoal>
)
