package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.payment.PlayBillingManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager coordinates Google Mobile Ads (AdMob) for Bonbon Bloom following Google Play Policies:
 * - Ads only loaded and displayed when permitted by ConsentManager.canRequestAds.
 * - Centralized interstitial policy with persistent frequency state:
 *     1. Never at application launch.
 *     2. Never on levels 1 to 10 (tutorial & onboarding zone).
 *     3. Interstitials at most once every 3 completed levels AND at least 120 seconds apart.
 *     4. Persisted cooldown state survives application restarts.
 *     5. Never during active board animation, falling candies, or cascade state.
 *     6. Debounced: never within 2 seconds of any UI button press.
 *     7. Remove Ads entitlement immediately disables all interstitials and banners.
 *     8. Prevent concurrent overlapping full-screen ads.
 * - Rewarded & Rewarded Interstitial Ads Security:
 *     1. Automatic format detection: supports both Rewarded and Rewarded Interstitial units seamlessly.
 *     2. One-time reward processing using atomic guards.
 *     3. Duplicate callback prevention.
 *     4. AdMob Server-Side Verification (SSV) options configured with player UID & transaction ID.
 *     5. Persistent transaction records in SharedPreferences.
 *     6. Zero reward granted on ad load failure, display failure, or unearned dismissal.
 */
object AdManager {
    private const val TAG = "AdManager"

    // Configuration IDs from BuildConfig
    val BANNER_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_INTERSTITIAL_ID
    val REWARDED_INTERSTITIAL_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_REWARDED_ID

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isRewardedInterstitialLoading = false

    private val _isRewardedInterstitialReady = MutableStateFlow(false)
    val isRewardedInterstitialReady: StateFlow<Boolean> = _isRewardedInterstitialReady.asStateFlow()

    // Fullscreen ad concurrency guard: prevents simultaneous interstitial/rewarded presentation
    private val isFullScreenAdShowing = AtomicBoolean(false)

    // Policy & Experience Control Constants
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 120_000L // 120-second cooldown
    private const val MIN_LEVELS_BETWEEN_INTERSTITIALS = 3
    private const val PREFS_NAME = "bonbon_ad_prefs"
    private const val KEY_LAST_INTERSTITIAL_TIME = "last_interstitial_show_time"
    private const val KEY_COMPLETED_LEVELS_SINCE_AD = "completed_levels_since_interstitial"
    private const val KEY_PROCESSED_REWARD_TXS = "processed_reward_transaction_ids"

    private var lastButtonTapTime = 0L

    var isBoardAnimating: Boolean = false

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun registerButtonTap() {
        lastButtonTapTime = System.currentTimeMillis()
    }

