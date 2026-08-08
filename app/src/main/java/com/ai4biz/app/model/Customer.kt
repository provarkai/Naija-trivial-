package com.ai4biz.app.model

/**
 * A customer or lead belonging to a [Workspace]. Deliberately lightweight
 * for Phase 2 Sprint 1 -- this is not a CRM (see docs/PHASE2_ARCHITECTURE.md
 * "What We Should NOT Build Yet"), just enough to let a Proposal/Invoice/
 * WhatsApp reply be personalized to a known contact.
 */
data class Customer(
    val id: String,
    val workspaceId: String,
    val name: String,
    val companyName: String?,
    val email: String?,
    val phone: String?,
    val whatsapp: String?,
    val industry: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)
