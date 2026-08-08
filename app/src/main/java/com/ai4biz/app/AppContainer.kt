package com.ai4biz.app

import android.content.Context
import com.ai4biz.app.ai.AiGeneratorService
import com.ai4biz.app.ai.MockAiGeneratorService
import com.ai4biz.app.ai.RemoteAiGeneratorService
import com.ai4biz.app.data.local.AppDatabase
import com.ai4biz.app.data.repository.AuthRepository
import com.ai4biz.app.data.repository.DocumentRepository

/**
 * Minimal hand-rolled DI container -- no Hilt/Dagger dependency for this
 * scaffold. [aiGeneratorService] auto-selects a real backend
 * ([RemoteAiGeneratorService], see /server) when one is configured via
 * `ai4biz.backend.url` in local.properties, and otherwise falls back to
 * [MockAiGeneratorService] so the app works with zero setup.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val documentRepository = DocumentRepository(database.generatedDocumentDao())
    val authRepository = AuthRepository(context)
    val aiGeneratorService: AiGeneratorService =
        if (BuildConfig.AI4BIZ_BACKEND_URL.isNotBlank()) {
            RemoteAiGeneratorService()
        } else {
            MockAiGeneratorService()
        }
}
