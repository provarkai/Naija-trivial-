package com.ai4biz.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.entitlementDataStore by preferencesDataStore(name = "entitlement_prefs")

/**
 * Persisted, server-verified premium status -- the single source of truth
 * [com.ai4biz.app.billing.BillingManager] exposes to the rest of the app.
 * Deliberately separate from Google Play Billing's own in-memory purchase
 * list: that list reflects "does this device currently see a purchase",
 * this reflects "did our server confirm that purchase is real and active",
 * and the two update on different schedules (see BillingManager).
 */
class EntitlementRepository(private val context: Context) {

    private object Keys {
        val VERIFIED_PREMIUM = booleanPreferencesKey("verified_premium")
        val VERIFIED_PLAN_ID = stringPreferencesKey("verified_plan_id")
    }

    val isPremium: Flow<Boolean> = context.entitlementDataStore.data.map { it[Keys.VERIFIED_PREMIUM] ?: false }
    val planId: Flow<String?> = context.entitlementDataStore.data.map { it[Keys.VERIFIED_PLAN_ID] }

    suspend fun setVerified(isPremium: Boolean, planId: String?) {
        context.entitlementDataStore.edit { prefs ->
            prefs[Keys.VERIFIED_PREMIUM] = isPremium
            if (planId != null) prefs[Keys.VERIFIED_PLAN_ID] = planId else prefs.remove(Keys.VERIFIED_PLAN_ID)
        }
    }
}
