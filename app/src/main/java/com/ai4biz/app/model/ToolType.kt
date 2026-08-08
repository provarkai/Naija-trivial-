package com.ai4biz.app.model

/**
 * The AI generator tools available in the app. This scaffold ships the five
 * Phase 1 / MVP modules from the Business Edge AI PRD; later phases (Finance
 * & CRM, Automation, Enterprise, AI agents marketplace) add more entries here
 * without needing to touch the generator/result UI, since both are driven by
 * this metadata.
 */
enum class ToolType(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val fields: List<InputField>
) {
    BUSINESS_PLAN(
        id = "business_plan",
        title = "AI Business Plan Generator",
        description = "Turn a rough idea into a structured business plan.",
        category = "MVP",
        fields = listOf(
            InputField("businessName", "Business name", "e.g. Lagos Fresh Foods"),
            InputField("industry", "Industry / sector", "e.g. Food & beverage"),
            InputField("targetMarket", "Target market", "Who are your customers?", multiline = true),
            InputField("problemSolved", "Problem you solve", "What pain point does this address?", multiline = true),
            InputField("revenueModel", "Revenue model", "How will you make money?", multiline = true)
        )
    ),
    PROPOSAL(
        id = "proposal",
        title = "AI Proposal Generator",
        description = "Draft a client-ready project proposal in minutes.",
        category = "MVP",
        fields = listOf(
            InputField("clientName", "Client name", "e.g. Zenith Retail Ltd"),
            InputField("projectTitle", "Project title", "e.g. Website redesign"),
            InputField("scopeOfWork", "Scope of work", "What will you deliver?", multiline = true),
            InputField("timeline", "Timeline", "e.g. 6 weeks"),
            InputField("budget", "Budget", "e.g. ₦850,000")
        )
    ),
    INVOICE_RECEIPT(
        id = "invoice_receipt",
        title = "AI Invoice & Receipt Generator",
        description = "Generate a clean invoice or receipt from a quick summary.",
        category = "MVP",
        fields = listOf(
            InputField("businessName", "Your business name", "e.g. Bello Solar Solutions"),
            InputField("clientName", "Bill to", "e.g. Chidinma Okafor"),
            InputField("itemsDescription", "Items / services", "One per line, with quantity and price", multiline = true),
            InputField("amount", "Total amount", "e.g. ₦120,000"),
            InputField("dueDate", "Due date", "e.g. 30 Aug 2026", required = false)
        )
    ),
    SOCIAL_MEDIA_CONTENT(
        id = "social_media_content",
        title = "AI Social Media Content Generator",
        description = "Create on-brand posts for your channels.",
        category = "MVP",
        fields = listOf(
            InputField("brandName", "Brand name", "e.g. Naija Fit Gym"),
            InputField("platform", "Platform", "e.g. Instagram, X, WhatsApp Status"),
            InputField("topic", "Topic / offer", "What's the post about?", multiline = true),
            InputField("tone", "Tone", "e.g. Friendly and energetic", required = false)
        )
    ),
    WHATSAPP_REPLY(
        id = "whatsapp_reply",
        title = "AI WhatsApp Reply Generator",
        description = "Draft a fast, professional reply to a customer message.",
        category = "MVP",
        fields = listOf(
            InputField("customerMessage", "Customer's message", "Paste what they sent you", multiline = true),
            InputField("businessContext", "Context", "Any details the reply should reflect", multiline = true, required = false),
            InputField("tone", "Tone", "e.g. Warm and professional", required = false)
        )
    );

    companion object {
        fun fromId(id: String): ToolType? = entries.find { it.id == id }
        val mvpTools: List<ToolType> get() = entries.filter { it.category == "MVP" }
    }
}
