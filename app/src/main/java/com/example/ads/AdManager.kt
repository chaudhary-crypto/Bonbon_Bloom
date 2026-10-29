package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.payment.PlayBillingManager
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
 * AdManager coordinates Google Mobile Ads (AdMob) for Bonbon Bloom following Google Play Policies:
 * - No ads at app launch.
 * - No ads on levels 1 to 10 (tutorial & onboarding zone).
 * - Interstitials at most once every 3 completed levels AND at least 120 seconds apart.
 * - Never during gameplay, board movement, or within 2 seconds after a button tap.
 * - Remove Ads purchase immediately disables all banners and interstitials.
 * - Rewarded video ads are strictly user-initiated, optional, clearly labeled, and reward late completions.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob test IDs loaded from BuildConfig placeholders
    // TODO: Configure production AdMob IDs in BuildConfig or .env when publishing
    val BANNER_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_REWARDED_ID

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    // Policy & Experience Control Timestamps & Counters
    private var lastInterstitialShowTime = 0L
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 120_000L // Strict 120-second cooldown
    private var completedLevelsSinceLastInterstitial = 0
    private const val MIN_LEVELS_BETWEEN_INTERSTITIALS = 3
    private var lastButtonTapTime = 0L

    var isBoardAnimating: Boolean = false

    fun registerButtonTap() {
        lastButtonTapTime = System.currentTimeMillis()
    }

    fun registerLevelCompleted(levelId: Int) {
        if (levelId > 10) {
            completedLevelsSinceLastInterstitial++
        }
    }

    fun preloadAds(context: Context) {
        preloadInterstitial(context)
        preloadRewarded(context)
    }

    fun preloadInterstitial(context: Context) {
        if (PlayBillingManager.hasRemovedAds.value) return
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        Handler(Looper.getMainLooper()).post {
            try {
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    context.applicationContext,
                    INTERSTITIAL_AD_UNIT_ID,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isInterstitialLoading = false
                            Log.d(TAG, "AdMob Interstitial loaded successfully")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialAd = null
                            isInterstitialLoading = false
                            Log.w(TAG, "AdMob Interstitial failed to load: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Throwable) {
                isInterstitialLoading = false
            }
        }
    }

    /**
     * Shows an interstitial between completed levels only if all policy constraints are met:
     * 1. Remove Ads is NOT purchased
     * 2. Level is > 10
     * 3. At least 3 levels completed since last interstitial
     * 4. At least 120 seconds have elapsed since last interstitial
     * 5. Board is not currently animating or in active gameplay
     * 6. At least 2 seconds have passed since any button was tapped
     */
    fun showInterstitial(
        activity: Activity,
        currentLevelId: Int = 11,
        force: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        // 1. Check Remove Ads purchase
        if (PlayBillingManager.hasRemovedAds.value) {
            onDismiss()
            return
        }

        // 2. No ads on levels 1 to 10
        if (currentLevelId <= 10 && !force) {
            onDismiss()
            return
        }

        val now = System.currentTimeMillis()

        // 3. Cooldown: at least 120 seconds apart
        if (!force && (now - lastInterstitialShowTime < INTERSTITIAL_MIN_INTERVAL_MS)) {
            onDismiss()
            return
        }

        // 4. Frequency: at least 3 completed levels
        if (!force && completedLevelsSinceLastInterstitial < MIN_LEVELS_BETWEEN_INTERSTITIALS) {
            onDismiss()
            return
        }

        // 5. Never during active board animation
        if (isBoardAnimating) {
            onDismiss()
            return
        }

        // 6. Never within 2 seconds after player tapped a button
        if (now - lastButtonTapTime < 2_000L && !force) {
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    lastInterstitialShowTime = System.currentTimeMillis()
                    completedLevelsSinceLastInterstitial = 0
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
            preloadInterstitial(activity)
            onDismiss()
        }
    }

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        Handler(Looper.getMainLooper()).post {
            try {
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
            } catch (e: Throwable) {
                isRewardedLoading = false
                _isRewardedReady.value = false
            }
        }
    }

    /**
     * Show optional player-initiated rewarded video ad:
     * Always clearly labeled (e.g. "Watch Ad: +5 Moves" or "Watch Ad: +1 Life").
     * Always grants the reward reliably even if the ad completes late or fails during playback.
     */
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
                    // Grant reward even if playback fails so user effort isn't wasted
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
            // If ad network has temporary latency, gracefully grant fallback reward and reload
            preloadRewarded(activity)
            onRewardEarned(1)
            onDismiss()
        }
    }
}
