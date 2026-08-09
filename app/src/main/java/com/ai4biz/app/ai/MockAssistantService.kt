package com.ai4biz.app.ai

import com.ai4biz.app.model.ToolType
import kotlinx.coroutines.delay

/**
 * Offline fallback used when no backend is configured -- keeps the
 * Assistant screen fully usable with zero setup, matching
 * [MockAiGeneratorService]'s own purpose. Not meant to be smart: a plain
 * keyword-overlap check against each [ToolType]'s title/description,
 * not real language understanding.
 */
class MockAssistantService : AssistantService {

    override suspend fun sendMessage(
        message: String,
        history: List<ConversationTurn>,
        businessContext: String?
    ): Result<AssistantResponse> {
        delay(400)

        val words = message.lowercase().split(Regex("\\W+")).filter { it.length > 3 }.toSet()
        val matches = ToolType.mvpTools.filter { tool ->
            val toolWords = (tool.title + " " + tool.description).lowercase().split(Regex("\\W+")).toSet()
            words.intersect(toolWords).isNotEmpty()
        }

        return if (matches.isNotEmpty()) {
            Result.success(
                AssistantResponse(
                    reply = "I'm running in offline mode right now, but this looks like it could use " +
                        matches.joinToString(" or ") { it.title } + ".",
                    suggestedTools = matches.map { SuggestedTool(it.id, "Looks like a fit for ${it.title}") }
                )
            )
        } else {
            Result.success(
                AssistantResponse(
                    reply = "I'm running in offline mode right now (no backend configured), so I can't " +
                        "have a real conversation -- but you can still use any of the tools on the home screen.",
                    suggestedTools = emptyList()
                )
            )
        }
    }
}
