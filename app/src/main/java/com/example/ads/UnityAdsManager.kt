package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * UnityAdsManager coordinates direct Unity Ads SDK operations alongside Google AdMob mediation:
 * - Game ID: 800427695
 * - Placements:
 *     - Rewarded: BP_Rewarded_Android
 *     - Interstitial: BP_Interstitial_Android
 *     - Banner: BP_Banner_Android
 * - Automatically initializes Unity Ads for both mediation adapter bidding and direct fallback.
 */
object UnityAdsManager {
    private const val TAG = "UnityAdsManager"

    val GAME_ID: String = BuildConfig.UNITY_GAME_ID
    val REWARDED_PLACEMENT: String = BuildConfig.UNITY_REWARDED_PLACEMENT
    val INTERSTITIAL_PLACEMENT: String = BuildConfig.UNITY_INTERSTITIAL_PLACEMENT
    val BANNER_PLACEMENT: String = BuildConfig.UNITY_BANNER_PLACEMENT

    private val isInitializing = AtomicBoolean(false)
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    /**
     * Initializes Unity Ads SDK.
     */
    fun initialize(context: Context, testMode: Boolean = false) {
        if (_isInitialized.value || isInitializing.getAndSet(true)) return

        Log.i(TAG, "Initializing Unity Ads SDK with Game ID: $GAME_ID (testMode=$testMode)")
        try {
            UnityAds.initialize(
                context.applicationContext,
                GAME_ID,
                testMode,
                object : IUnityAdsInitializationListener {
                    override fun onInitializationComplete() {
                        isInitializing.set(false)
                        _isInitialized.value = true
                        Log.i(TAG, "Unity Ads successfully initialized.")
                        preloadAds()
                    }

                    override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                        isInitializing.set(false)
                        _isInitialized.value = false
                        Log.w(TAG, "Unity Ads initialization failed [$error]: $message")
                    }
                }
            )
        } catch (e: Throwable) {
            isInitializing.set(false)
            Log.w(TAG, "Unity Ads initialization exception: ${e.message}")
        }
    }

    /**
     * Preloads Unity Ads placements for instant availability.
     */
    fun preloadAds() {
        if (!_isInitialized.value) return
        loadRewarded()
        loadInterstitial()
    }

    fun loadRewarded(placementId: String = REWARDED_PLACEMENT, onComplete: ((Boolean) -> Unit)? = null) {
        if (!_isInitialized.value) {
            onComplete?.invoke(false)
            return
        }
        UnityAds.load(
            placementId,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placement: String) {
                    Log.i(TAG, "Unity Rewarded ad loaded for placement: $placement")
                    _isRewardedReady.value = true
                    onComplete?.invoke(true)
                }

                override fun onUnityAdsFailedToLoad(placement: String, error: UnityAds.UnityAdsLoadError, message: String) {
                    Log.w(TAG, "Unity Rewarded failed to load [$error]: $message for $placement")
                    _isRewardedReady.value = false
                    onComplete?.invoke(false)
                }
            }
        )
    }

    fun loadInterstitial(placementId: String = INTERSTITIAL_PLACEMENT, onComplete: ((Boolean) -> Unit)? = null) {
        if (!_isInitialized.value) {
            onComplete?.invoke(false)
            return
        }
        UnityAds.load(
            placementId,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placement: String) {
                    Log.i(TAG, "Unity Interstitial ad loaded for placement: $placement")
                    _isInterstitialReady.value = true
                    onComplete?.invoke(true)
                }

                override fun onUnityAdsFailedToLoad(placement: String, error: UnityAds.UnityAdsLoadError, message: String) {
                    Log.w(TAG, "Unity Interstitial failed to load [$error]: $message for $placement")
                    _isInterstitialReady.value = false
                    onComplete?.invoke(false)
                }
            }
        )
    }

    /**
     * Shows Unity Rewarded ad with completion reward callback.
     */
    fun showRewarded(
        activity: Activity,
        placementId: String = REWARDED_PLACEMENT,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        if (!_isInitialized.value) {
            Log.w(TAG, "Cannot show Unity Rewarded: SDK not initialized.")
            onDismiss()
            return
        }

        val showOptions = UnityAdsShowOptions()
        UnityAds.show(
            activity,
            placementId,
            showOptions,
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placement: String) {
                    Log.i(TAG, "Unity Rewarded show started: $placement")
                    _isRewardedReady.value = false
                }

                override fun onUnityAdsShowClick(placement: String) {
                    Log.d(TAG, "Unity Rewarded clicked: $placement")
                }

                override fun onUnityAdsShowComplete(placement: String, state: UnityAds.UnityAdsShowCompletionState) {
                    Log.i(TAG, "Unity Rewarded show completed: $placement, state: $state")
                    _isRewardedReady.value = false
                    loadRewarded(placementId)
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        onRewardEarned(1)
                    }
                    onDismiss()
                }

                override fun onUnityAdsShowFailure(placement: String, error: UnityAds.UnityAdsShowError, message: String) {
                    Log.w(TAG, "Unity Rewarded show failed [$error]: $message")
                    _isRewardedReady.value = false
                    loadRewarded(placementId)
                    onDismiss()
                }
            }
        )
    }

    /**
     * Shows Unity Interstitial ad.
     */
    fun showInterstitial(
        activity: Activity,
        placementId: String = INTERSTITIAL_PLACEMENT,
        onDismiss: () -> Unit = {}
    ) {
        if (!_isInitialized.value) {
            onDismiss()
            return
        }

        UnityAds.show(
            activity,
            placementId,
            UnityAdsShowOptions(),
            object : IUnityAdsShowListener {
                override fun onUnityAdsShowStart(placement: String) {
                    Log.i(TAG, "Unity Interstitial started: $placement")
                    _isInterstitialReady.value = false
                }

                override fun onUnityAdsShowClick(placement: String) {
                    Log.d(TAG, "Unity Interstitial clicked: $placement")
                }

                override fun onUnityAdsShowComplete(placement: String, state: UnityAds.UnityAdsShowCompletionState) {
                    Log.i(TAG, "Unity Interstitial completed: $placement")
                    _isInterstitialReady.value = false
                    loadInterstitial(placementId)
                    onDismiss()
                }

                override fun onUnityAdsShowFailure(placement: String, error: UnityAds.UnityAdsShowError, message: String) {
                    Log.w(TAG, "Unity Interstitial failed to show [$error]: $message")
                    _isInterstitialReady.value = false
                    loadInterstitial(placementId)
                    onDismiss()
                }
            }
        )
    }
}