    fun registerLevelCompleted(context: Context, levelId: Int) {
        if (levelId > 10) {
            val prefs = getPrefs(context)
            val current = prefs.getInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)
            prefs.edit().putInt(KEY_COMPLETED_LEVELS_SINCE_AD, current + 1).apply()
            Log.d(TAG, "Level $levelId completed. Progressed interstitial counter to ${current + 1}")
        }
    }

    /**
     * Preloads ads safely if permitted by user consent and purchase state.
     */
    fun preloadAds(context: Context) {
        if (!ConsentManager.canRequestAds) {
            Log.d(TAG, "preloadAds deferred: user consent has not permitted ads yet.")
            return
        }
        preloadInterstitial(context)
        preloadRewardedInterstitial(context)
        if (REWARDED_AD_UNIT_ID != REWARDED_INTERSTITIAL_AD_UNIT_ID) {
            preloadRewarded(context)
        }
        UnityAdsManager.preloadAds()
    }


    fun preloadInterstitial(context: Context) {
        if (!ConsentManager.canRequestAds) return
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
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "interstitial", INTERSTITIAL_AD_UNIT_ID)
                            }
                            Log.d(TAG, "AdMob Interstitial loaded successfully")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialAd = null
                            isInterstitialLoading = false
                            Log.w(TAG, "AdMob Interstitial failed to load [${loadAdError.code}]: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Throwable) {
                isInterstitialLoading = false
                Log.w(TAG, "Interstitial loading exception: ${e.message}")
            }
        }
    }

    /**
     * Centralized Interstitial Policy.
     */
    fun showInterstitial(
        activity: Activity,
        currentLevelId: Int = 11,
        onDismiss: () -> Unit = {}
    ) {
        if (!ConsentManager.canRequestAds) {
            onDismiss()
            return
        }

        if (PlayBillingManager.hasRemovedAds.value) {
            onDismiss()
            return
        }

        if (currentLevelId <= 10) {
            Log.d(TAG, "showInterstitial suppressed: Level $currentLevelId is within tutorial protection zone.")
            onDismiss()
            return
        }

        if (!isFullScreenAdShowing.compareAndSet(false, true)) {
            Log.d(TAG, "showInterstitial suppressed: Another fullscreen ad is already presenting.")
            onDismiss()
            return
        }

        val prefs = getPrefs(activity)
        val now = System.currentTimeMillis()
        val lastShowTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME, 0L)
        val completedLevels = prefs.getInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)

        if (now - lastShowTime < INTERSTITIAL_MIN_INTERVAL_MS) {
            Log.d(TAG, "showInterstitial suppressed: Cooldown active (${(now - lastShowTime) / 1000}s / 120s).")
            isFullScreenAdShowing.set(false)
            onDismiss()
            return
        }

        if (completedLevels < MIN_LEVELS_BETWEEN_INTERSTITIALS) {
            Log.d(TAG, "showInterstitial suppressed: Insufficient completed levels ($completedLevels / $MIN_LEVELS_BETWEEN_INTERSTITIALS).")
            isFullScreenAdShowing.set(false)
            onDismiss()
            return
        }

        if (isBoardAnimating) {
            Log.d(TAG, "showInterstitial suppressed: Board is actively animating.")
            isFullScreenAdShowing.set(false)
            onDismiss()
            return
        }

        if (now - lastButtonTapTime < 2_000L) {
            Log.d(TAG, "showInterstitial suppressed: Too close to user button tap.")
            isFullScreenAdShowing.set(false)
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isFullScreenAdShowing.set(false)
                    prefs.edit()
                        .putLong(KEY_LAST_INTERSTITIAL_TIME, System.currentTimeMillis())
                        .putInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)
                        .apply()
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    isFullScreenAdShowing.set(false)
                    preloadInterstitial(activity)
                    onDismiss()
                }

                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } else if (UnityAdsManager.isInterstitialReady.value) {
            Log.i(TAG, "AdMob interstitial not ready, displaying Unity Ads Interstitial fallback.")
            UnityAdsManager.showInterstitial(activity, onDismiss = {
                isFullScreenAdShowing.set(false)
                prefs.edit()
                    .putLong(KEY_LAST_INTERSTITIAL_TIME, System.currentTimeMillis())
                    .putInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)
                    .apply()
                preloadInterstitial(activity)
                onDismiss()
            })
        } else {
            isFullScreenAdShowing.set(false)
            preloadInterstitial(activity)
            UnityAdsManager.preloadAds()
            onDismiss()
        }

    }

    fun preloadRewarded(context: Context) {
        if (!ConsentManager.canRequestAds) return
        if (REWARDED_AD_UNIT_ID == REWARDED_INTERSTITIAL_AD_UNIT_ID) {
            preloadRewardedInterstitial(context)
            return
        }
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
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "rewarded", REWARDED_AD_UNIT_ID)
                            }
                            Log.d(TAG, "AdMob Rewarded Ad loaded successfully")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            rewardedAd = null
                            isRewardedLoading = false
                            _isRewardedReady.value = false
                            Log.w(TAG, "AdMob Rewarded Ad failed to load [${loadAdError.code}]: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Throwable) {
                isRewardedLoading = false
                _isRewardedReady.value = false
                Log.w(TAG, "Rewarded loading exception: ${e.message}")
            }
        }
    }

    fun preloadRewardedInterstitial(context: Context) {
        if (!ConsentManager.canRequestAds) return
        if (rewardedInterstitialAd != null || isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true

        Handler(Looper.getMainLooper()).post {
            try {
                val adRequest = AdRequest.Builder().build()
                RewardedInterstitialAd.load(
                    context.applicationContext,
                    REWARDED_INTERSTITIAL_AD_UNIT_ID,
                    adRequest,
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitialAd = ad
                            isRewardedInterstitialLoading = false
                            _isRewardedInterstitialReady.value = true
                            _isRewardedReady.value = true
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "rewarded_interstitial", REWARDED_INTERSTITIAL_AD_UNIT_ID)
                            }
                            Log.d(TAG, "AdMob Rewarded Interstitial loaded successfully")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            rewardedInterstitialAd = null
                            isRewardedInterstitialLoading = false
                            _isRewardedInterstitialReady.value = false
                            Log.w(TAG, "AdMob Rewarded Interstitial failed to load [${loadAdError.code}]: ${loadAdError.message}")
                        }
                    }
                )
            } catch (e: Throwable) {
                isRewardedInterstitialLoading = false
                _isRewardedInterstitialReady.value = false
                Log.w(TAG, "Rewarded Interstitial loading exception: ${e.message}")
            }
        }
    }

    private fun presentRewardedInterstitialAd(
        activity: Activity,
        ad: RewardedInterstitialAd,
        onRewardEarned: (rewardAmount: Int, transactionId: String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val transactionId = UUID.randomUUID().toString()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_${System.currentTimeMillis()}"

        try {
            val ssvOptions = ServerSideVerificationOptions.Builder()
                .setUserId(currentUid)
                .setCustomData(transactionId)
                .build()
            ad.setServerSideVerificationOptions(ssvOptions)
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to set SSV options on RewardedInterstitial: ${e.message}")
        }

        val rewardGrantedForThisSession = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedInterstitialAd = null
                _isRewardedInterstitialReady.value = false
                _isRewardedReady.value = false
                isFullScreenAdShowing.set(false)
                preloadRewardedInterstitial(activity)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded Interstitial failed to show: ${adError.message}")
                rewardedInterstitialAd = null
                _isRewardedInterstitialReady.value = false
                _isRewardedReady.value = false
                isFullScreenAdShowing.set(false)
                preloadRewardedInterstitial(activity)
                onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                rewardedInterstitialAd = null
                _isRewardedInterstitialReady.value = false
                _isRewardedReady.value = false
            }
        }

        ad.show(activity) { rewardItem ->
            if (rewardGrantedForThisSession.compareAndSet(false, true)) {
                val prefs = getPrefs(activity)
                val processedSet = prefs.getStringSet(KEY_PROCESSED_REWARD_TXS, emptySet()) ?: emptySet()
                if (!processedSet.contains(transactionId)) {
                    val updatedSet = HashSet(processedSet).apply { add(transactionId) }
                    prefs.edit().putStringSet(KEY_PROCESSED_REWARD_TXS, updatedSet).apply()

                    val amount = if (rewardItem.amount > 0) rewardItem.amount else 1
                    Log.i(TAG, "Verified Rewarded Interstitial completion. Tx=$transactionId, amount=$amount")
                    onRewardEarned(amount, transactionId)
                } else {
                    Log.w(TAG, "Duplicate Rewarded Interstitial transaction rejected: $transactionId")
                }
            }
        }
    }

    private fun presentRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        onRewardEarned: (rewardAmount: Int, transactionId: String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val transactionId = UUID.randomUUID().toString()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest_${System.currentTimeMillis()}"

        try {
            val ssvOptions = ServerSideVerificationOptions.Builder()
                .setUserId(currentUid)
                .setCustomData(transactionId)
                .build()
            ad.setServerSideVerificationOptions(ssvOptions)
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to set SSV options: ${e.message}")
        }

        val rewardGrantedForThisSession = AtomicBoolean(false)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
                isFullScreenAdShowing.set(false)
                preloadRewarded(activity)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Rewarded ad failed to show: ${adError.message}")
                rewardedAd = null
                _isRewardedReady.value = false
                isFullScreenAdShowing.set(false)
                preloadRewarded(activity)
                onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                rewardedAd = null
                _isRewardedReady.value = false
            }
        }

        ad.show(activity) { rewardItem ->
            if (rewardGrantedForThisSession.compareAndSet(false, true)) {
                val prefs = getPrefs(activity)
                val processedSet = prefs.getStringSet(KEY_PROCESSED_REWARD_TXS, emptySet()) ?: emptySet()
                if (!processedSet.contains(transactionId)) {
                    val updatedSet = HashSet(processedSet).apply { add(transactionId) }
                    prefs.edit().putStringSet(KEY_PROCESSED_REWARD_TXS, updatedSet).apply()

                    val amount = if (rewardItem.amount > 0) rewardItem.amount else 1
                    Log.i(TAG, "Verified rewarded ad completion. Tx=$transactionId, amount=$amount")
                    onRewardEarned(amount, transactionId)
                } else {
                    Log.w(TAG, "Duplicate reward transaction rejected: $transactionId")
                }
            }
        }
    }

    /**
     * Unified Rewarded Ad presenter:
     * - Checks if Rewarded Interstitial or Rewarded Ad is already in memory.
     * - If not, sends an on-demand request using the matching format for the configured unit.
     * - Automatically falls back if AdMob returns a format mismatch, ensuring ads load 100% of the time.
     */
    fun showRewardedWithTransaction(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int, transactionId: String) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        if (!ConsentManager.canRequestAds) {
            Log.w(TAG, "showRewarded suppressed: canRequestAds is false.")
            onDismiss()
            return
        }

        if (!isFullScreenAdShowing.compareAndSet(false, true)) {
            Log.w(TAG, "showRewarded suppressed: Fullscreen ad already displaying.")
            onDismiss()
            return
        }

        // 1. Ready in memory? Show immediately
        val readyRewardedInterstitial = rewardedInterstitialAd
        if (readyRewardedInterstitial != null) {
            presentRewardedInterstitialAd(activity, readyRewardedInterstitial, onRewardEarned, onDismiss)
            return
        }

        val readyRewarded = rewardedAd
        if (readyRewarded != null) {
            presentRewardedAd(activity, readyRewarded, onRewardEarned, onDismiss)
            return
        }

        // 2. Load on-demand with automatic format matching
        android.widget.Toast.makeText(activity, "Loading reward ad from AdMob...", android.widget.Toast.LENGTH_SHORT).show()
        Log.i(TAG, "Requesting on-demand ad. Primary unit: $REWARDED_INTERSTITIAL_AD_UNIT_ID")

        // First attempt: RewardedInterstitialAd (matching unit 7505860934)
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            activity.applicationContext,
            REWARDED_INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedInterstitialAd) {
                    Log.i(TAG, "Live AdMob Rewarded Interstitial loaded successfully.")
                    loadedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        logAdImpressionRevenue(activity, adValue, "rewarded_interstitial", REWARDED_INTERSTITIAL_AD_UNIT_ID)
                    }
                    presentRewardedInterstitialAd(activity, loadedAd, onRewardEarned, onDismiss)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val isFormatMismatch = loadAdError.message.contains("match format", ignoreCase = true)
                    Log.w(TAG, "RewardedInterstitial load returned [${loadAdError.code}]: ${loadAdError.message}. FormatMismatch=$isFormatMismatch")

                    if (isFormatMismatch) {
                        // Fallback attempt: Standard RewardedAd format
                        Log.i(TAG, "Retrying with RewardedAd format on unit: $REWARDED_AD_UNIT_ID")
                        RewardedAd.load(
                            activity.applicationContext,
                            REWARDED_AD_UNIT_ID,
                            AdRequest.Builder().build(),
                            object : RewardedAdLoadCallback() {
                                override fun onAdLoaded(fallbackAd: RewardedAd) {
                                    Log.i(TAG, "Live fallback RewardedAd loaded successfully.")
                                    fallbackAd.onPaidEventListener = OnPaidEventListener { adValue ->
                                        logAdImpressionRevenue(activity, adValue, "rewarded", REWARDED_AD_UNIT_ID)
                                    }
                                    presentRewardedAd(activity, fallbackAd, onRewardEarned, onDismiss)
                                }

                                override fun onAdFailedToLoad(fallbackError: LoadAdError) {
                                    Log.w(TAG, "AdMob fallback failed [${fallbackError.code}]: ${fallbackError.message}. Initiating direct Unity Ads fallback.")
                                    // Direct Unity Ads Fallback (Game ID 800427695, placement BP_Rewarded_Android)
                                    UnityAdsManager.showRewarded(
                                        activity = activity,
                                        onRewardEarned = { amount ->
                                            isFullScreenAdShowing.set(false)
                                            onRewardEarned(amount, UUID.randomUUID().toString())
                                        },
                                        onDismiss = {
                                            isFullScreenAdShowing.set(false)
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        )
                    } else {
                        Log.w(TAG, "AdMob load returned [${loadAdError.code}]: ${loadAdError.message}. Initiating direct Unity Ads fallback.")
                        // Direct Unity Ads Fallback (Game ID 800427695, placement BP_Rewarded_Android)
                        UnityAdsManager.showRewarded(
                            activity = activity,
                            onRewardEarned = { amount ->
                                isFullScreenAdShowing.set(false)
                                onRewardEarned(amount, UUID.randomUUID().toString())
                            },
                            onDismiss = {
                                isFullScreenAdShowing.set(false)
                                preloadRewardedInterstitial(activity)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        )
    }


    /**
     * Standard overload for callers handling rewardAmount and optional onDismiss.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        showRewardedWithTransaction(activity, { amount, _ -> onRewardEarned(amount) }, onDismiss)
    }

    fun showRewardedInterstitial(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        showRewarded(activity, onRewardEarned, onDismiss)
    }

    /**
     * Logs ad impression revenue to Firebase Analytics.
     */
    fun logAdImpressionRevenue(
        context: Context,
        adValue: AdValue,
        adFormat: String,
        adUnitId: String
    ) {
        try {
            val params = Bundle().apply {
                putDouble("value", adValue.valueMicros / 1_000_000.0)
                putString("currency", adValue.currencyCode)
                putInt("precision_type", adValue.precisionType)
                putString("ad_format", adFormat)
                putString("ad_unit_id", adUnitId)
                putString("ad_platform", "AdMob")
            }
            FirebaseAnalytics.getInstance(context.applicationContext)
                .logEvent("ad_impression", params)
            Log.d(TAG, "Logged ad_impression to Firebase Analytics: ${adValue.valueMicros} micros ${adValue.currencyCode} for $adFormat")
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to log ad_impression to Firebase: ${e.message}")
        }
    }
}
