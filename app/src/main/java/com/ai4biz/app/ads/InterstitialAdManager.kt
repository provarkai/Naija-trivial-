package com.ai4biz.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows interstitial ads, reloading automatically after each
 * show/failure so there's usually a fresh ad ready. [GeneratorViewModel]
 * (via [com.ai4biz.app.data.repository.UsageRepository]) decides *when* to
 * call [showIfReady] -- every [SHOW_EVERY_N] successful generations, not
 * every single one, to keep the ad experience from feeling aggressive.
 */
class InterstitialAdManager(private val context: Context) {

    companion object {
        const val SHOW_EVERY_N = 3
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var generationsSinceLastAd = 0

    /**
     * Call once per successful generation. Returns true every [SHOW_EVERY_N]
     * calls, meaning "show an interstitial now" -- keeps the ad cadence
     * capped instead of showing one after every single generation.
     */
    fun registerGenerationAndShouldShow(): Boolean {
        generationsSinceLastAd++
        if (generationsSinceLastAd >= SHOW_EVERY_N) {
            generationsSinceLastAd = 0
            return true
        }
        return false
    }

    fun preload() {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            AdsConfig.interstitialUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /** Shows the ad if one is loaded; otherwise just invokes [onDismissed] and starts preloading for next time. */
    fun showIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preload()
            onDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                preload()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                preload()
                onDismissed()
            }
        }
        ad.show(activity)
    }
}
