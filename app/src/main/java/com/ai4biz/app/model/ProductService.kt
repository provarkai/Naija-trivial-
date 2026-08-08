package com.ai4biz.app.model

enum class ProductServiceType {
    PRODUCT,
    SERVICE
}

/**
 * Something a [Workspace]'s business sells. Reused by Proposal, Invoice,
 * Social Content and future AI-assistant tools so pricing/description
 * doesn't need to be retyped per generation (see docs/PHASE2_ARCHITECTURE.md).
 */
data class ProductService(
    val id: String,
    val workspaceId: String,
    val name: String,
    val type: ProductServiceType,
    val description: String,
    val category: String,
    val price: Double,
    val currency: String,
    val unit: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
