package com.ai4biz.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth_prefs")

data class AuthState(
    val isSignedIn: Boolean = false,
    val displayName: String = "",
    val isGuest: Boolean = false
)

/**
 * Local-only auth state for this scaffold. The PRD's Security section calls
 * for Google Sign-In + email auth; wiring in real Firebase Auth later only
 * means replacing the bodies of [signInWithEmail]/[signInAsGuest] (and adding
 * a signInWithGoogle) while keeping this same [authState] contract for the UI.
 */
class AuthRepository(private val context: Context) {

    private object Keys {
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val IS_GUEST = booleanPreferencesKey("is_guest")
    }

    val authState: Flow<AuthState> = context.authDataStore.data.map { prefs ->
        AuthState(
            isSignedIn = prefs[Keys.SIGNED_IN] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME] ?: "",
            isGuest = prefs[Keys.IS_GUEST] ?: false
        )
    }

    suspend fun signInWithEmail(email: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.SIGNED_IN] = true
            prefs[Keys.DISPLAY_NAME] = email
            prefs[Keys.IS_GUEST] = false
        }
    }

    suspend fun signInAsGuest() {
        context.authDataStore.edit { prefs ->
            prefs[Keys.SIGNED_IN] = true
            prefs[Keys.DISPLAY_NAME] = "Guest"
            prefs[Keys.IS_GUEST] = true
        }
    }

    suspend fun signOut() {
        context.authDataStore.edit { prefs ->
            prefs[Keys.SIGNED_IN] = false
            prefs[Keys.DISPLAY_NAME] = ""
            prefs[Keys.IS_GUEST] = false
        }
    }
}
