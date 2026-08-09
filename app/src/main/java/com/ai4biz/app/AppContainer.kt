package com.ai4biz.app

import android.content.Context
import com.ai4biz.app.ads.InterstitialAdManager
import com.ai4biz.app.ads.RewardedAdManager
import com.ai4biz.app.ai.AiGeneratorService
import com.ai4biz.app.ai.AssistantRepository
import com.ai4biz.app.ai.AssistantService
import com.ai4biz.app.ai.BusinessContextService
import com.ai4biz.app.ai.MockAiGeneratorService
import com.ai4biz.app.ai.MockAssistantService
import com.ai4biz.app.ai.RemoteAiGeneratorService
import com.ai4biz.app.ai.RemoteAssistantService
import com.ai4biz.app.billing.BillingManager
import com.ai4biz.app.billing.ClientOnlyPurchaseVerifier
import com.ai4biz.app.billing.PurchaseVerifier
import com.ai4biz.app.billing.RemotePurchaseVerifier
import com.ai4biz.app.data.local.AppDatabase
import com.ai4biz.app.data.repository.AuthRepository
import com.ai4biz.app.data.repository.BrandSettingsRepository
import com.ai4biz.app.data.repository.BusinessGoalRepository
import com.ai4biz.app.data.repository.BusinessProfileRepository
import com.ai4biz.app.data.repository.ConversationRepository
import com.ai4biz.app.data.repository.CustomerRepository
import com.ai4biz.app.data.repository.DeviceIdentityRepository
import com.ai4biz.app.data.repository.DocumentRepository
import com.ai4biz.app.data.repository.EntitlementRepository
import com.ai4biz.app.data.repository.MessageRepository
import com.ai4biz.app.data.repository.OnboardingRepository
import com.ai4biz.app.data.repository.ProductServiceRepository
import com.ai4biz.app.data.repository.UsageRepository
import com.ai4biz.app.data.repository.WorkspaceRepository

/**
 * Minimal hand-rolled DI container -- no Hilt/Dagger dependency for this
 * scaffold. [aiGeneratorService] and [purchaseVerifier] both auto-select a
 * real backend (see /server) when one is configured via `ai4biz.backend.url`
 * in local.properties, and otherwise fall back to local-only behavior so
 * the app works with zero setup.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    val documentRepository = DocumentRepository(database.generatedDocumentDao())
    val authRepository = AuthRepository(context)
    val usageRepository = UsageRepository(context)
    val entitlementRepository = EntitlementRepository(context.applicationContext)

    // Phase 2 Sprint 1 (docs/PHASE2_ARCHITECTURE.md) -- Business Workspace
    // data foundation. Not yet used by any UI (that's Sprint 2+); wired here
    // so the repositories exist and the default workspace bootstrap
    // (Ai4bizApplication.onCreate) has something to call.
    val deviceIdentityRepository = DeviceIdentityRepository(context)
    val workspaceRepository = WorkspaceRepository(database.workspaceDao(), deviceIdentityRepository)
    val businessProfileRepository = BusinessProfileRepository(database.businessProfileDao())
    val brandSettingsRepository = BrandSettingsRepository(database.brandSettingsDao())
    val productServiceRepository = ProductServiceRepository(database.productServiceDao())
    val businessGoalRepository = BusinessGoalRepository(database.businessGoalDao())
    val customerRepository = CustomerRepository(database.customerDao())

    // Phase 2 Sprint 2 (docs/PHASE2_ARCHITECTURE.md) -- tracks whether the
    // user has been through the Business Setup wizard (finished or skipped).
    val onboardingRepository = OnboardingRepository(context)

    // Phase 2 Sprint 4 (docs/PHASE2_ARCHITECTURE.md) -- assembles workspace
    // business data into AI-prompt-ready text for the existing 5 generator
    // tools. Consumed by GeneratorViewModel.
    val businessContextService = BusinessContextService(
        workspaceRepository,
        businessProfileRepository,
        brandSettingsRepository,
        productServiceRepository,
        businessGoalRepository
    )

    val aiGeneratorService: AiGeneratorService =
        if (BuildConfig.AI4BIZ_BACKEND_URL.isNotBlank()) {
            RemoteAiGeneratorService()
        } else {
            MockAiGeneratorService()
        }

    // Phase 2 Sprints 5-6 (docs/PHASE2_ARCHITECTURE.md) -- the AI
    // Assistant: conversation persistence + the assistant service itself.
    val conversationRepository = ConversationRepository(database.conversationDao())
    val messageRepository = MessageRepository(database.messageDao())

    val assistantService: AssistantService =
        if (BuildConfig.AI4BIZ_BACKEND_URL.isNotBlank()) {
            RemoteAssistantService()
        } else {
            MockAssistantService()
        }

    val assistantRepository = AssistantRepository(
        assistantService,
        conversationRepository,
        messageRepository,
        businessContextService
    )

    private val purchaseVerifier: PurchaseVerifier =
        if (BuildConfig.AI4BIZ_BACKEND_URL.isNotBlank()) {
            RemotePurchaseVerifier()
        } else {
            ClientOnlyPurchaseVerifier()
        }

    val interstitialAdManager = InterstitialAdManager(context.applicationContext).apply { preload() }
    val rewardedAdManager = RewardedAdManager(context.applicationContext).apply { preload() }

    val billingManager = BillingManager(
        context.applicationContext,
        purchaseVerifier,
        entitlementRepository
    ).apply { startConnection() }
}
