package com.ai4biz.app

import android.content.Context
import com.ai4biz.app.ai.AiGeneratorService
import com.ai4biz.app.ai.MockAiGeneratorService
import com.ai4biz.app.data.local.AppDatabase
import com.ai4biz.app.data.repository.AuthRepository
import com.ai4biz.app.data.repository.DocumentRepository

/**
 * Minimal hand-rolled DI container -- no Hilt/Dagger dependency for this
 * scaffold. Swap [aiGeneratorService] for a real backend-backed
 * implementation when ready; everything downstream depends on the
 * [AiGeneratorService] interface, not this mock.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val documentRepository = DocumentRepository(database.generatedDocumentDao())
    val authRepository = AuthRepository(context)
    val aiGeneratorService: AiGeneratorService = MockAiGeneratorService()
}
