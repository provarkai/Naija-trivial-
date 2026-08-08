package com.ai4biz.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Ai4bizApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        container = AppContainer(this)

        // Phase 2 Sprint 1 (docs/PHASE2_ARCHITECTURE.md): make sure every
        // install has a workspace to attribute documents/business data to,
        // even before the onboarding wizard (Sprint 2) exists. Idempotent --
        // a no-op on migrated installs, since MIGRATION_1_2 already seeded
        // the default workspace row.
        applicationScope.launch {
            container.workspaceRepository.getOrCreateDefaultWorkspace()
        }
    }
}
