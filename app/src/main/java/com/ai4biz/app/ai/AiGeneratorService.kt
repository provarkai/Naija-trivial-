package com.ai4biz.app.ai

import com.ai4biz.app.model.ToolType

/**
 * Generates AI content for a given tool. This is the single seam to swap in
 * a real LLM backend later (see [MockAiGeneratorService] for the contract to
 * preserve — inputs are keyed by [com.ai4biz.app.model.InputField.key]).
 */
interface AiGeneratorService {
    suspend fun generate(toolType: ToolType, inputs: Map<String, String>): Result<String>
}
