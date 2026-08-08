package com.ai4biz.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.usageDataStore by preferencesDataStore(name = "usage_prefs")

data class UsageState(
    val generationsUsedToday: Int = 0,
    val bonusGenerations: Int = 0
) {
    val remainingFree: Int get() = (UsageRepository.DAILY_FREE_LIMIT - generationsUsedToday).coerceAtLeast(0)
    val canGenerate: Boolean get() = remainingFree > 0 || bonusGenerations > 0
}

/**
 * Tracks how many free generations a user has left today, plus any bonus
 * generations earned by watching a rewarded ad. This is a local-only nudge
 * toward the (still-stubbed) paid tiers -- not a server-enforced limit, so
 * it's not a real anti-abuse boundary, just a free-tier UX pattern.
 */
class UsageRepository(private val context: Context) {

    companion object {
        const val DAILY_FREE_LIMIT = 5
    }

    private object Keys {
        val USED_TODAY = intPreferencesKey("generations_used_today")
        val BONUS = intPreferencesKey("bonus_generations")
        val LAST_RESET_DAY = longPreferencesKey("last_reset_epoch_day")
    }

    private fun currentEpochDay(): Long = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

    val usageState: Flow<UsageState> = context.usageDataStore.data.map { prefs ->
        val today = currentEpochDay()
        val lastReset = prefs[Keys.LAST_RESET_DAY] ?: today
        val usedToday = if (lastReset == today) prefs[Keys.USED_TODAY] ?: 0 else 0
        UsageState(
            generationsUsedToday = usedToday,
            bonusGenerations = prefs[Keys.BONUS] ?: 0
        )
    }

    /** Call after a successful generation. Consumes today's free allowance first, then bonus. */
    suspend fun recordGeneration() {
        context.usageDataStore.edit { prefs ->
            val today = currentEpochDay()
            val lastReset = prefs[Keys.LAST_RESET_DAY] ?: today
            val usedToday = if (lastReset == today) prefs[Keys.USED_TODAY] ?: 0 else 0
            val bonus = prefs[Keys.BONUS] ?: 0

            if (usedToday < DAILY_FREE_LIMIT) {
                prefs[Keys.USED_TODAY] = usedToday + 1
            } else if (bonus > 0) {
                prefs[Keys.BONUS] = bonus - 1
            }
            prefs[Keys.LAST_RESET_DAY] = today
        }
    }

    /** Call when a rewarded ad is watched to completion. */
    suspend fun addBonusGeneration() {
        context.usageDataStore.edit { prefs ->
            prefs[Keys.BONUS] = (prefs[Keys.BONUS] ?: 0) + 1
        }
    }
}
