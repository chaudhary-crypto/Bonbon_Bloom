package com.example.payment

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.*
import com.example.auth.CloudSyncManager
import com.example.data.UserDao
import com.example.data.UserRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

data class ProductOfferUi(
    val productId: String,
    val isSubscription: Boolean,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val billingPeriodIso: String? = null,
    val offerToken: String? = null
)

sealed class BillingUiState {
    data object Idle : BillingUiState()
    data class Connecting(val message: String = "Connecting to Google Play...") : BillingUiState()
    data object Ready : BillingUiState()
    data class Launching(val productId: String) : BillingUiState()
    data class PendingPayment(val productId: String, val orderId: String) : BillingUiState()
    data class VerifyingServer(val productId: String) : BillingUiState()
    data class Success(
        val productId: String,
        val isSubscription: Boolean,
        val verifiedByServer: Boolean
    ) : BillingUiState()
    data object Cancelled : BillingUiState()
    data class Unavailable(
        val reason: String,
        val responseCode: Int? = null,
        val productId: String? = null,
        val isSubscription: Boolean = false
    ) : BillingUiState()
    data class Error(val message: String, val responseCode: Int? = null) : BillingUiState()
}

/**
 * PlayBillingManager coordinates Google Play Billing Library 8.0.0 for Bonbon Bloom:
 * - Preloads live ProductDetails & localized Play Console pricing for all SUBS and INAPP SKUs.
 * - Automatic connection retry & actionable unavailability feedback (no silent no-ops, no free grants).
 * - Strict purchaseState == PURCHASED verification with Cloud Function receipt check + Room ledger deduplication.
 * - Crash-resilient acknowledgment (SUBS / Remove Ads) and consumption (Coins / Diamonds / Starter Pack).
 * - Startup & resume entitlement restoration + revocation enforcement.
 */
object PlayBillingManager {
    private const val TAG = "PlayBillingManager"

    // Subscriptions
    const val PRODUCT_VIP_WEEKLY = "vip_weekly"
    const val PRODUCT_VIP_MONTHLY = "vip_monthly"

    // Non-consumable
    const val PRODUCT_REMOVE_ADS = "remove_ads"

    // Consumables
    const val PRODUCT_COINS_500 = "coins_500"
    const val PRODUCT_COINS_1500 = "coins_1500"
    const val PRODUCT_COINS_5000 = "coins_5000"
    const val PRODUCT_GEMS_50 = "gems_50"
    const val PRODUCT_GEMS_150 = "gems_150"
    const val PRODUCT_GEMS_200 = "gems_200"
    const val PRODUCT_GEMS_500 = "gems_500"
    const val PRODUCT_GEMS_600 = "gems_600"
    const val PRODUCT_GEMS_1500 = "gems_1500"
    const val PRODUCT_STARTER_PACK = "booster_starter_pack"

    val ALL_SUBSCRIPTION_SKUS = listOf(PRODUCT_VIP_WEEKLY, PRODUCT_VIP_MONTHLY)
    val ALL_INAPP_SKUS = listOf(
        PRODUCT_REMOVE_ADS,
        PRODUCT_COINS_500,
        PRODUCT_COINS_1500,
        PRODUCT_COINS_5000,
        PRODUCT_GEMS_50,
        PRODUCT_GEMS_150,
        PRODUCT_GEMS_200,
        PRODUCT_GEMS_500,
        PRODUCT_GEMS_600,
        PRODUCT_GEMS_1500,
        PRODUCT_STARTER_PACK
    )

    private const val PREFS_NAME = "bonbon_billing_prefs"
    private const val KEY_PROCESSED_TOKENS = "processed_purchase_tokens"

    private var billingClient: BillingClient? = null
    @Volatile
    private var isConnected = false
    @Volatile
    private var lastSetupErrorCode: Int? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var appContext: Context? = null
    private var userRepository: UserRepository? = null
    private val cachedProductDetails = ConcurrentHashMap<String, ProductDetails>()

    private val _billingState = MutableStateFlow<BillingUiState>(BillingUiState.Idle)
    val billingState: StateFlow<BillingUiState> = _billingState.asStateFlow()

    private val _productOffers = MutableStateFlow<Map<String, ProductOfferUi>>(emptyMap())
    val productOffers: StateFlow<Map<String, ProductOfferUi>> = _productOffers.asStateFlow()

