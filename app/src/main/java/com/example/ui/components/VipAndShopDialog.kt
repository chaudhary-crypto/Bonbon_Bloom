package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.R
import com.example.ads.AdManager
import com.example.audio.SoundManager
import com.example.auth.GoogleAuthManager
import com.example.data.UserProfile
import com.example.game.BoosterType
import com.example.payment.BillingUiState
import com.example.payment.PlayBillingManager
import com.example.payment.ProductOfferUi

data class PurchaseConfirmInfo(
    val title: String,
    val description: String,
    val priceText: String,
    val productId: String,
    val isSubscription: Boolean,
    val renewalPeriodLabel: String? = null,
    val onDebugSandboxConfirm: () -> Unit
)

@Composable
fun VipAndShopDialog(
    profile: UserProfile,
    activity: Activity?,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onSubscribeVip: (Int) -> Unit,
    onClaimVipDailyDiamonds: () -> Unit,
    onBuyDiamondsPack: (Int, Int) -> Unit,
    onBuyCoinsWithDiamonds: (Int, Int) -> Unit,
    onBuyHighValueBooster: (BoosterType, Int, Int) -> Unit,
    onActivateInfiniteLives: (Int, Int) -> Unit,
    onBuyBoosterWithCoins: (BoosterType, Int) -> Unit,
    onGrantFreeReward: (BoosterType) -> Unit,
    onAddFreeCoins: (Int) -> Unit,
    onRefillLivesFree: () -> Unit,
    onRecordPurchase: ((String, String?) -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedActivity = remember(activity, context) {
        activity ?: GoogleAuthManager.findActivity(context)
    }

    val productOffers by PlayBillingManager.productOffers.collectAsState()
    val billingState by PlayBillingManager.billingState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var pendingPurchase by remember { mutableStateOf<PurchaseConfirmInfo?>(null) }
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }
    var showPrivacyReader by remember { mutableStateOf(false) }
    var showTermsReader by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (productOffers.isEmpty()) {
            PlayBillingManager.reconnectAndPreload()
        }
    }

    fun priceFor(productId: String, fallback: String): String {
        return productOffers[productId]?.formattedPrice?.takeIf { it.isNotBlank() } ?: fallback
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E102F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👑", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ROYAL STORE & VIP",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                        Text(
                            text = if (productOffers.isNotEmpty()) {
                                "Live Google Play Pricing Active"
                            } else {
                                "Syncing Google Play Store Catalog..."
                            },
                            fontSize = 11.sp,
                            color = if (productOffers.isNotEmpty()) Color(0xFF81C784) else Color(0xFFCE93D8)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                PlayBillingManager.queryAndRestorePurchases { success, items ->
                                    purchaseSuccessMessage = if (success && items.isNotEmpty()) {
                                        "Restored ${items.size} active Play Store purchases: ${items.joinToString(", ")}"
                                    } else if (success) {
                                        "No prior unconsumed or active subscriptions found on this Google Play account."
                                    } else {
                                        "Could not reach Google Play Store right now. Check connection and try again."
                                    }
                                }
                            },
                            modifier = Modifier.testTag("restore_purchases_button")
                        ) {
                            Text("Restore", color = Color(0xFF80D8FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clickable(onClick = onDismiss)
                                    .testTag("shop_close_button")
                            ) {
                                Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // User Balance Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2C194D),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("❤️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (profile.hasInfiniteLives) "♾️ VIP" else "${profile.lives}/5",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.hasInfiniteLives) Color(0xFFFF4081) else Color.White
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF2C194D),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🟡", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.coins}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF004D40),
                        modifier = Modifier.weight(1.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("💎", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.diamonds} DIA",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF18FFFF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF2A1647),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFFD54F),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("👑 VIP Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("💎 Diamonds", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("✨ High Items", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("🟡 Coins", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            item {
                                val weeklyPrice = priceFor(PlayBillingManager.PRODUCT_VIP_WEEKLY, "$2.99")
                                val monthlyPrice = priceFor(PlayBillingManager.PRODUCT_VIP_MONTHLY, "$7.99")
                                val removeAdsPrice = priceFor(PlayBillingManager.PRODUCT_REMOVE_ADS, "$3.99")

                                VipSubscriptionView(
                                    profile = profile,
                                    weeklyPrice = weeklyPrice,
                                    monthlyPrice = monthlyPrice,
                                    removeAdsPrice = removeAdsPrice,
                                    onSubscribe = { skuId, days, displayPrice, periodLabel ->
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = if (skuId == PlayBillingManager.PRODUCT_VIP_MONTHLY) {
                                                "👑 Monthly Royal VIP Gold Pass"
                                            } else {
                                                "👑 Weekly Royal VIP Gold Pass"
                                            },
                                            description = "Unlimited Lives, +25 Daily Diamonds, 2X Level Coins & +50 Bonus Welcome Diamonds ($days days recurring).",
                                            priceText = "$displayPrice / $periodLabel",
                                            productId = skuId,
                                            isSubscription = true,
                                            renewalPeriodLabel = periodLabel,
                                            onDebugSandboxConfirm = {
                                                onSubscribeVip(days)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "VIP Pass activated (Debug Sandbox)."
                                            }
                                        )
                                    },
                                    onBuyRemoveAds = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "🚫 Ad-Free Permanent Pass",
                                            description = "Permanently remove all forced interstitial and banner ads across Bonbon Bloom.",
                                            priceText = removeAdsPrice,
                                            productId = PlayBillingManager.PRODUCT_REMOVE_ADS,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onRecordPurchase?.invoke("DEBUG_SANDBOX", null)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "Remove Ads activated (Debug Sandbox)."
                                            }
                                        )
                                    },
                                    onClaimDailyDiamonds = {
                                        onClaimVipDailyDiamonds()
                                        SoundManager.playLevelWin()
                                        purchaseSuccessMessage = "💎 Claimed +25 Daily VIP Diamonds!"
                                    },
                                    onOpenPrivacy = { showPrivacyReader = true },
                                    onOpenTerms = { showTermsReader = true }
                                )
                            }
                        }
                        1 -> {
                            item {
                                Text(
                                    text = "💎 BUY DIAMOND PACKS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF18FFFF)
                                )
                                Text(
                                    text = "Secured via Google Play Billing & server receipt verification.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            item {
                                val p50 = priceFor(PlayBillingManager.PRODUCT_GEMS_50, "$0.99")
                                DiamondPackCard(
                                    icon = "💎",
                                    title = "Handful of Diamonds",
                                    diamonds = 50,
                                    bonusCoins = 0,
                                    tag = null,
                                    priceText = p50,
                                    testTag = "buy_gems_50",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Handful of Diamonds",
                                            description = "One-time pack: +50 Diamond Coins.",
                                            priceText = p50,
                                            productId = PlayBillingManager.PRODUCT_GEMS_50,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onBuyDiamondsPack(50, 0)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +50 Diamonds added!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                val p150 = priceFor(PlayBillingManager.PRODUCT_GEMS_150, "$2.49")
                                DiamondPackCard(
                                    icon = "💎✨",
                                    title = "Sack of Diamonds",
                                    diamonds = 150,
                                    bonusCoins = 1000,
                                    tag = "🔥 MOST POPULAR",
                                    priceText = p150,
                                    testTag = "buy_gems_150",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Sack of Diamonds",
                                            description = "One-time pack: +150 Diamonds & +1,000 Coins.",
                                            priceText = p150,
                                            productId = PlayBillingManager.PRODUCT_GEMS_150,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onBuyDiamondsPack(150, 1000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +150 Diamonds & +1,000 Coins added!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                val p500 = priceFor(PlayBillingManager.PRODUCT_GEMS_500, "$6.99")
                                DiamondPackCard(
                                    icon = "👑💎",
                                    title = "Royal Chest of Diamonds",
                                    diamonds = 500,
                                    bonusCoins = 5000,
                                    tag = "⭐ BEST VALUE",
                                    priceText = p500,
                                    testTag = "buy_gems_500",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Royal Chest of Diamonds",
                                            description = "One-time pack: +500 Diamonds & +5,000 Coins.",
                                            priceText = p500,
                                            productId = PlayBillingManager.PRODUCT_GEMS_500,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onBuyDiamondsPack(500, 5000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +500 Diamonds & +5,000 Coins added!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                val p1500 = priceFor(PlayBillingManager.PRODUCT_GEMS_1500, "$14.99")
                                DiamondPackCard(
                                    icon = "🏰💎",
                                    title = "Treasury of Diamonds",
                                    diamonds = 1500,
                                    bonusCoins = 20000,
                                    tag = "👑 ROYAL VAULT",
                                    priceText = p1500,
                                    testTag = "buy_gems_1500",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Treasury of Diamonds",
                                            description = "One-time pack: +1,500 Diamonds & +20,000 Coins.",
                                            priceText = p1500,
                                            productId = PlayBillingManager.PRODUCT_GEMS_1500,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onBuyDiamondsPack(1500, 20000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +1,500 Diamonds & +20,000 Coins added!"
                                            }
                                        )
                                    }
                                )
                            }
                        }
                        2 -> {
                            item {
                                Text(
                                    text = "✨ HIGH-VALUE DIAMOND ITEMS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFD54F)
                                )
                                Text(
                                    text = "Redeem earned or purchased Diamond Coins for powerful boosters.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            item {
                                val starterPrice = priceFor(PlayBillingManager.PRODUCT_STARTER_PACK, "$1.99")
                                DiamondPackCard(
                                    icon = "🚀",
                                    title = "Starter Booster Bundle",
                                    diamonds = 20,
                                    bonusCoins = 200,
                                    tag = "Includes +3 Each Booster",
                                    priceText = starterPrice,
                                    testTag = "buy_starter_pack",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Starter Booster Bundle",
                                            description = "+200 Coins, +20 Diamonds, and +3 Hammer, Shuffle, Color Blast & Extra Moves boosters.",
                                            priceText = starterPrice,
                                            productId = PlayBillingManager.PRODUCT_STARTER_PACK,
                                            isSubscription = false,
                                            onDebugSandboxConfirm = {
                                                onBuyDiamondsPack(20, 200)
                                                SoundManager.playBooster()
                                                purchaseSuccessMessage = "🚀 Starter Booster Bundle unlocked!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                HighValueItemCard(
                                    icon = "🪄",
                                    title = "Magic Wand (x3)",
                                    description = "Transforms any chosen 3x3 grid into Color Bombs and wrapped blasts!",
                                    ownedCount = profile.magicWandCount,
                                    diamondCost = 20,
                                    canAfford = profile.diamonds >= 20,
                                    onBuy = {
                                        if (profile.diamonds >= 20) {
                                            onBuyHighValueBooster(BoosterType.MagicWand, 3, 20)
                                            SoundManager.playBooster()
                                            purchaseSuccessMessage = "🪄 +3 Magic Wands unlocked!"
                                        } else {
                                            selectedTab = 1
                                        }
                                    }
                                )
                            }
                            item {
                                HighValueItemCard(
                                    icon = "🌌",
                                    title = "Cosmic Candy Blast (x3)",
                                    description = "Destroys all obstacles on the entire board and triggers crosshair blasts!",
                                    ownedCount = profile.cosmicBlastCount,
                                    diamondCost = 25,
                                    canAfford = profile.diamonds >= 25,
                                    onBuy = {
                                        if (profile.diamonds >= 25) {
                                            onBuyHighValueBooster(BoosterType.CosmicBlast, 3, 25)
                                            SoundManager.playBooster()
                                            purchaseSuccessMessage = "🌌 +3 Cosmic Blasts unlocked!"
                                        } else {
                                            selectedTab = 1
                                        }
                                    }
                                )
                            }
                            item {
                                HighValueItemCard(
                                    icon = "♾️❤️",
                                    title = "Unlimited Lives Pass (2 Hours)",
                                    description = "Play non-stop without losing any hearts for 2 full hours!",
                                    ownedCount = if (profile.hasInfiniteLives) 1 else 0,
                                    diamondCost = 15,
                                    canAfford = profile.diamonds >= 15,
                                    buttonLabel = if (profile.hasInfiniteLives) "Extend (+2h)" else "Activate",
                                    onBuy = {
                                        if (profile.diamonds >= 15) {
                                            onActivateInfiniteLives(2, 15)
                                            SoundManager.playLevelWin()
                                            purchaseSuccessMessage = "♾️ Unlimited Lives activated for 2 hours!"
                                        } else {
                                            selectedTab = 1
                                        }
                                    }
                                )
                            }
                            item {
                                HighValueItemCard(
                                    icon = "👑📦",
                                    title = "Royal Grand Master Bundle",
                                    description = "5 Magic Wands + 5 Cosmic Blasts + 10 Hammers + 2h Infinite Lives",
                                    ownedCount = 0,
                                    diamondCost = 45,
                                    canAfford = profile.diamonds >= 45,
                                    onBuy = {
                                        if (profile.diamonds >= 45) {
                                            onBuyHighValueBooster(BoosterType.MagicWand, 5, 20)
                                            onBuyHighValueBooster(BoosterType.CosmicBlast, 5, 20)
                                            onBuyHighValueBooster(BoosterType.Hammer, 10, 5)
                                            onActivateInfiniteLives(2, 0)
                                            SoundManager.playLevelWin()
                                            purchaseSuccessMessage = "👑 Royal Master Bundle Unlocked!"
                                        } else {
                                            selectedTab = 1
                                        }
                                    }
                                )
                            }
                        }
                        3 -> {
                            item {
                                CoinsAndRewardsSection(
                                    profile = profile,
                                    activity = resolvedActivity,
                                    onBuyCoinsWithDiamonds = { coins, diamonds ->
                                        if (profile.diamonds >= diamonds) {
                                            onBuyCoinsWithDiamonds(coins, diamonds)
                                            SoundManager.playLevelWin()
                                            purchaseSuccessMessage = "🟡 +$coins Candy Coins added!"
                                        } else {
                                            selectedTab = 1
                                        }
                                    },
                                    onBuyBoosterWithCoins = onBuyBoosterWithCoins,
                                    onGrantFreeReward = onGrantFreeReward,
                                    onAddFreeCoins = onAddFreeCoins,
                                    onRefillLivesFree = onRefillLivesFree
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Google Play Billing Checkout Confirmation Dialog with full Regulatory & Payment Disclosures
    pendingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = {
                Text("🛍️ Google Play Checkout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(purchase.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF673AB7))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(purchase.description, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Google Play Total:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(purchase.priceText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (purchase.isSubscription) {
                        Text(
                            text = stringResource(R.string.bonbon_vip_subscription_terms),
                            fontSize = 11.sp,
                            color = Color(0xFF424242),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = stringResource(R.string.bonbon_tax_and_refund_terms),
                        fontSize = 10.5.sp,
                        color = Color(0xFF616161),
                        lineHeight = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(onClick = { showPrivacyReader = true }) {
                            Text("Privacy Policy", fontSize = 11.sp)
                        }
                        TextButton(onClick = { showTermsReader = true }) {
                            Text("Terms of Service", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Google Play Billing Trigger — never grants unpaid items on missing Activity or error
                    Button(
                        onClick = {
                            val targetProductId = purchase.productId
                            val isSub = purchase.isSubscription
                            pendingPurchase = null
                            if (resolvedActivity != null) {
                                PlayBillingManager.launchPurchaseFlow(
                                    activity = resolvedActivity,
                                    productId = targetProductId,
                                    isSubscription = isSub
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Cannot open Google Play Checkout right now. Please reopen the store.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pay_with_google_play_button")
                    ) {
                        Text("Pay with Google Play (${purchase.priceText})", fontWeight = FontWeight.Bold)
                    }

                    if (BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val debugAction = purchase.onDebugSandboxConfirm
                                pendingPurchase = null
                                onRecordPurchase?.invoke("DEBUG_SANDBOX", null)
                                debugAction()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Text("[DEBUG ONLY: Test Billing Sandbox]", fontSize = 11.sp, color = Color(0xFF7E57C2))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Live Google Play Billing State Overlays & Feedback
    when (val state = billingState) {
        is BillingUiState.Connecting,
        is BillingUiState.Launching,
        is BillingUiState.VerifyingServer -> {
            val label = when (state) {
                is BillingUiState.Connecting -> state.message
                is BillingUiState.Launching -> "Opening Google Play checkout for ${state.productId}..."
                is BillingUiState.VerifyingServer -> "Verifying payment securely with Google Play & server..."
                else -> "Processing..."
            }
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Google Play Billing", fontWeight = FontWeight.Bold) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(label, fontSize = 13.sp)
                    }
                },
                confirmButton = {}
            )
        }
        is BillingUiState.PendingPayment -> {
            AlertDialog(
                onDismissRequest = { PlayBillingManager.dismissBillingState() },
                title = { Text("⏳ Payment Pending", fontWeight = FontWeight.Bold, color = Color(0xFFE65100)) },
                text = {
                    Text(
                        "Your Google Play payment (${state.orderId}) for ${state.productId} is awaiting confirmation (for example, UPI or cash voucher).\n\nYour VIP perks or items will unlock automatically as soon as Google Play completes payment.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(onClick = { PlayBillingManager.dismissBillingState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is BillingUiState.Unavailable -> {
            AlertDialog(
                onDismissRequest = { PlayBillingManager.dismissBillingState() },
                title = { Text("Google Play Unavailable", fontWeight = FontWeight.Bold, color = Color(0xFFD84315)) },
                text = {
                    Text(
                        "${state.reason}\n\nCheck that you are signed into Google Play Store with an active payment method and network connection.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val pid = state.productId
                            val isSub = state.isSubscription
                            PlayBillingManager.dismissBillingState()
                            if (pid != null && resolvedActivity != null) {
                                PlayBillingManager.launchPurchaseFlow(resolvedActivity, pid, isSub)
                            } else {
                                PlayBillingManager.reconnectAndPreload()
                            }
                        }
                    ) {
                        Text("Retry Google Play")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { PlayBillingManager.dismissBillingState() }) {
                        Text("Close")
                    }
                }
            )
        }
        is BillingUiState.Error -> {
            AlertDialog(
                onDismissRequest = { PlayBillingManager.dismissBillingState() },
                title = { Text("Checkout Notice", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
                text = { Text(state.message, fontSize = 13.sp) },
                confirmButton = {
                    Button(onClick = { PlayBillingManager.dismissBillingState() }) {
                        Text("OK")
                    }
                }
            )
        }
        is BillingUiState.Success -> {
            AlertDialog(
                onDismissRequest = { PlayBillingManager.dismissBillingState() },
                title = { Text("🎉 Purchase Verified!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
                text = {
                    val verNote = if (state.verifiedByServer) {
                        "Verified by Google Play & Bonbon Bloom Server."
                    } else {
                        "Verified by Google Play Billing."
                    }
                    Text("${state.productId} unlocked and saved to your account.\n\n$verNote", fontSize = 14.sp)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            SoundManager.playLevelWin()
                            PlayBillingManager.dismissBillingState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                    ) {
                        Text("Awesome!")
                    }
                }
            )
        }
        BillingUiState.Cancelled -> {
            AlertDialog(
                onDismissRequest = { PlayBillingManager.dismissBillingState() },
                title = { Text("Purchase Cancelled", fontWeight = FontWeight.Bold) },
                text = { Text("No charge was made to your Google Play account.", fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = { PlayBillingManager.dismissBillingState() }) {
                        Text("OK")
                    }
                }
            )
        }
        BillingUiState.Idle, BillingUiState.Ready -> Unit
    }

    purchaseSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { purchaseSuccessMessage = null },
            title = { Text("🎉 Success!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
            text = { Text(msg, fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = { purchaseSuccessMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Text("Awesome!")
                }
            }
        )
    }

    if (showPrivacyReader) {
        InAppPolicyReaderDialog(context = context, onDismiss = { showPrivacyReader = false })
    }
    if (showTermsReader) {
        InAppTermsReaderDialog(context = context, onDismiss = { showTermsReader = false })
    }
}

@Composable
fun VipSubscriptionView(
    profile: UserProfile,
    weeklyPrice: String,
    monthlyPrice: String,
    removeAdsPrice: String,
    onSubscribe: (skuId: String, days: Int, priceText: String, periodLabel: String) -> Unit,
    onBuyRemoveAds: () -> Unit,
    onClaimDailyDiamonds: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit
) {
    val isVip = profile.isVipActive
    val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
    val canClaimDaily = isVip && profile.lastVipDailyClaim < today

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF7C4DFF))),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF311B92))))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isVip) "ROYAL VIP PASS ACTIVE" else "ROYAL CANDY VIP PASS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = if (isVip) "VIP Member: Unlimited Lives Active" else "Weekly & Monthly Auto-Renewing Pass",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VipPerkItem("♾️", "UNLIMITED LIVES", "Never wait for hearts or lose lives on retries")
                    VipPerkItem("💎", "+25 DIAMONDS DAILY", "Collect 175 Diamonds every single week")
                    VipPerkItem("🌟", "2X LEVEL COINS", "Double coin rewards on every level completed")
                    VipPerkItem("🪄", "ROYAL WELCOME BUNDLE", "+50 💎 + 2 Magic Wands + 2 Cosmic Blasts")
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isVip) {
                    if (canClaimDaily) {
                        Button(
                            onClick = onClaimDailyDiamonds,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                        ) {
                            Text("🎁 CLAIM TODAY'S 25 💎", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "✓ Today's 25 💎 Claimed (Next tomorrow)",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { onSubscribe(PlayBillingManager.PRODUCT_VIP_WEEKLY, 7, weeklyPrice, "week") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("subscribe_vip_weekly_button"),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                    ) {
                        Text("⭐ WEEKLY VIP: $weeklyPrice / WEEK", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onSubscribe(PlayBillingManager.PRODUCT_VIP_MONTHLY, 30, monthlyPrice, "month") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("subscribe_vip_monthly_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F))
                    ) {
                        Text("👑 MONTHLY VIP: $monthlyPrice / MONTH", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!profile.hasRemovedAds) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onBuyRemoveAds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("buy_remove_ads_button"),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF80D8FF))
                    ) {
                        Text("🚫 REMOVE ALL ADS FOREVER — $removeAdsPrice", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.bonbon_vip_subscription_terms),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.bonbon_tax_and_refund_terms),
                    fontSize = 9.5.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onOpenPrivacy) {
                        Text("Privacy Policy", fontSize = 10.sp, color = Color(0xFF80D8FF))
                    }
                    TextButton(onClick = onOpenTerms) {
                        Text("Terms & Refunds", fontSize = 10.sp, color = Color(0xFF80D8FF))
                    }
                }
            }
        }
    }
}

@Composable
fun VipPerkItem(icon: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
            Text(description, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun DiamondPackCard(
    icon: String,
    title: String,
    diamonds: Int,
    bonusCoins: Int,
    tag: String?,
    priceText: String,
    testTag: String = "",
    onBuy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF26123D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A148C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (tag != null) {
                        Text(tag, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFF4081))
                    }
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+$diamonds 💎", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF18FFFF))
                        if (bonusCoins > 0) {
                            Text(" +$bonusCoins 🟡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        }
                    }
                }
            }

            Button(
                onClick = onBuy,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                modifier = Modifier
                    .height(48.dp)
                    .let { if (testTag.isNotEmpty()) it.testTag(testTag) else it }
            ) {
                Text(priceText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
        }
    }
}

@Composable
fun HighValueItemCard(
    icon: String,
    title: String,
    description: String,
    ownedCount: Int,
    diamondCost: Int,
    canAfford: Boolean,
    buttonLabel: String? = null,
    onBuy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF26123D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5E35B1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(icon, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(description, fontSize = 10.sp, color = Color(0xFFB0BEC5), maxLines = 2)
                    if (ownedCount > 0) {
                        Text("Owned: $ownedCount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF81C784))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onBuy,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) Color(0xFF7C4DFF) else Color(0xFF4527A0)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = buttonLabel ?: "$diamondCost 💎",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAfford) Color.White else Color(0xFFFF80AB)
                )
            }
        }
    }
}

@Composable
fun CoinsAndRewardsSection(
    profile: UserProfile,
    activity: Activity?,
    onBuyCoinsWithDiamonds: (Int, Int) -> Unit,
    onBuyBoosterWithCoins: (BoosterType, Int) -> Unit,
    onGrantFreeReward: (BoosterType) -> Unit,
    onAddFreeCoins: (Int) -> Unit,
    onRefillLivesFree: () -> Unit
) {
    val context = LocalContext.current
    val resolvedActivity = activity ?: GoogleAuthManager.findActivity(context)
    val adUnavailableMsg = stringResource(R.string.bonbon_ad_unavailable)

    fun showRewardedOrNotify(onEarned: () -> Unit) {
        if (resolvedActivity == null) {
            Toast.makeText(context, adUnavailableMsg, Toast.LENGTH_SHORT).show()
            return
        }
        AdManager.showRewarded(
            activity = resolvedActivity,
            onRewardEarned = {
                SoundManager.playLevelWin()
                onEarned()
            },
            onUnavailable = {
                Toast.makeText(context, adUnavailableMsg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("🟡 EXCHANGE DIAMONDS FOR COINS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))

        CoinExchangeRow("1,500 🟡", "10 💎", profile.diamonds >= 10) {
            onBuyCoinsWithDiamonds(1500, 10)
        }
        CoinExchangeRow("5,000 🟡", "30 💎", profile.diamonds >= 30) {
            onBuyCoinsWithDiamonds(5000, 30)
        }
        CoinExchangeRow("20,000 🟡", "100 💎", profile.diamonds >= 100) {
            onBuyCoinsWithDiamonds(20000, 100)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text("🎁 WATCH AD FOR BONUS REWARDS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE040FB))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showRewardedOrNotify { onAddFreeCoins(150) } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Text("📺 +150 🟡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = { showRewardedOrNotify { onRefillLivesFree() } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("📺 Full 5 ❤️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { showRewardedOrNotify { onGrantFreeReward(BoosterType.Hammer) } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
            ) {
                Text("📺 +1 🔨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = { showRewardedOrNotify { onGrantFreeReward(BoosterType.ColorBlast) } },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))
            ) {
                Text("📺 +1 🌈", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun CoinExchangeRow(coinsText: String, diamondCostText: String, canAfford: Boolean, onExchange: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF26123D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF371E56)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(coinsText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
            Button(
                onClick = onExchange,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canAfford) Color(0xFF00B0FF) else Color(0xFF37474F)
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Text(diamondCostText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
