package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UnityAdsManager stub (Compliant with Google Play & third-party ad policies).
 * Unity Ads SDK is disabled; all advertising operations run exclusively via Google Mobile Ads (AdMob).
 * Never grants unpaid fallback rewards when an ad fails to load or is unavailable.
 */
object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    fun initialize(context: Context, testMode: Boolean = false) {
        _isInitialized.value = false
    }

    fun preloadAds() {
        // No-op: AdMob only
    }

    fun loadRewarded(placementId: String = "rewarded", onComplete: ((Boolean) -> Unit)? = null) {
        onComplete?.invoke(false)
    }

    fun loadInterstitial(placementId: String = "interstitial", onComplete: ((Boolean) -> Unit)? = null) {
        onComplete?.invoke(false)
    }

    fun showRewarded(
        activity: Activity,
        placementId: String = "rewarded",
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {},
        onUnavailable: () -> Unit = {}
    ) {
        Log.w(TAG, "Unity Ads stub disabled; no unpaid fallback reward granted.")
        onUnavailable()
        onDismiss()
    }

    fun showInterstitial(
        activity: Activity,
        placementId: String = "interstitial",
        onDismiss: () -> Unit = {}
    ) {
        onDismiss()
    }
}