    private val _purchasedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val purchasedProductIds: StateFlow<Set<String>> = _purchasedProductIds.asStateFlow()

    private val _hasRemovedAds = MutableStateFlow(false)
    val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    private val _isVipActive = MutableStateFlow(false)
    val isVipActive: StateFlow<Boolean> = _isVipActive.asStateFlow()

    var onPurchaseCompleted: ((productId: String, isSubscription: Boolean) -> Unit)? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun isTokenProcessed(context: Context, token: String): Boolean {
        val set = getPrefs(context).getStringSet(KEY_PROCESSED_TOKENS, emptySet()) ?: emptySet()
        return set.contains(token)
    }

    private fun markTokenProcessed(context: Context, token: String) {
        val prefs = getPrefs(context)
        val current = prefs.getStringSet(KEY_PROCESSED_TOKENS, emptySet()) ?: emptySet()
        val updated = HashSet(current).apply { add(token) }
        prefs.edit().putStringSet(KEY_PROCESSED_TOKENS, updated).apply()
    }

    fun dismissBillingState() {
        _billingState.value = if (isConnected) BillingUiState.Ready else BillingUiState.Idle
    }

    fun initialize(context: Context, userDao: UserDao? = null) {
        appContext = context.applicationContext
        if (userDao != null) {
            userRepository = UserRepository(userDao)
        }
        if (billingClient != null) {
            if (!isConnected) {
                reconnectAndPreload()
            }
            return
        }
        try {
            val listener = PurchasesUpdatedListener { billingResult, purchases ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        if (!purchases.isNullOrEmpty()) {
                            for (purchase in purchases) {
                                handlePurchase(purchase)
                            }
                        } else {
                            _billingState.value = BillingUiState.Error(
                                "Google Play returned no purchase details. Please tap Restore Purchases if charged.",
                                billingResult.responseCode
                            )
                        }
                    }
                    BillingClient.BillingResponseCode.USER_CANCELED -> {
                        Log.i(TAG, "User cancelled Play Billing sheet.")
                        _billingState.value = BillingUiState.Cancelled
                    }
                    BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                        Log.i(TAG, "Item already owned; restoring purchases from Google Play.")
                        queryAndRestorePurchases { success, restored ->
                            if (success && restored.isNotEmpty()) {
                                _billingState.value = BillingUiState.Success(
                                    productId = restored.first(),
                                    isSubscription = restored.first() in ALL_SUBSCRIPTION_SKUS,
                                    verifiedByServer = false
                                )
                            } else {
                                _billingState.value = BillingUiState.Error(
                                    "You already own this item. Restored active entitlements.",
                                    billingResult.responseCode
                                )
                            }
                        }
                    }
                    else -> {
                        Log.w(TAG, "Purchase failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                        _billingState.value = BillingUiState.Error(
                            explainBillingCode(billingResult.responseCode, billingResult.debugMessage),
                            billingResult.responseCode
                        )
                    }
                }
            }

            val pendingParams = PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()

            billingClient = BillingClient.newBuilder(appContext!!)
                .setListener(listener)
                .enablePendingPurchases(pendingParams)
                .build()

            reconnectAndPreload()
        } catch (e: Throwable) {
            Log.w(TAG, "Play Billing initialization unavailable on this device: ${e.message}")
            isConnected = false
            _billingState.value = BillingUiState.Unavailable(
                reason = "Google Play Billing service is not available on this device (${e.message ?: "no Play Services"})."
            )
        }
    }

    fun reconnectAndPreload() {
        scope.launch {
            val connected = ensureConnected()
            if (connected) {
                preloadAllProductDetails()
                queryAndRestorePurchases()
            }
        }
    }

    private suspend fun ensureConnected(): Boolean {
        val client = billingClient ?: run {
            val ctx = appContext ?: return false
            initialize(ctx)
            billingClient ?: return false
        }
        if (isConnected && client.isReady) return true

        _billingState.value = BillingUiState.Connecting()
        val deferred = CompletableDeferred<Boolean>()
        try {
            withContext(Dispatchers.Main) {
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            isConnected = true
                            lastSetupErrorCode = null
                            if (!deferred.isCompleted) deferred.complete(true)
                        } else {
                            isConnected = false
                            lastSetupErrorCode = billingResult.responseCode
                            Log.w(TAG, "Billing setup error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                            if (!deferred.isCompleted) deferred.complete(false)
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        isConnected = false
                        Log.w(TAG, "Billing service disconnected.")
                        if (!deferred.isCompleted) deferred.complete(false)
                    }
                })
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Billing startConnection threw: ${e.message}")
            isConnected = false
            return false
        }

        val result = withTimeoutOrNull(6000L) { deferred.await() } ?: false
        if (result) {
            _billingState.value = BillingUiState.Ready
        }
        return result
    }

    suspend fun preloadAllProductDetails() {
        val client = billingClient ?: return
        if (!client.isReady) return

        val offersMap = _productOffers.value.toMutableMap()

        // Query subscriptions
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ALL_SUBSCRIPTION_SKUS.map { sku ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val subsResult = client.queryProductDetails(subsParams)
        if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            subsResult.productDetailsList.orEmpty().forEach { details ->
                cachedProductDetails[details.productId] = details
                val offer = details.subscriptionOfferDetails?.firstOrNull()
                val recurringPhase = offer?.pricingPhases?.pricingPhaseList?.lastOrNull()
                if (recurringPhase != null && offer != null) {
                    offersMap[details.productId] = ProductOfferUi(
                        productId = details.productId,
                        isSubscription = true,
                        title = details.title,
                        description = details.description,
                        formattedPrice = recurringPhase.formattedPrice,
                        priceCurrencyCode = recurringPhase.priceCurrencyCode,
                        billingPeriodIso = recurringPhase.billingPeriod,
                        offerToken = offer.offerToken
                    )
                }
            }
        } else {
            Log.w(TAG, "Failed preloading SUBS details: ${subsResult.billingResult.responseCode}")
        }

        // Query in-app products
        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ALL_INAPP_SKUS.map { sku ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        val inAppResult = client.queryProductDetails(inAppParams)
        if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            inAppResult.productDetailsList.orEmpty().forEach { details ->
                cachedProductDetails[details.productId] = details
                val oneTime = details.oneTimePurchaseOfferDetails
                if (oneTime != null) {
                    offersMap[details.productId] = ProductOfferUi(
                        productId = details.productId,
                        isSubscription = false,
                        title = details.title,
                        description = details.description,
                        formattedPrice = oneTime.formattedPrice,
                        priceCurrencyCode = oneTime.priceCurrencyCode
                    )
                }
            }
        } else {
            Log.w(TAG, "Failed preloading INAPP details: ${inAppResult.billingResult.responseCode}")
        }

        _productOffers.value = offersMap
    }

    /**
     * Launch Google Play Billing purchase flow. Automatically reconnects and fetches live ProductDetails
     * if disconnected. Never grants items on missing connection, missing SKU, or error.
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        isSubscription: Boolean = productId in ALL_SUBSCRIPTION_SKUS
    ) {
        if (productId.isBlank()) {
            _billingState.value = BillingUiState.Error("Invalid product SKU selected.")
            return
        }

        _billingState.value = BillingUiState.Launching(productId)

        scope.launch {
            if (!ensureConnected()) {
                _billingState.value = BillingUiState.Unavailable(
                    reason = explainBillingCode(
                        lastSetupErrorCode ?: BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                        "Google Play Billing could not connect."
                    ),
                    responseCode = lastSetupErrorCode,
                    productId = productId,
                    isSubscription = isSubscription
                )
                return@launch
            }

            val client = billingClient ?: run {
                _billingState.value = BillingUiState.Unavailable(
                    reason = "Google Play Billing client is unavailable.",
                    productId = productId,
                    isSubscription = isSubscription
                )
                return@launch
            }

            // Always ensure fresh ProductDetails from Play Store (single retry if absent)
            var productDetails = cachedProductDetails[productId]
            if (productDetails == null) {
                val productType = if (isSubscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(productId)
                                .setProductType(productType)
                                .build()
                        )
                    )
                    .build()
                val queryResult = client.queryProductDetails(params)
                productDetails = queryResult.productDetailsList?.firstOrNull()
                if (productDetails != null) {
                    cachedProductDetails[productId] = productDetails
                    // Refresh live price map
                    preloadAllProductDetails()
                } else {
                    val code = queryResult.billingResult.responseCode
                    Log.w(TAG, "ProductDetails missing for $productId (sub=$isSubscription). Code=$code ${queryResult.billingResult.debugMessage}")
                    _billingState.value = BillingUiState.Unavailable(
                        reason = "Product '$productId' was not returned by Google Play (Code $code). Make sure you are signed into Google Play, installed from a Play testing track, and the SKU is Active in Play Console.",
                        responseCode = code,
                        productId = productId,
                        isSubscription = isSubscription
                    )
                    return@launch
                }
            }

            val productDetailsParamsList = if (isSubscription) {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken.isNullOrBlank()) {
                    _billingState.value = BillingUiState.Unavailable(
                        reason = "No active subscription base plan found in Google Play Console for '$productId'.",
                        productId = productId,
                        isSubscription = true
                    )
                    return@launch
                }
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            } else {
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val launchResult = withContext(Dispatchers.Main) {
                client.launchBillingFlow(activity, flowParams)
            }
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                _billingState.value = BillingUiState.Error(
                    explainBillingCode(launchResult.responseCode, launchResult.debugMessage),
                    launchResult.responseCode
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val token = purchase.purchaseToken
        val orderId = purchase.orderId ?: "GPA.PENDING"
        val primarySku = purchase.products.firstOrNull() ?: return
        val primaryIsSub = primarySku in ALL_SUBSCRIPTION_SKUS

        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.i(TAG, "Purchase PENDING confirmation from Google Play: $primarySku ($orderId)")
            scope.launch {
                userRepository?.recordPendingPurchase(token, orderId, primarySku, primaryIsSub)
                _billingState.value = BillingUiState.PendingPayment(primarySku, orderId)
            }
            return
        }

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.w(TAG, "Ignoring non-purchased state=${purchase.purchaseState} for $primarySku")
            _billingState.value = BillingUiState.Error("Payment was not completed by Google Play.")
            return
        }

        scope.launch {
            val ctx = appContext
            val repo = userRepository
            val alreadyEntitled = (ctx != null && isTokenProcessed(ctx, token)) ||
                (repo != null && repo.isTokenAlreadyEntitled(token))

            if (alreadyEntitled) {
                // Ensure acknowledgment/consumption even if app crashed right after granting entitlement
                completeAcknowledgeOrConsume(purchase)
                Log.i(TAG, "Purchase token already entitled; synced acknowledgment state.")
                return@launch
            }

            _billingState.value = BillingUiState.VerifyingServer(primarySku)

            var verifiedByServer = false
            var vipExpiryOverride: Long? = null

            if (ctx != null) {
                when (
                    val verifyResult = CloudSyncManager.verifyPurchaseWithServer(
                        context = ctx,
                        productId = primarySku,
                        purchaseToken = token,
                        orderId = orderId,
                        isSubscription = primaryIsSub
                    )
                ) {
                    is CloudSyncManager.ServerVerifyResult.Verified -> {
                        verifiedByServer = true
                        vipExpiryOverride = verifyResult.vipExpiryMillis
                    }
                    is CloudSyncManager.ServerVerifyResult.Rejected -> {
                        Log.w(TAG, "Server rejected purchase token for $primarySku: ${verifyResult.reason}")
                        _billingState.value = BillingUiState.Error(verifyResult.reason)
                        return@launch
                    }
                    is CloudSyncManager.ServerVerifyResult.Unavailable -> {
                        // Server unreachable or guest user: Play Billing PURCHASED state is already verified by Play Store.
                        Log.w(TAG, "Server verification unavailable (${verifyResult.reason}); fulfilling Play-confirmed purchase locally.")
                    }
                }
            }

            // Apply entitlements via Room ledger + AntiCheat signature
            for (productId in purchase.products) {
                val isSub = productId in ALL_SUBSCRIPTION_SKUS
                val isNonConsumable = productId == PRODUCT_REMOVE_ADS
                val verificationSource = if (verifiedByServer) "CLOUD_FUNCTION" else "PLAY_BILLING_LOCAL"

                repo?.applyVerifiedPlayPurchase(
                    purchaseToken = token,
                    orderId = orderId,
                    productId = productId,
                    isSubscription = isSub,
                    verificationSource = verificationSource,
                    vipExpiryOverrideMs = vipExpiryOverride
                )

                if (isNonConsumable) _hasRemovedAds.value = true
                if (isSub) _isVipActive.value = true
            }

            // Acknowledge non-consumables/subs OR consume consumables AFTER entitlement recorded
            completeAcknowledgeOrConsume(purchase)
            if (ctx != null) {
                markTokenProcessed(ctx, token)
            }

            withContext(Dispatchers.Main) {
                _purchasedProductIds.value = _purchasedProductIds.value + purchase.products
                onPurchaseCompleted?.invoke(primarySku, primaryIsSub)
            }
            _billingState.value = BillingUiState.Success(primarySku, primaryIsSub, verifiedByServer)
        }
    }

    private suspend fun completeAcknowledgeOrConsume(purchase: Purchase) {
        val client = billingClient ?: return
        val hasSubscriptionOrNonConsumable = purchase.products.any {
            it in ALL_SUBSCRIPTION_SKUS || it == PRODUCT_REMOVE_ADS
        }
        if (hasSubscriptionOrNonConsumable) {
            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val res = client.acknowledgePurchase(ackParams)
                Log.i(TAG, "AcknowledgePurchase result=${res.responseCode} for ${purchase.products}")
            }
        } else {
            val consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            val res = client.consumePurchase(consumeParams)
            Log.i(TAG, "ConsumePurchase result=${res.billingResult.responseCode} for ${purchase.products}")
        }
    }

    /**
     * Queries active INAPP and SUBS purchases from Google Play, acknowledges unacknowledged items,
     * processes any interrupted purchases, and revokes expired/cancelled VIP status if Play reports inactive.
     */
    fun queryAndRestorePurchases(
        onComplete: ((success: Boolean, restoredProducts: List<String>) -> Unit)? = null
    ) {
        scope.launch {
            if (!ensureConnected()) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, emptyList())
                }
                return@launch
            }
            val client = billingClient ?: run {
                withContext(Dispatchers.Main) { onComplete?.invoke(false, emptyList()) }
                return@launch
            }

            val restored = mutableListOf<String>()
            var hasActiveRemoveAds = false
            var hasActiveVipSub = false
            var activeVipProductId: String? = null

            // 1. Query non-consumables and unconsumed in-app products
            val inAppResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in inAppResult.purchasesList) {
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            restored.addAll(purchase.products)
                            if (purchase.products.contains(PRODUCT_REMOVE_ADS)) {
                                hasActiveRemoveAds = true
                            }
                            handlePurchase(purchase)
                        }
                        Purchase.PurchaseState.PENDING -> {
                            val sku = purchase.products.firstOrNull() ?: continue
                            userRepository?.recordPendingPurchase(
                                purchase.purchaseToken,
                                purchase.orderId ?: "GPA.PENDING",
                                sku,
                                isSubscription = false
                            )
                        }
                    }
                }
            }
            _hasRemovedAds.value = hasActiveRemoveAds

            // 2. Query active VIP subscriptions
            val subsResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in subsResult.purchasesList) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        restored.addAll(purchase.products)
                        val matchedSub = purchase.products.firstOrNull { it in ALL_SUBSCRIPTION_SKUS }
                        if (matchedSub != null) {
                            hasActiveVipSub = true
                            activeVipProductId = matchedSub
                        }
                        handlePurchase(purchase)
                    }
                }
            }
            _isVipActive.value = hasActiveVipSub

            // Reconcile persisted entitlement flags in Room
            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK
            ) {
                userRepository?.reconcilePlayEntitlements(
                    hasActiveVipSub = hasActiveVipSub,
                    vipSubProductId = activeVipProductId,
                    hasActiveRemoveAds = hasActiveRemoveAds
                )
            }

            withContext(Dispatchers.Main) {
                _purchasedProductIds.value = _purchasedProductIds.value + restored
                onComplete?.invoke(true, restored.distinct())
            }
        }
    }

    private fun explainBillingCode(code: Int, debugMsg: String = ""): String {
        return when (code) {
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                "Google Play Billing is unavailable on this device. Ensure the Play Store app is updated and a Google account is signed in."
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.NETWORK_ERROR ->
                "Unable to reach Google Play Store. Please check your internet connection and tap Retry."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                "This item is not yet active or available in your Play Store region ($debugMsg)."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                "Play Billing configuration error ($debugMsg). Please verify app signing and Play Console product setup."
            BillingClient.BillingResponseCode.USER_CANCELED ->
                "Purchase was cancelled."
            else ->
                "Google Play checkout could not be completed (Code $code). ${debugMsg.ifBlank { "Please try again." }}"
        }
    }
}
