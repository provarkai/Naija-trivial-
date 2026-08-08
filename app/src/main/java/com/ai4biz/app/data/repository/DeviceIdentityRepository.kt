package com.ai4biz.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.deviceIdentityDataStore by preferencesDataStore(name = "device_identity_prefs")

/**
 * A stable per-device pseudo-identity, used as [com.ai4biz.app.model.Workspace.ownerUserId]
 * until real backend authentication exists (Phase 2 Sprint 9, see
 * docs/PHASE2_ARCHITECTURE.md). Deliberately separate from [AuthRepository]:
 * that repository's state is ephemeral session info (signed in/guest,
 * display name) that gets cleared on sign-out, whereas this id must survive
 * sign-in/sign-out/guest-mode toggles so a device's workspaces stay
 * attributed to the same owner across all of that.
 */
class DeviceIdentityRepository(private val context: Context) {

    private object Keys {
        val DEVICE_USER_ID = stringPreferencesKey("device_user_id")
    }

    /**
     * Returns the existing device id, generating and persisting one on
     * first call. Called rarely (once per cold start, from the app's
     * default-workspace bootstrap), so a plain read-then-write is fine --
     * no need for anything fancier on this low-frequency a path.
     */
    suspend fun getOrCreateDeviceUserId(): String {
        val current = context.deviceIdentityDataStore.data
            .map { it[Keys.DEVICE_USER_ID] }
            .first()
        if (current != null) return current

        val generated = UUID.randomUUID().toString()
        context.deviceIdentityDataStore.edit { it[Keys.DEVICE_USER_ID] = generated }
        return generated
    }
}
