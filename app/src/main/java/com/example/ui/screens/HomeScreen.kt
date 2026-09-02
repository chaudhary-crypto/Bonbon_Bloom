package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.Activity
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.audio.SoundManager
import com.example.game.Candy
import com.example.game.CandyType
import com.example.game.SpecialType
import com.example.ui.components.CandyView
import com.example.ui.components.VipAndShopDialog

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdventureClicked: () -> Unit,
    onQuickPlayClicked: (Int) -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var showVipShopDialog by remember { mutableStateOf(false) }
    var shopInitialTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(profile.musicEnabled) {
        if (profile.musicEnabled) {
            SoundManager.startMusic()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "home_bounce")
    val logoBounce by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF4081),
                        Color(0xFFE040FB),
                        Color(0xFF7C4DFF),
                        Color(0xFF3D5AFE)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Lives, Coins, Diamonds, Audio Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Currency Pills (Lives, Coins, Diamonds)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Lives Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.clickable {
                            SoundManager.playButtonClick()
                            shopInitialTab = 0
                            showVipShopDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❤️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (profile.hasInfiniteLives) "♾️ VIP" else "${profile.lives}/5",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE91E63)
                            )
                        }
                    }

                    // Coins Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.clickable {
                            SoundManager.playButtonClick()
                            shopInitialTab = 3
                            showVipShopDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🟡", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.coins}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF57F17)
                            )
                        }
                    }

                    // Diamonds Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF004D40),
                        modifier = Modifier.clickable {
                            SoundManager.playButtonClick()
                            shopInitialTab = 1
                            showVipShopDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💎", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile.diamonds}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF18FFFF)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // Sound & Music Toggles
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (profile.soundEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleSound() }) {
                            Text(if (profile.soundEnabled) "🔊" else "🔇", fontSize = 14.sp)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (profile.musicEnabled) Color(0xFFFF4081) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleMusic() }) {
                            Text(if (profile.musicEnabled) "🎵" else "🔕", fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Animated Candy Showcase Cluster
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Box(modifier = Modifier.size(54.dp)) {
                    CandyView(candy = Candy(1, CandyType.Strawberry, SpecialType.RocketRow))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(68.dp)) {
                    CandyView(candy = Candy(2, CandyType.RainbowBomb, SpecialType.Rainbow))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(54.dp)) {
                    CandyView(candy = Candy(3, CandyType.Lemon, SpecialType.Bomb))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Crush Candy Title
            Text(
                text = "CRUSH CANDY",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFD54F),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "  SAGA DELUXE  ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF5D4037)
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Main Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Play Current Level Button
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        onQuickPlayClicked(profile.highestUnlockedLevel)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "PLAY LEVEL ${profile.highestUnlockedLevel} ▶",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                // Adventure Map Button
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        onAdventureClicked()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "🗺️ ADVENTURE MAP (1-100)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Royal VIP Pass & Diamond Store Card
                Surface(
                    onClick = {
                        SoundManager.playButtonClick()
                        shopInitialTab = 0
                        showVipShopDialog = true
                    },
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF00E5FF))
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFF311B92))
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("👑", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (profile.isVipActive) "ROYAL VIP ACTIVE 👑" else "ROYAL VIP PASS & STORE",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFD54F)
                                    )
                                    Text(
                                        text = if (profile.isVipActive) "♾️ Unlimited Lives & 2X Coins" else "Unlimited Lives, 💎 Diamonds & Boosters",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFD54F)
                            ) {
                                Text(
                                    text = if (profile.isVipActive) "PERKS" else "JOIN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF311B92),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Daily Reward Button
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        showDailyRewardDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = "🎁 DAILY REWARD WHEEL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Watch Ad for Free Coins
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        activity?.let { act ->
                            AdManager.showRewarded(
                                activity = act,
                                onRewardEarned = {
                                    viewModel.addCoins(150)
                                    SoundManager.playLevelWin()
                                }
                            )
                        } ?: run {
                            viewModel.addCoins(150)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7).copy(alpha = 0.85f))
                ) {
                    Text(
                        text = "📺 FREE COINS: WATCH AD (+150 🟡)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Google AdMob Banner Ad
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Daily Reward Dialog
        if (showDailyRewardDialog) {
            DailyRewardDialog(
                activity = activity,
                onDismiss = { showDailyRewardDialog = false },
                onClaim = { coins ->
                    viewModel.addCoins(coins)
                    showDailyRewardDialog = false
                }
            )
        }

        // Royal VIP & Diamond Shop Dialog
        if (showVipShopDialog) {
            VipAndShopDialog(
                profile = profile,
                activity = activity,
                initialTab = shopInitialTab,
                onDismiss = { showVipShopDialog = false },
                onSubscribeVip = { days -> viewModel.subscribeVip(days) },
                onClaimVipDailyDiamonds = { viewModel.claimVipDailyDiamonds() },
                onBuyDiamondsPack = { dia, coins -> viewModel.buyDiamondsPack(dia, coins) },
                onBuyCoinsWithDiamonds = { coins, dia -> viewModel.buyCoinsWithDiamonds(coins, dia) },
                onBuyHighValueBooster = { booster, count, dia -> viewModel.buyHighValueBooster(booster, count, dia) },
                onActivateInfiniteLives = { hours, dia -> viewModel.activateInfiniteLives(hours, dia) },
                onBuyBoosterWithCoins = { booster, cost -> viewModel.buyBooster(booster, cost) },
                onGrantFreeReward = { booster -> viewModel.grantBoosterReward(booster, 1) },
                onAddFreeCoins = { amount -> viewModel.addCoins(amount) },
                onRefillLivesFree = { viewModel.refillLivesFree() }
            )
        }
    }
}

@Composable
fun DailyRewardDialog(
    activity: Activity?,
    onDismiss: () -> Unit,
    onClaim: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DAILY REWARDS", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD81B60))
                Text("Log in every day for sweet gifts!", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) { day ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.size(76.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Day ${day + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(if (day == 2) "⭐" else "🟡", fontSize = 20.sp)
                                Text("+${(day + 1) * 75}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF57F17))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Standard Claim
                Button(
                    onClick = {
                        SoundManager.playLevelWin()
                        onClaim(200)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("CLAIM TODAY (+200 🟡)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Double with Video Ad
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        activity?.let { act ->
                            AdManager.showRewarded(
                                activity = act,
                                onRewardEarned = {
                                    SoundManager.playLevelWin()
                                    onClaim(400)
                                }
                            )
                        } ?: run {
                            onClaim(400)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                ) {
                    Text("📺 2X GIFT WITH AD! (+400 🟡)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
