package com.ai4biz.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows rewarded ads. Used to let free-tier users unlock an
 * extra generation once their daily allowance runs out (see
 * [com.ai4biz.app.data.repository.UsageRepository]).
 */
class RewardedAdManager(private val context: Context) {

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            AdsConfig.rewardedUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun isReady(): Boolean = rewardedAd != null

    /** [onEarned] fires only if the user actually watched the ad to completion. */
    fun showIfReady(activity: Activity, onEarned: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            preload()
            onUnavailable()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                preload()
                onUnavailable()
            }
        }
        ad.show(activity) { onEarned() }
    }
}
