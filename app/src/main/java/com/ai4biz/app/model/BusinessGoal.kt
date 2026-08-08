package com.ai4biz.app.model

enum class BusinessGoalType {
    GET_MORE_CUSTOMERS,
    INCREASE_SALES,
    IMPROVE_MARKETING,
    SAVE_TIME,
    CUSTOMER_RETENTION,
    BRAND_AWARENESS,
    AUTOMATION,
    EXPANSION
}

/**
 * A goal the [Workspace]'s owner picked during Business Setup (Phase 2
 * onboarding wizard, not yet built -- see docs/PHASE2_ARCHITECTURE.md).
 * [priority] is a small positive integer, lower = higher priority, so the
 * up-to-3 goals the spec asks for can be ordered without a separate
 * ranking table.
 */
data class BusinessGoal(
    val id: String,
    val workspaceId: String,
    val goalType: BusinessGoalType,
    val description: String,
    val priority: Int,
    val isActive: Boolean
)
