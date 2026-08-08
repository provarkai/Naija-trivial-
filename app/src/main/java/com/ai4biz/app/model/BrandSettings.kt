package com.ai4biz.app.model

/**
 * Tone (see [BrandTone]) can be selected from a fixed list of Phase 2 spec
 * options; else the wizard falls back to storing a free-text description
 * under [CUSTOM].
 */
enum class BrandTone {
    PROFESSIONAL,
    FRIENDLY,
    PREMIUM,
    CASUAL,
    CORPORATE,
    BOLD,
    PERSUASIVE,
    CUSTOM
}

/**
 * How a [Workspace]'s brand should sound and look -- injected into AI
 * prompts (Phase 2 context engine, see docs/PHASE2_ARCHITECTURE.md) so
 * generated content matches the business's voice without the user having
 * to restate it every time.
 */
data class BrandSettings(
    val id: String,
    val workspaceId: String,
    val brandName: String,
    val tagline: String,
    val tone: BrandTone,
    val writingStyle: String,
    val primaryColor: String,
    val secondaryColor: String,
    val defaultLanguage: String,
    val logoUri: String?
)
