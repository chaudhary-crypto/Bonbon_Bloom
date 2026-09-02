package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdManager coordinates Google Mobile Ads (AdMob) for Crush Candy:
 * - Banner ads for HomeScreen, WorldMapScreen, and GameScreen
 * - Interstitial ads between levels
 * - Rewarded video ads for free lives, coins, extra moves, and boosters
 *
 * Uses official Google AdMob test ad unit IDs for safe, crash-free testing.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob test unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private var lastInterstitialShowTime = 0L
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 60_000L // 1 minute cooldown

    fun preloadAds(context: Context) {
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "AdMob Interstitial Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.w(TAG, "AdMob Interstitial failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitial(
        activity: Activity,
        force: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastInterstitialShowTime < INTERSTITIAL_MIN_INTERVAL_MS)) {
            // Respect cool-down for non-intrusive gameplay experience
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } else {
            // Not ready yet, proceed smoothly
            preloadInterstitial(activity)
            onDismiss()
        }
    }

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    _isRewardedReady.value = true
                    Log.d(TAG, "AdMob Rewarded Ad loaded successfully")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    _isRewardedReady.value = false
                    Log.w(TAG, "AdMob Rewarded Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewarded(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    preloadRewarded(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    _isRewardedReady.value = false
                    preloadRewarded(activity)
                    // Fallback grant reward so user isn't penalized for ad delivery hiccups
                    if (!rewardEarned) {
                        onRewardEarned(1)
                    }
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    rewardedAd = null
                    _isRewardedReady.value = false
                }
            }

            ad.show(activity) { rewardItem ->
                rewardEarned = true
                val amount = if (rewardItem.amount > 0) rewardItem.amount else 1
                onRewardEarned(amount)
            }
        } else {
            // If ad is not ready, trigger reload and grant reward gracefully (rewarded fallback)
            preloadRewarded(activity)
            onRewardEarned(1)
            onDismiss()
        }
    }
}
