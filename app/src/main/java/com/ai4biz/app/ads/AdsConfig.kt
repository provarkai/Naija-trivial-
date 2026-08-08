package com.ai4biz.app.ads

import com.ai4biz.app.BuildConfig

/**
 * Ad unit IDs. Debug builds always use Google's published test IDs
 * (https://developers.google.com/admob/android/test-ads) regardless of
 * what local.properties configures, so real ads are never accidentally
 * served -- or clicked -- during development. Release builds use the
 * configured IDs, falling back to the same test IDs if unset so the app
 * never ships with a broken/blank ad unit.
 */
object AdsConfig {
    private const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    val bannerUnitId: String
        get() = pick(BuildConfig.ADMOB_BANNER_UNIT_ID, TEST_BANNER_UNIT_ID)

    val interstitialUnitId: String
        get() = pick(BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID, TEST_INTERSTITIAL_UNIT_ID)

    val rewardedUnitId: String
        get() = pick(BuildConfig.ADMOB_REWARDED_UNIT_ID, TEST_REWARDED_UNIT_ID)

    private fun pick(configured: String, test: String): String =
        if (BuildConfig.DEBUG) test else configured.ifBlank { test }
}
