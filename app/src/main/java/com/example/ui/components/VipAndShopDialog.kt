package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.AdManager
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.game.BoosterType

data class PurchaseConfirmInfo(
    val title: String,
    val description: String,
    val priceText: String,
    val productId: String = "",
    val isSubscription: Boolean = false,
    val onConfirm: () -> Unit
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
    var selectedTab by remember { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var pendingPurchase by remember { mutableStateOf<PurchaseConfirmInfo?>(null) }
    var purchaseSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isRestoringPurchases by remember { mutableStateOf(false) }

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
                    Column {
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
                            text = "Get Diamonds, VIP Subscription & High-Value Items",
                            fontSize = 11.sp,
                            color = Color(0xFFCE93D8)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                isRestoringPurchases = true
                                com.example.payment.PlayBillingManager.queryAndRestorePurchases { success, items ->
                                    isRestoringPurchases = false
                                    purchaseSuccessMessage = if (items.isNotEmpty()) {
                                        "✓ Restored ${items.size} active purchases: ${items.joinToString(", ")}"
                                    } else {
                                        "No active purchases found to restore."
                                    }
                                }
                            }
                        ) {
                            Text("Restore", color = Color(0xFF80D8FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.clickable(onClick = onDismiss)
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
                    // Lives Pill
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

                    // Coins Pill
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

                    // Diamonds Pill
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

                // Tab Content
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
                                VipSubscriptionView(
                                    profile = profile,
                                    onSubscribe = { days, priceText ->
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "👑 Royal VIP Pass Subscription",
                                            description = "Get Unlimited Lives, +25 Daily Diamonds, 2X Coin Multiplier & High-Value item bundles for $days days!",
                                            priceText = priceText,
                                            onConfirm = {
                                                onSubscribeVip(days)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "🎉 VIP Pass activated successfully! Enjoy Unlimited Lives & +50 Bonus Diamonds!"
                                            }
                                        )
                                    },
                                    onClaimDailyDiamonds = {
                                        onClaimVipDailyDiamonds()
                                        SoundManager.playLevelWin()
                                        purchaseSuccessMessage = "💎 Claimed +25 Daily VIP Diamonds!"
                                    }
                                )
                            }
                        }
                        1 -> {
                            item {
                                Text(
                                    text = "💎 BUY DIAMOND COIN PACKS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF18FFFF)
                                )
                                Text(
                                    text = "Diamond Coins unlock exclusive high-value boosters & perks!",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            item {
                                DiamondPackCard(
                                    icon = "💎",
                                    title = "Handful of Diamonds",
                                    diamonds = 50,
                                    bonusCoins = 0,
                                    tag = null,
                                    priceText = "$0.99",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Handful of Diamonds",
                                            description = "Instantly receive 50 Diamond Coins.",
                                            priceText = "$0.99",
                                            onConfirm = {
                                                onBuyDiamondsPack(50, 0)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +50 Diamonds added to your vault!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                DiamondPackCard(
                                    icon = "💎✨",
                                    title = "Sack of Diamonds",
                                    diamonds = 150,
                                    bonusCoins = 1000,
                                    tag = "🔥 MOST POPULAR",
                                    priceText = "$2.49",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Sack of Diamonds",
                                            description = "Receive 150 Diamond Coins + Bonus 1,000 Candy Coins!",
                                            priceText = "$2.49",
                                            onConfirm = {
                                                onBuyDiamondsPack(150, 1000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +150 Diamonds and +1,000 Coins added!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                DiamondPackCard(
                                    icon = "👑💎",
                                    title = "Royal Chest of Diamonds",
                                    diamonds = 500,
                                    bonusCoins = 5000,
                                    tag = "⭐ BEST VALUE",
                                    priceText = "$6.99",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Royal Chest of Diamonds",
                                            description = "Receive 500 Diamond Coins + Bonus 5,000 Candy Coins!",
                                            priceText = "$6.99",
                                            onConfirm = {
                                                onBuyDiamondsPack(500, 5000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +500 Diamonds and +5,000 Coins added!"
                                            }
                                        )
                                    }
                                )
                            }
                            item {
                                DiamondPackCard(
                                    icon = "🏰💎",
                                    title = "Treasury of Diamonds",
                                    diamonds = 1500,
                                    bonusCoins = 20000,
                                    tag = "👑 ROYAL VAULT",
                                    priceText = "$14.99",
                                    onBuy = {
                                        pendingPurchase = PurchaseConfirmInfo(
                                            title = "Treasury of Diamonds",
                                            description = "Receive 1,500 Diamond Coins + Bonus 20,000 Candy Coins!",
                                            priceText = "$14.99",
                                            onConfirm = {
                                                onBuyDiamondsPack(1500, 20000)
                                                SoundManager.playLevelWin()
                                                purchaseSuccessMessage = "💎 +1,500 Diamonds and +20,000 Coins added!"
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
                                    text = "Exclusive legendary items unlocked only with Diamond Coins!",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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
                                    description = "5 Magic Wands + 5 Cosmic Blasts + 10 Hammers + 2h Infinite Lives (Save 40%)",
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
                                            purchaseSuccessMessage = "👑 Royal Master Bundle Unlocked! Enjoy your legendary powerups!"
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
                                    activity = activity,
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

    // Google Play Billing Checkout Dialog
    pendingPurchase?.let { purchase ->
        AlertDialog(
            onDismissRequest = { pendingPurchase = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛍️ Google Play Checkout", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
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
                            Text("Total:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(purchase.priceText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF2E7D32))
                        }
                    }

                    if (purchase.isSubscription) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Auto-renewing subscription. Charged to your Google Play account upon confirmation. Cancel anytime in Google Play Store > Subscriptions.",
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            lineHeight = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Official Google Play 1-Tap button
                    Button(
                        onClick = {
                            val action = purchase.onConfirm
                            val targetProductId = purchase.productId.ifEmpty { "coins_500" }
                            pendingPurchase = null
                            if (activity != null) {
                                com.example.payment.PlayBillingManager.launchPurchaseFlow(
                                    activity = activity,
                                    productId = targetProductId,
                                    isSubscription = purchase.isSubscription,
                                    onSuccessFallback = {
                                        onRecordPurchase?.invoke("GOOGLE_PLAY", null)
                                        action()
                                    }
                                )
                            } else {
                                onRecordPurchase?.invoke("GOOGLE_PLAY", null)
                                action()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Pay with Google Play (${purchase.priceText})", fontWeight = FontWeight.Bold)
                    }

                    // Debug-only developer sandbox test button
                    if (com.example.BuildConfig.DEBUG) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val action = purchase.onConfirm
                                pendingPurchase = null
                                onRecordPurchase?.invoke("DEBUG_SANDBOX", null)
                                action()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
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

    // Purchase Success Dialog
    purchaseSuccessMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { purchaseSuccessMessage = null },
            title = { Text("🎉 Success!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) },
            text = { Text(msg, fontSize = 15.sp) },
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
}

@Composable
fun VipSubscriptionView(
    profile: UserProfile,
    onSubscribe: (Int, String) -> Unit,
    onClaimDailyDiamonds: () -> Unit
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF4A148C), Color(0xFF311B92))
                    )
                )
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
                            text = if (isVip) "VIP Member: Unlimited Lives Active" else "Weekly & Monthly Subscription Pass",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // VIP Perks List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VipPerkItem("♾️", "UNLIMITED LIVES", "Never wait for hearts or lose lives on retries")
                    VipPerkItem("💎", "+25 DIAMONDS DAILY", "Collect 175 Diamonds every single week")
                    VipPerkItem("🌟", "2X LEVEL COINS & STARS", "Double coin rewards on every level completed")
                    VipPerkItem("🪄", "ROYAL WELCOME BUNDLE", "+50 💎 + 2 Magic Wands + 2 Cosmic Blasts")
                    VipPerkItem("🚫", "VIP INSTANT REWARDS", "No wait times on all free reward claims")
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isVip) {
                    // Daily Diamond Claim Button for VIP
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
                                text = "✓ Today's 25 💎 Claimed (Next in tomorrow)",
                                color = Color(0xFF81C784),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onSubscribe(7, "$2.99 / week") },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F))
                    ) {
                        Text("Extend VIP Pass (+7 Days - $2.99)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onSubscribe(7, "$2.99 / week") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                    ) {
                        Text("⭐ SUBSCRIBE: $2.99 / WEEK", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onSubscribe(30, "$7.99 / month") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(23.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFD54F))
                    ) {
                        Text("👑 1-MONTH ROYAL VIP - $7.99 (SAVE 35%)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Auto-renewing subscription. Payment charged to Google Play account at confirmation. Subscription automatically renews unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play Store > Subscriptions.",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
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
                modifier = Modifier.height(40.dp)
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
                modifier = Modifier.height(40.dp)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Exchange Diamonds for Coins
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

        // Free Ad Rewards
        Text("🎁 WATCH AD FOR FREE REWARDS", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE040FB))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    activity?.let { act ->
                        AdManager.showRewarded(
                            activity = act,
                            onRewardEarned = {
                                SoundManager.playLevelWin()
                                onAddFreeCoins(150)
                            }
                        )
                    } ?: onAddFreeCoins(150)
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
            ) {
                Text("📺 +150 🟡", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    activity?.let { act ->
                        AdManager.showRewarded(
                            activity = act,
                            onRewardEarned = {
                                SoundManager.playLevelWin()
                                onRefillLivesFree()
                            }
                        )
                    } ?: onRefillLivesFree()
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
            ) {
                Text("📺 Full 5 ❤️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    activity?.let { act ->
                        AdManager.showRewarded(
                            activity = act,
                            onRewardEarned = {
                                SoundManager.playLevelWin()
                                onGrantFreeReward(BoosterType.Hammer)
                            }
                        )
                    } ?: onGrantFreeReward(BoosterType.Hammer)
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(21.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E57C2))
            ) {
                Text("📺 +1 🔨", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Button(
                onClick = {
                    activity?.let { act ->
                        AdManager.showRewarded(
                            activity = act,
                            onRewardEarned = {
                                SoundManager.playLevelWin()
                                onGrantFreeReward(BoosterType.ColorBlast)
                            }
                        )
                    } ?: onGrantFreeReward(BoosterType.ColorBlast)
                },
                modifier = Modifier.weight(1f).height(42.dp),
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
                modifier = Modifier.height(36.dp)
            ) {
                Text(diamondCostText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
