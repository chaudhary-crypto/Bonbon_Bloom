package com.example.payment

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PlayBillingManager handles Google Play Billing Library 7.x integration for Bonbon Bloom:
 * - One-time in-app products (Coins, Gems, Booster packs, Remove Ads)
 * - Auto-renewing VIP Subscriptions
 * - Immediate purchase acknowledgment & consumption
 * - Pending purchases handling
 * - Offline / Startup purchase restoration
 * - Debug test sandbox fallback strictly under BuildConfig.DEBUG
 */
object PlayBillingManager {
    private const val TAG = "PlayBillingManager"

    // Product IDs
    const val PRODUCT_REMOVE_ADS = "remove_ads"
    const val PRODUCT_VIP_MONTHLY = "vip_monthly"
    const val PRODUCT_COINS_500 = "coins_500"
    const val PRODUCT_COINS_1500 = "coins_1500"
    const val PRODUCT_COINS_5000 = "coins_5000"
    const val PRODUCT_GEMS_50 = "gems_50"
    const val PRODUCT_GEMS_200 = "gems_200"
    const val PRODUCT_GEMS_600 = "gems_600"
    const val PRODUCT_STARTER_PACK = "booster_starter_pack"

    private var billingClient: BillingClient? = null
    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _purchasedProductIds = MutableStateFlow<Set<String>>(emptySet())
    val purchasedProductIds: StateFlow<Set<String>> = _purchasedProductIds.asStateFlow()

    private val _hasRemovedAds = MutableStateFlow(false)
    val hasRemovedAds: StateFlow<Boolean> = _hasRemovedAds.asStateFlow()

    private val _isVipActive = MutableStateFlow(false)
    val isVipActive: StateFlow<Boolean> = _isVipActive.asStateFlow()

    // Callback on purchase success: (productId: String, isSubscription: Boolean) -> Unit
    var onPurchaseCompleted: ((productId: String, isSubscription: Boolean) -> Unit)? = null

    fun initialize(context: Context) {
        val listener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.i(TAG, "User cancelled purchase flow.")
            } else {
                Log.w(TAG, "Purchase failed with code: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }

        val pendingParams = PendingPurchasesParams.newBuilder()
            .enableOneTimeProducts()
            .enablePrepaidPlans()
            .build()

        billingClient = BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(pendingParams)
            .build()

        startConnection {
            queryAndRestorePurchases()
        }
    }

    private fun startConnection(onConnected: () -> Unit) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.i(TAG, "Google Play Billing setup successful.")
                    onConnected()
                } else {
                    isConnected = false
                    Log.w(TAG, "Billing setup finished with code: ${billingResult.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "Billing service disconnected. Will retry on next request.")
            }
        })
    }

    /**
     * Launch Google Play Billing Purchase Flow for a specified product ID.
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        isSubscription: Boolean = false,
        onSuccessFallback: (() -> Unit)? = null
    ) {
        val client = billingClient
        if (client == null || !isConnected) {
            Log.w(TAG, "BillingClient not connected.")
            // In DEBUG mode only, allow explicit test simulation fallback
            if (BuildConfig.DEBUG && onSuccessFallback != null) {
                Log.i(TAG, "[DEBUG ONLY] Executing Test Billing Sandbox fallback for $productId")
                onSuccessFallback()
                if (productId == PRODUCT_REMOVE_ADS) {
                    _hasRemovedAds.value = true
                } else if (productId == PRODUCT_VIP_MONTHLY) {
                    _isVipActive.value = true
                }
                return
            }
            return
        }

        scope.launch {
            val productType = if (isSubscription) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productDetailsResult = client.queryProductDetails(params)
            val detailsList = productDetailsResult.productDetailsList

            if (detailsList.isNullOrEmpty()) {
                Log.w(TAG, "Product details not found on Play Console for $productId. Code: ${productDetailsResult.billingResult.responseCode}")
                if (BuildConfig.DEBUG && onSuccessFallback != null) {
                    withContext(Dispatchers.Main) {
                        Log.i(TAG, "[DEBUG ONLY] Product details not live on Play Console yet. Triggering debug test credit.")
                        onSuccessFallback()
                        if (productId == PRODUCT_REMOVE_ADS) _hasRemovedAds.value = true
                        if (productId == PRODUCT_VIP_MONTHLY) _isVipActive.value = true
                    }
                }
                return@launch
            }

            val productDetails = detailsList[0]
            val productDetailsParamsList = if (isSubscription) {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
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

            withContext(Dispatchers.Main) {
                client.launchBillingFlow(activity, flowParams)
            }
        }
    }

    /**
     * Handles processing, acknowledgment, and consumption of a Google Play purchase.
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                Log.i(TAG, "Purchase is pending confirmation from Google Play.")
            }
            return
        }

        scope.launch {
            val products = purchase.products
            for (productId in products) {
                val isSub = productId == PRODUCT_VIP_MONTHLY
                val isNonConsumable = productId == PRODUCT_REMOVE_ADS

                if (isSub || isNonConsumable) {
                    // Acknowledge non-consumable and subscriptions
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        val result = billingClient?.acknowledgePurchase(ackParams)
                        if (result?.responseCode == BillingClient.BillingResponseCode.OK) {
                            Log.i(TAG, "Purchase acknowledged successfully: $productId")
                        }
                    }

                    if (isNonConsumable) {
                        _hasRemovedAds.value = true
                    }
                    if (isSub) {
                        _isVipActive.value = true
                    }
                } else {
                    // Consume consumable items so they can be bought again (coins, gems, boosters)
                    val consumeParams = ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    val consumeResult = billingClient?.consumePurchase(consumeParams)
                    if (consumeResult?.billingResult?.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "Product consumed: $productId")
                    }
                }

                withContext(Dispatchers.Main) {
                    _purchasedProductIds.value = _purchasedProductIds.value + productId
                    onPurchaseCompleted?.invoke(productId, isSub)
                }
            }
        }
    }

    /**
     * Query existing active purchases to restore on startup or upon user tapping "Restore Purchases".
     */
    fun queryAndRestorePurchases(onComplete: ((success: Boolean, restoredProducts: List<String>) -> Unit)? = null) {
        val client = billingClient ?: return
        if (!isConnected) {
            startConnection {
                queryAndRestorePurchases(onComplete)
            }
            return
        }

        scope.launch {
            val restored = mutableListOf<String>()

            // 1. Check active in-app non-consumables (e.g. Remove Ads)
            val inAppResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            if (inAppResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (p in inAppResult.purchasesList) {
                    if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        restored.addAll(p.products)
                        if (p.products.contains(PRODUCT_REMOVE_ADS)) {
                            _hasRemovedAds.value = true
                        }
                    }
                }
            }

            // 2. Check active subscriptions (e.g. VIP Monthly)
            val subsResult = client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            if (subsResult.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (p in subsResult.purchasesList) {
                    if (p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        restored.addAll(p.products)
                        if (p.products.contains(PRODUCT_VIP_MONTHLY)) {
                            _isVipActive.value = true
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                _purchasedProductIds.value = _purchasedProductIds.value + restored
                onComplete?.invoke(true, restored)
            }
        }
    }
}
