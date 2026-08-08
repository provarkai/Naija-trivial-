package com.ai4biz.app.ai

import com.ai4biz.app.model.ToolType
import kotlinx.coroutines.delay

/**
 * Offline, template-based stand-in for a real LLM backend so the app is
 * fully usable out of the box with no API key.
 *
 * To wire in a real model, implement [AiGeneratorService] against your own
 * backend (recommended: proxy the LLM call through a server you control —
 * never ship a model API key inside the Android client) and swap the
 * instance created in [com.ai4biz.app.AppContainer].
 */
class MockAiGeneratorService : AiGeneratorService {

    override suspend fun generate(toolType: ToolType, inputs: Map<String, String>): Result<String> {
        return try {
            // Simulate network/AI latency so the loading state is exercised.
            delay(1100)
            val content = when (toolType) {
                ToolType.BUSINESS_PLAN -> businessPlan(inputs)
                ToolType.PROPOSAL -> proposal(inputs)
                ToolType.INVOICE_RECEIPT -> invoiceReceipt(inputs)
                ToolType.SOCIAL_MEDIA_CONTENT -> socialMediaContent(inputs)
                ToolType.WHATSAPP_REPLY -> whatsAppReply(inputs)
            }
            Result.success(content)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun Map<String, String>.value(key: String, fallback: String = "Not specified"): String =
        this[key]?.trim().takeUnless { it.isNullOrEmpty() } ?: fallback

    private fun businessPlan(inputs: Map<String, String>) = buildString {
        val name = inputs.value("businessName", "Your Business")
        appendLine("BUSINESS PLAN: $name")
        appendLine()
        appendLine("EXECUTIVE SUMMARY")
        appendLine(
            "$name operates in ${inputs.value("industry")} and exists to solve: " +
                "${inputs.value("problemSolved")} By focusing on this problem, $name is " +
                "positioned to build lasting relationships with its target customers."
        )
        appendLine()
        appendLine("TARGET MARKET")
        appendLine(inputs.value("targetMarket"))
        appendLine()
        appendLine("PROBLEM & SOLUTION")
        appendLine(inputs.value("problemSolved"))
        appendLine()
        appendLine("REVENUE MODEL")
        appendLine(inputs.value("revenueModel"))
        appendLine()
        appendLine("NEXT STEPS")
        appendLine("- Validate demand by talking to 10 potential customers")
        appendLine("- Build a minimum viable offer and test pricing")
        appendLine("- Set a 90-day milestone plan and track weekly progress")
    }.trim()

    private fun proposal(inputs: Map<String, String>) = buildString {
        val title = inputs.value("projectTitle", "Project Proposal")
        appendLine("PROPOSAL: $title")
        appendLine("Prepared for: ${inputs.value("clientName")}")
        appendLine()
        appendLine("OVERVIEW")
        appendLine(
            "This proposal outlines how we will deliver \"$title\" for " +
                "${inputs.value("clientName")}, within the scope, timeline, and budget below."
        )
        appendLine()
        appendLine("SCOPE OF WORK")
        appendLine(inputs.value("scopeOfWork"))
        appendLine()
        appendLine("TIMELINE")
        appendLine(inputs.value("timeline"))
        appendLine()
        appendLine("INVESTMENT")
        appendLine(inputs.value("budget"))
        appendLine()
        appendLine("NEXT STEPS")
        appendLine("Reply to confirm and we'll schedule a kickoff call within 2 business days.")
    }.trim()

    private fun invoiceReceipt(inputs: Map<String, String>) = buildString {
        appendLine("INVOICE")
        appendLine()
        appendLine("From: ${inputs.value("businessName")}")
        appendLine("Bill To: ${inputs.value("clientName")}")
        appendLine()
        appendLine("ITEMS / SERVICES")
        appendLine(inputs.value("itemsDescription"))
        appendLine()
        appendLine("TOTAL DUE: ${inputs.value("amount")}")
        appendLine("DUE DATE: ${inputs.value("dueDate", "Upon receipt")}")
        appendLine()
        appendLine("Thank you for your business.")
    }.trim()

    private fun socialMediaContent(inputs: Map<String, String>) = buildString {
        val brand = inputs.value("brandName", "Your Brand")
        val platform = inputs.value("platform", "social media")
        val topic = inputs.value("topic")
        val tone = inputs.value("tone", "friendly and confident")
        appendLine("SOCIAL MEDIA CONTENT — $brand ($platform)")
        appendLine("Tone: $tone")
        appendLine()
        appendLine("OPTION 1 (Announcement)")
        appendLine("$brand here! $topic Tap the link in bio to find out more. 🚀")
        appendLine()
        appendLine("OPTION 2 (Story-driven)")
        appendLine(
            "We built $brand because we believe in doing things differently. " +
                "Today, that means: $topic What do you think? Let us know below 👇"
        )
        appendLine()
        appendLine("OPTION 3 (Short & punchy)")
        appendLine("$topic — only from $brand.")
    }.trim()

    private fun whatsAppReply(inputs: Map<String, String>) = buildString {
        val tone = inputs.value("tone", "warm and professional")
        val context = inputs.value("businessContext", "")
        appendLine("SUGGESTED REPLY (tone: $tone)")
        appendLine()
        appendLine(
            "Hi! Thanks so much for reaching out. " +
                if (context.isNotEmpty()) "$context " else ""
        )
        appendLine(
            "Regarding your message — \"${inputs.value("customerMessage", "your question")}\" — " +
                "here's what I can tell you: we've got you covered and will follow up with full " +
                "details shortly. Please let us know if you need anything else in the meantime!"
        )
    }.trim()
}
