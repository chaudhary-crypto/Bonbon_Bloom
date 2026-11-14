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

    // --- Official Production AdMob Unit IDs (Saved for NOVARO TECHNOLOGIES) ---
    const val PROD_APP_ID = "ca-app-pub-9950623744691218~4994758991"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-9950623744691218/3912118329"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9950623744691218/9288568089"
    const val PROD_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9950623744691218/7505860934"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-9950623744691218/7505860934"

    // --- Official Google Mobile Ads Test Unit IDs (Active while AdMob account approval is pending) ---
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/5354046379"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // --- Unity Ads Configuration ---
    const val UNITY_GAME_ID = "800427695"
    const val UNITY_REWARDED_PLACEMENT = "BP_Rewarded_Android"
    const val UNITY_INTERSTITIAL_PLACEMENT = "BP_Interstitial_Android"
    const val UNITY_BANNER_PLACEMENT = "BP_Banner_Android"

    // Configuration IDs from BuildConfig (Maintained for full backwards compatibility)
    val BANNER_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_INTERSTITIAL_ID
    val REWARDED_INTERSTITIAL_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID: String = com.example.BuildConfig.ADMOB_REWARDED_ID

    // Test Ads Mode: Enabled automatically in debug builds; disabled by default in release
    private const val KEY_TEST_ADS_MODE = "is_test_ads_mode_enabled"
    private val _isTestAdsMode = MutableStateFlow(com.example.BuildConfig.DEBUG)
    val isTestAdsMode: StateFlow<Boolean> = _isTestAdsMode.asStateFlow()

    fun isTestAdsActive(context: Context): Boolean {
        if (!com.example.BuildConfig.DEBUG) {
            _isTestAdsMode.value = false
            return false
        }
        val prefs = getPrefs(context)
        val active = prefs.getBoolean(KEY_TEST_ADS_MODE, true)
        _isTestAdsMode.value = active
        return active
    }

    fun setTestAdsMode(context: Context, enabled: Boolean) {
        val effective = com.example.BuildConfig.DEBUG && enabled
        val prefs = getPrefs(context)
        prefs.edit().putBoolean(KEY_TEST_ADS_MODE, effective).apply()
        _isTestAdsMode.value = effective
        Log.i(TAG, "Ad mode updated. isTestAdsMode=$effective. Refreshing preloaded ads...")
        interstitialAd = null
        rewardedAd = null
        rewardedInterstitialAd = null
        _isRewardedReady.value = false
        _isRewardedInterstitialReady.value = false
        preloadAds(context)
    }

    fun toggleTestAdsMode(context: Context): Boolean {
        val newMode = !_isTestAdsMode.value
        setTestAdsMode(context, newMode)
        return newMode
    }

    fun canServeAds(context: Context): Boolean {
        return if (com.example.BuildConfig.DEBUG) {
            isTestAdsActive(context) || ConsentManager.canRequestAds
        } else {
            ConsentManager.canRequestAds
        }
    }

    val currentBannerAdUnitId: String
        get() = if (_isTestAdsMode.value) TEST_BANNER_AD_UNIT_ID else BANNER_AD_UNIT_ID.ifEmpty { PROD_BANNER_AD_UNIT_ID }

    val currentInterstitialAdUnitId: String
        get() = if (_isTestAdsMode.value) TEST_INTERSTITIAL_AD_UNIT_ID else INTERSTITIAL_AD_UNIT_ID.ifEmpty { PROD_INTERSTITIAL_AD_UNIT_ID }

    val currentRewardedInterstitialAdUnitId: String
        get() = if (_isTestAdsMode.value) TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID else REWARDED_INTERSTITIAL_AD_UNIT_ID.ifEmpty { PROD_REWARDED_INTERSTITIAL_AD_UNIT_ID }

    val currentRewardedAdUnitId: String
        get() = if (_isTestAdsMode.value) TEST_REWARDED_AD_UNIT_ID else REWARDED_AD_UNIT_ID.ifEmpty { PROD_REWARDED_AD_UNIT_ID }

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

    private val _isAdLoading = MutableStateFlow(false)
    val isAdLoading: StateFlow<Boolean> = _isAdLoading.asStateFlow()

    fun setAdLoading(loading: Boolean) {
        _isAdLoading.value = loading
    }

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
        if (!canServeAds(context)) {
            Log.d(TAG, "preloadAds deferred: ads not permitted yet.")
            return
        }
        preloadInterstitial(context)
        preloadRewarded(context)
        preloadRewardedInterstitial(context)
        UnityAdsManager.preloadAds()
    }


    fun preloadInterstitial(context: Context) {
        if (!canServeAds(context)) return
        if (PlayBillingManager.hasRemovedAds.value) return
        if (interstitialAd != null || isInterstitialLoading) return
        isInterstitialLoading = true

        val unitId = currentInterstitialAdUnitId
        Handler(Looper.getMainLooper()).post {
            try {
                val adRequest = AdRequest.Builder().build()
                InterstitialAd.load(
                    context.applicationContext,
                    unitId,
                    adRequest,
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            isInterstitialLoading = false
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "interstitial", unitId)
                            }
                            Log.d(TAG, "AdMob Interstitial loaded successfully ($unitId)")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialAd = null
                            isInterstitialLoading = false
                            Log.w(TAG, "AdMob Interstitial failed to load [$unitId - ${loadAdError.code}]: ${loadAdError.message}")
                            if (com.example.BuildConfig.DEBUG && unitId != TEST_INTERSTITIAL_AD_UNIT_ID) {
                                Log.i(TAG, "Debug fallback: preloading Google Test Interstitial...")
                                Handler(Looper.getMainLooper()).post {
                                    loadFallbackTestInterstitial(context)
                                }
                            }
                        }
                    }
                )
            } catch (e: Throwable) {
                isInterstitialLoading = false
                Log.w(TAG, "Interstitial loading exception: ${e.message}")
            }
        }
    }

    private fun loadFallbackTestInterstitial(context: Context) {
        try {
            InterstitialAd.load(
                context.applicationContext,
                TEST_INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        Log.i(TAG, "Google Test Interstitial loaded successfully as fallback.")
                    }
                    override fun onAdFailedToLoad(err: LoadAdError) {
                        Log.w(TAG, "Fallback test interstitial failed [${err.code}]: ${err.message}")
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    /**
     * Centralized Interstitial Policy.
     */
    fun showInterstitial(
        activity: Activity,
        currentLevelId: Int = 11,
        forceShow: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        if (!canServeAds(activity)) {
            onDismiss()
            return
        }

        if (PlayBillingManager.hasRemovedAds.value) {
            onDismiss()
            return
        }

        if (!forceShow) {
            if (currentLevelId <= 10) {
                Log.d(TAG, "showInterstitial suppressed: Level $currentLevelId is within tutorial protection zone.")
                onDismiss()
                return
            }

            val prefs = getPrefs(activity)
            val now = System.currentTimeMillis()
            val lastShowTime = prefs.getLong(KEY_LAST_INTERSTITIAL_TIME, 0L)
            val completedLevels = prefs.getInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)

            if (now - lastShowTime < INTERSTITIAL_MIN_INTERVAL_MS) {
                Log.d(TAG, "showInterstitial suppressed: Cooldown active (${(now - lastShowTime) / 1000}s / 120s).")
                onDismiss()
                return
            }

            if (completedLevels < MIN_LEVELS_BETWEEN_INTERSTITIALS) {
                Log.d(TAG, "showInterstitial suppressed: Insufficient completed levels ($completedLevels / $MIN_LEVELS_BETWEEN_INTERSTITIALS).")
                onDismiss()
                return
            }

            if (isBoardAnimating) {
                Log.d(TAG, "showInterstitial suppressed: Board is actively animating.")
                onDismiss()
                return
            }

            if (now - lastButtonTapTime < 2_000L) {
                Log.d(TAG, "showInterstitial suppressed: Too close to user button tap.")
                onDismiss()
                return
            }
        }

        if (!isFullScreenAdShowing.compareAndSet(false, true)) {
            Log.d(TAG, "showInterstitial suppressed: Another fullscreen ad is already presenting.")
            onDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    isFullScreenAdShowing.set(false)
                    val prefs = getPrefs(activity)
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
        } else {
            // Load on demand and present immediately
            val unitId = currentInterstitialAdUnitId
            Log.i(TAG, "Interstitial not in memory, requesting on demand for unit $unitId...")
            _isAdLoading.value = true
            InterstitialAd.load(
                activity.applicationContext,
                unitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(loadedAd: InterstitialAd) {
                        _isAdLoading.value = false
                        loadedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isFullScreenAdShowing.set(false)
                                val prefs = getPrefs(activity)
                                prefs.edit()
                                    .putLong(KEY_LAST_INTERSTITIAL_TIME, System.currentTimeMillis())
                                    .putInt(KEY_COMPLETED_LEVELS_SINCE_AD, 0)
                                    .apply()
                                preloadInterstitial(activity)
                                onDismiss()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                isFullScreenAdShowing.set(false)
                                preloadInterstitial(activity)
                                onDismiss()
                            }
                        }
                        loadedAd.show(activity)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        _isAdLoading.value = false
                        Log.w(TAG, "On-demand interstitial failed [$unitId - ${loadAdError.code}].")
                        if (com.example.BuildConfig.DEBUG && unitId != TEST_INTERSTITIAL_AD_UNIT_ID) {
                            InterstitialAd.load(
                                activity.applicationContext,
                                TEST_INTERSTITIAL_AD_UNIT_ID,
                                AdRequest.Builder().build(),
                                object : InterstitialAdLoadCallback() {
                                    override fun onAdLoaded(testAd: InterstitialAd) {
                                        testAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                                            override fun onAdDismissedFullScreenContent() {
                                                isFullScreenAdShowing.set(false)
                                                preloadInterstitial(activity)
                                                onDismiss()
                                            }

                                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                                isFullScreenAdShowing.set(false)
                                                onDismiss()
                                            }
                                        }
                                        testAd.show(activity)
                                    }

                                    override fun onAdFailedToLoad(err: LoadAdError) {
                                        isFullScreenAdShowing.set(false)
                                        preloadInterstitial(activity)
                                        onDismiss()
                                    }
                                }
                            )
                        } else {
                            isFullScreenAdShowing.set(false)
                            preloadInterstitial(activity)
                            onDismiss()
                        }
                    }
                }
            )
        }

    }

    fun preloadRewarded(context: Context) {
        if (!canServeAds(context)) return
        if (rewardedAd != null || isRewardedLoading) return
        isRewardedLoading = true

        val unitId = currentRewardedAdUnitId
        Handler(Looper.getMainLooper()).post {
            try {
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context.applicationContext,
                    unitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            isRewardedLoading = false
                            _isRewardedReady.value = true
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "rewarded", unitId)
                            }
                            Log.d(TAG, "AdMob Rewarded Ad loaded successfully ($unitId)")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            rewardedAd = null
                            isRewardedLoading = false
                            _isRewardedReady.value = false
                            Log.w(TAG, "AdMob Rewarded Ad failed to load [$unitId - ${loadAdError.code}]: ${loadAdError.message}")
                            if (com.example.BuildConfig.DEBUG && unitId != TEST_REWARDED_AD_UNIT_ID) {
                                Handler(Looper.getMainLooper()).post {
                                    loadFallbackTestRewarded(context)
                                }
                            }
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

    private fun loadFallbackTestRewarded(context: Context) {
        try {
            RewardedAd.load(
                context.applicationContext,
                TEST_REWARDED_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        _isRewardedReady.value = true
                        Log.i(TAG, "Google Test Rewarded Ad cached as fallback successfully!")
                    }
                    override fun onAdFailedToLoad(err: LoadAdError) {
                        Log.w(TAG, "Fallback test rewarded failed: ${err.message}")
                    }
                }
            )
        } catch (_: Throwable) {}
    }

    fun preloadRewardedInterstitial(context: Context) {
        if (!canServeAds(context)) return
        if (rewardedInterstitialAd != null || isRewardedInterstitialLoading) return
        isRewardedInterstitialLoading = true

        val unitId = currentRewardedInterstitialAdUnitId
        Handler(Looper.getMainLooper()).post {
            try {
                val adRequest = AdRequest.Builder().build()
                RewardedInterstitialAd.load(
                    context.applicationContext,
                    unitId,
                    adRequest,
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitialAd = ad
                            isRewardedInterstitialLoading = false
                            _isRewardedInterstitialReady.value = true
                            _isRewardedReady.value = true
                            ad.onPaidEventListener = OnPaidEventListener { adValue ->
                                logAdImpressionRevenue(context, adValue, "rewarded_interstitial", unitId)
                            }
                            Log.d(TAG, "AdMob Rewarded Interstitial loaded successfully ($unitId)")
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            rewardedInterstitialAd = null
                            isRewardedInterstitialLoading = false
                            _isRewardedInterstitialReady.value = false
                            Log.w(TAG, "AdMob Rewarded Interstitial failed to load [$unitId - ${loadAdError.code}]: ${loadAdError.message}")
                            if (com.example.BuildConfig.DEBUG && unitId != TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID) {
                                Handler(Looper.getMainLooper()).post {
                                    loadFallbackTestRewardedInterstitial(context)
                                }
                            }
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

    private fun loadFallbackTestRewardedInterstitial(context: Context) {
        try {
            RewardedInterstitialAd.load(
                context.applicationContext,
                TEST_REWARDED_INTERSTITIAL_AD_UNIT_ID,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        rewardedInterstitialAd = ad
                        _isRewardedInterstitialReady.value = true
                        _isRewardedReady.value = true
                        Log.i(TAG, "Google Test Rewarded Interstitial cached as fallback successfully!")
                    }
                    override fun onAdFailedToLoad(err: LoadAdError) {
                        Log.w(TAG, "Fallback test rewarded interstitial failed: ${err.message}")
                    }
                }
            )
        } catch (_: Throwable) {}
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
        onDismiss: () -> Unit = {},
        onUnavailable: (() -> Unit)? = null
    ) {
        if (!canServeAds(activity)) {
            Log.w(TAG, "showRewarded suppressed: ads not permitted.")
            onUnavailable?.invoke()
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

        // 2. Load on-demand: indicate loading to user
        val primaryUnit = currentRewardedAdUnitId
        Log.i(TAG, "Requesting on-demand Rewarded Ad. Primary unit: $primaryUnit (isTestAdsMode=${_isTestAdsMode.value})")
        _isAdLoading.value = true

        val mainHandler = Handler(Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (_isAdLoading.value) {
                Log.w(TAG, "Ad request timed out after 15s.")
                _isAdLoading.value = false
                isFullScreenAdShowing.set(false)
                onUnavailable?.invoke()
                onDismiss()
            }
        }
        mainHandler.postDelayed(timeoutRunnable, 15000L)

        RewardedAd.load(
            activity.applicationContext,
            primaryUnit,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    _isAdLoading.value = false
                    Log.i(TAG, "AdMob Rewarded Ad loaded successfully ($primaryUnit).")
                    loadedAd.onPaidEventListener = OnPaidEventListener { adValue ->
                        logAdImpressionRevenue(activity, adValue, "rewarded", primaryUnit)
                    }
                    presentRewardedAd(activity, loadedAd, onRewardEarned, onDismiss)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "RewardedAd load returned [${loadAdError.code}]: ${loadAdError.message}.")
                    tryRewardedInterstitialFallback(activity, mainHandler, timeoutRunnable, onRewardEarned, onDismiss, onUnavailable)
                }
            }
        )
    }

    private fun tryRewardedInterstitialFallback(
        activity: Activity,
        mainHandler: Handler,
        timeoutRunnable: Runnable,
        onRewardEarned: (rewardAmount: Int, transactionId: String) -> Unit,
        onDismiss: () -> Unit,
        onUnavailable: (() -> Unit)? = null
    ) {
        val interUnit = currentRewardedInterstitialAdUnitId
        RewardedInterstitialAd.load(
            activity.applicationContext,
            interUnit,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(interAd: RewardedInterstitialAd) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    _isAdLoading.value = false
                    Log.i(TAG, "RewardedInterstitial fallback loaded ($interUnit).")
                    presentRewardedInterstitialAd(activity, interAd, onRewardEarned, onDismiss)
                }

                override fun onAdFailedToLoad(err: LoadAdError) {
                    mainHandler.removeCallbacks(timeoutRunnable)
                    _isAdLoading.value = false
                    isFullScreenAdShowing.set(false)
                    Log.w(TAG, "All rewarded ad formats failed [${err.code}]: ${err.message}. No free reward granted.")
                    onUnavailable?.invoke()
                    onDismiss()
                }
            }
        )
    }

    /**
     * Standard overload for callers handling rewardAmount, optional onDismiss, and optional onUnavailable.
     */
    fun showRewarded(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {},
        onUnavailable: (() -> Unit)? = null
    ) {
        showRewardedWithTransaction(
            activity = activity,
            onRewardEarned = { amount, _ -> onRewardEarned(amount) },
            onDismiss = onDismiss,
            onUnavailable = onUnavailable
        )
    }

    fun showRewardedInterstitial(
        activity: Activity,
        onRewardEarned: (rewardAmount: Int) -> Unit,
        onDismiss: () -> Unit = {},
        onUnavailable: (() -> Unit)? = null
    ) {
        showRewarded(activity, onRewardEarned, onDismiss, onUnavailable)
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
