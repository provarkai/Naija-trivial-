package com.ai4biz.app.ai

import com.ai4biz.app.data.repository.BrandSettingsRepository
import com.ai4biz.app.data.repository.BusinessGoalRepository
import com.ai4biz.app.data.repository.BusinessProfileRepository
import com.ai4biz.app.data.repository.ProductServiceRepository
import com.ai4biz.app.data.repository.WorkspaceRepository
import com.ai4biz.app.model.BusinessContext
import com.ai4biz.app.model.BusinessProfile
import com.ai4biz.app.model.ToolType
import kotlinx.coroutines.flow.first

/**
 * Not "Business context" or "businessContext" -- both collide with
 * existing user-typed [com.ai4biz.app.model.InputField] keys already
 * declared on [ToolType] tools (`businessName` on Business Plan/Invoice,
 * `businessContext` on WhatsApp Reply). This key is deliberately distinct.
 */
private const val CONTEXT_ENTRY_KEY = "Workspace context"

/**
 * Assembles workspace business data into AI-prompt-ready text (Phase 2
 * Sprint 4, see docs/PHASE2_ARCHITECTURE.md). Reads the Sprint 1
 * repositories directly -- no new schema, no server changes; the merged
 * result flows through the exact same `inputs: Map<String,String>` seam
 * [AiGeneratorService.generate] already accepts.
 *
 * Deliberately filters what it returns per [ToolType] rather than always
 * sending the whole business profile -- see [formatForPrompt].
 */
class BusinessContextService(
    private val workspaceRepository: WorkspaceRepository,
    private val businessProfileRepository: BusinessProfileRepository,
    private val brandSettingsRepository: BrandSettingsRepository,
    private val productServiceRepository: ProductServiceRepository,
    private val businessGoalRepository: BusinessGoalRepository
) {

    suspend fun getContext(): BusinessContext {
        val workspace = workspaceRepository.observeActiveWorkspace().first()
            ?: return BusinessContext(null, null, emptyList(), emptyList())

        val profile = businessProfileRepository.observeAllByWorkspace(workspace.id).first().firstOrNull()
        val brand = brandSettingsRepository.observeAllByWorkspace(workspace.id).first().firstOrNull()
        val products = productServiceRepository.observeAllByWorkspace(workspace.id).first()
        val goals = businessGoalRepository.observeAllByWorkspace(workspace.id).first()

        return BusinessContext(profile, brand, products, goals)
    }

    /**
     * Returns zero or one extra prompt entry, filtered to what's actually
     * relevant to [toolType] -- e.g. a social post gets the product list,
     * an invoice gets contact info and currency, a WhatsApp reply gets
     * just the baseline (kept short since it's a casual-reply tool).
     * Returns an empty map if there's no [com.ai4biz.app.model.BusinessProfile]
     * yet, so a workspace that skipped the Sprint 2 wizard sees identical
     * behavior to before this feature existed.
     */
    fun formatForPrompt(context: BusinessContext, toolType: ToolType): Map<String, String> {
        val profile = context.businessProfile ?: return emptyMap()
        val lines = mutableListOf<String>()

        lines += "Business: ${profile.businessName} (${profile.industry}, ${profile.businessType})"
        context.brandSettings?.let { brand ->
            val toneLabel = brand.tone.name.lowercase().replaceFirstChar(Char::uppercase)
            lines += "Brand tone: $toneLabel" + if (brand.tagline.isNotBlank()) " -- \"${brand.tagline}\"" else ""
        }

        when (toolType) {
            ToolType.BUSINESS_PLAN -> {
                if (profile.description.isNotBlank()) lines += "Target customers: ${profile.description}"
                location(profile)?.let { lines += "Location: $it" }
                if (context.goals.isNotEmpty()) {
                    lines += "Business goals: " + context.goals.sortedBy { it.priority }
                        .joinToString(", ") { it.goalType.name.lowercase().replace('_', ' ') }
                }
            }
            ToolType.PROPOSAL -> {
                if (profile.description.isNotBlank()) lines += "Target customers: ${profile.description}"
                if (context.products.isNotEmpty()) {
                    lines += "What we offer: " + context.products.joinToString(", ") { it.name }
                }
            }
            ToolType.INVOICE_RECEIPT -> {
                val contact = listOfNotNull(profile.phone, profile.email, profile.address).joinToString(" / ")
                if (contact.isNotBlank()) lines += "Business contact: $contact"
                lines += "Currency: ${profile.currency}"
                if (context.products.isNotEmpty()) {
                    lines += "Catalog: " + context.products.joinToString(", ") {
                        if (it.price > 0) "${it.name} (₦${it.price})" else it.name
                    }
                }
            }
            ToolType.SOCIAL_MEDIA_CONTENT -> {
                if (context.products.isNotEmpty()) {
                    lines += "Products/services to feature: " + context.products.joinToString(", ") { it.name }
                }
            }
            ToolType.WHATSAPP_REPLY -> {
                // Intentionally nothing extra -- WhatsApp replies are short
                // and casual; the baseline business/brand lines above are enough.
            }
        }

        return mapOf(CONTEXT_ENTRY_KEY to lines.joinToString("\n"))
    }

    /**
     * General-purpose formatting for the AI Assistant (Phase 2 Sprints
     * 5-6), which doesn't know the user's intent ahead of time the way
     * [formatForPrompt] does for a specific [ToolType] -- so this returns
     * a broader (but still not exhaustive) summary rather than a
     * tool-filtered one. Returns null if there's no
     * [com.ai4biz.app.model.BusinessProfile] yet.
     */
    fun formatForAssistant(context: BusinessContext): String? {
        val profile = context.businessProfile ?: return null
        val lines = mutableListOf<String>()

        lines += "Business: ${profile.businessName} (${profile.industry}, ${profile.businessType})"
        context.brandSettings?.let { brand ->
            val toneLabel = brand.tone.name.lowercase().replaceFirstChar(Char::uppercase)
            lines += "Brand tone: $toneLabel" + if (brand.tagline.isNotBlank()) " -- \"${brand.tagline}\"" else ""
        }
        if (profile.description.isNotBlank()) lines += "Target customers: ${profile.description}"
        if (context.products.isNotEmpty()) {
            lines += "Products/services: " + context.products.joinToString(", ") { it.name }
        }
        if (context.goals.isNotEmpty()) {
            lines += "Goals: " + context.goals.sortedBy { it.priority }
                .joinToString(", ") { it.goalType.name.lowercase().replace('_', ' ') }
        }

        return lines.joinToString("\n")
    }

    private fun location(profile: BusinessProfile): String? {
        val parts = listOf(profile.city, profile.state, profile.country).filter { it.isNotBlank() }
        return parts.joinToString(", ").ifBlank { null }
    }
}
