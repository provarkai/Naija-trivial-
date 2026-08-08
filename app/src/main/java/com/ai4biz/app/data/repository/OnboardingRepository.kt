package com.ai4biz.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

/**
 * Tracks whether the user has been through the Business Setup wizard
 * (Phase 2 Sprint 2, see docs/PHASE2_ARCHITECTURE.md) -- either by
 * finishing it or tapping "Skip for now". Either way it shouldn't be
 * shown again on a later sign-in; [AuthScreen.goHome] gates on this flag.
 */
class OnboardingRepository(private val context: Context) {

    private object Keys {
        val HAS_COMPLETED_BUSINESS_SETUP = booleanPreferencesKey("has_completed_business_setup")
    }

    val hasCompletedBusinessSetup: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[Keys.HAS_COMPLETED_BUSINESS_SETUP] ?: false
    }

    suspend fun setHasCompletedBusinessSetup(completed: Boolean = true) {
        context.onboardingDataStore.edit { prefs ->
            prefs[Keys.HAS_COMPLETED_BUSINESS_SETUP] = completed
        }
    }
}
