package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.Activity
import android.os.Build
import com.example.MainActivity
import com.example.R
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.audio.SoundManager
import com.example.game.Candy
import com.example.game.CandyType
import com.example.game.SpecialType
import com.example.ui.components.AccountProfileDialog
import com.example.ui.components.CandyView
import com.example.ui.components.LoginDialog
import com.example.ui.components.LuckyWheelDialog
import com.example.ui.components.PiggyBankDialog
import com.example.ui.components.SecurityStatusDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.VipAndShopDialog

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAdventureClicked: () -> Unit,
    onQuickPlayClicked: (Int) -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var showLuckyWheelDialog by remember { mutableStateOf(false) }
    var showPiggyBankDialog by remember { mutableStateOf(false) }
    var showVipShopDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var shopInitialTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(profile.highestUnlockedLevel) {
        if (profile.highestUnlockedLevel > 1 && !profile.hasAskedNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            showNotificationRationaleDialog = true
        }
    }

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
            // Top Bar: Account Login/Profile & Anti-Cheat Shield & Sound/Music
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Account Button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1F1235).copy(alpha = 0.85f),
                    modifier = Modifier.clickable {
                        SoundManager.playButtonClick()
                        if (profile.isLoggedIn) {
                            showAccountDialog = true
                        } else {
                            showLoginDialog = true
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (profile.authProvider) {
                                "GOOGLE" -> "🌐"
                                "EMAIL" -> "📧"
                                "PHONE" -> "📱"
                                else -> "👤"
                            },
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (profile.isLoggedIn) profile.username else "Sign In (Google)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (!profile.isLoggedIn) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFD54F),
                                modifier = Modifier.padding(start = 2.dp)
                            ) {
                                Text(
                                    text = "SYNC",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF3E2723),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Anti-Cheat Shield Pill
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (profile.isCheatingDetected) Color(0xFFD32F2F) else Color(0xFF00E676).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (profile.isCheatingDetected) Color(0xFFFF5252) else Color(0xFF00E676)
                        ),
                        modifier = Modifier.clickable {
                            SoundManager.playButtonClick()
                            showSecurityDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🛡️", fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (profile.isCheatingDetected) "Alert!" else "Anti-Hack",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.isCheatingDetected) Color.White else Color(0xFFE8F5E9)
                            )
                        }
                    }

                    // Sound & Music Toggles
                    Surface(
                        shape = CircleShape,
                        color = if (profile.soundEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleSound() }) {
                            Text(if (profile.soundEnabled) "🔊" else "🔇", fontSize = 13.sp)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = if (profile.musicEnabled) Color(0xFFFF4081) else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(onClick = { viewModel.toggleMusic() }) {
                            Text(if (profile.musicEnabled) "🎵" else "🔕", fontSize = 13.sp)
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        IconButton(onClick = {
                            SoundManager.playButtonClick()
                            showSettingsDialog = true
                        }) {
                            Text("⚙️", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Anti-Cheat Active Warning Banner (if tampered)
            if (profile.isCheatingDetected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFB71C1C),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable {
                            SoundManager.playButtonClick()
                            showSecurityDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Anti-Cheat Guard: Data integrity alert. Tap to inspect shield & verify fair play.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Secondary Header: Lives, Coins, Diamonds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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

                // VIP Pass Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFD54F),
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
                        Text("👑", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (profile.isVipActive) "VIP ACTIVE" else "GET VIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // Candy Wonderland Hero Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(115.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_candy_banner),
                    contentDescription = "Candy Kingdom Wonderland",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay animated candy showcase
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.size(50.dp)) {
                        CandyView(candy = Candy(1, CandyType.Strawberry, SpecialType.RocketRow))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(64.dp)) {
                        CandyView(candy = Candy(2, CandyType.RainbowBomb, SpecialType.Rainbow))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Box(modifier = Modifier.size(50.dp)) {
                        CandyView(candy = Candy(3, CandyType.Lemon, SpecialType.Bomb))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bonbon Bloom Title
            Text(
                text = "BONBON BLOOM",
                fontSize = 40.sp,
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
                    text = "  SWEET PUZZLE  ",
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
                        if (profile.lives > 0 || profile.hasInfiniteLives) {
                            onQuickPlayClicked(profile.highestUnlockedLevel)
                        } else {
                            shopInitialTab = 0
                            showVipShopDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(30.dp),
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

                // Adventure Map Button (1-1000 Levels)
                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        onAdventureClicked()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "🗺️ SAGA WORLD MAP (1–1,000 LEVELS)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Grid of 2 Glassmorphic Feature Cards: Lucky Wheel & Piggy Bank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Lucky Wheel Card
                    Surface(
                        onClick = {
                            SoundManager.playButtonClick()
                            showLuckyWheelDialog = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎡", fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Lucky Wheel", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (profile.freeLuckySpinsCount > 0) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (profile.freeLuckySpinsCount > 0) "SPIN FREE" else "WATCH AD",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (profile.freeLuckySpinsCount > 0) Color(0xFF3E2723) else Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Piggy Bank Card
                    Surface(
                        onClick = {
                            SoundManager.playButtonClick()
                            showPiggyBankDialog = true
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🐷", fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Piggy Bank", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFF80AB),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = "${profile.piggyBankCoins} 🟡",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF26123D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Royal VIP Pass & Diamond Store Card
                Surface(
                    onClick = {
                        SoundManager.playButtonClick()
                        shopInitialTab = 0
                        showVipShopDialog = true
                    },
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFF4081), Color(0xFF00E5FF))
                            ),
                            shape = RoundedCornerShape(22.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF4A148C), Color(0xFF6A1B9A), Color(0xFF311B92))
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                        text = if (profile.isVipActive) "ROYAL VIP ACTIVE 👑" else "ROYAL VIP HUB & STORE",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFD54F)
                                    )
                                    Text(
                                        text = if (profile.isVipActive) "♾️ Unlimited Lives & 2X Coins" else "Infinite Lives, 💎 Diamonds & Boosters",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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
                        .fillMaxWidth(0.9f)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7).copy(alpha = 0.85f))
                ) {
                    Text(
                        text = "📺 FREE COINS: WATCH AD (+150 🟡)",
                        fontSize = 12.sp,
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

        // Lucky Spin Wheel Dialog
        if (showLuckyWheelDialog) {
            LuckyWheelDialog(
                profile = profile,
                activity = activity,
                onDismiss = { showLuckyWheelDialog = false },
                onClaimReward = { prizeIdx, callback ->
                    viewModel.claimLuckyWheelReward(prizeIdx, callback)
                },
                onGrantFreeSpin = {
                    viewModel.grantFreeLuckySpin()
                }
            )
        }

        // Piggy Bank Dialog
        if (showPiggyBankDialog) {
            PiggyBankDialog(
                profile = profile,
                onDismiss = { showPiggyBankDialog = false },
                onSmash = { callback ->
                    viewModel.smashPiggyBank(callback)
                }
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
                onRefillLivesFree = { viewModel.refillLivesFree() },
                onRecordPurchase = { method, email -> viewModel.recordPurchase(method, email) }
            )
        }

        // Multi-Provider Login Dialog (Google, Email, Phone, Social, Guest)
        if (showLoginDialog) {
            LoginDialog(
                currentEmail = profile.email,
                onDismiss = { showLoginDialog = false },
                onLoginWithGoogle = { email, name ->
                    viewModel.loginWithGoogle(email, name)
                },
                onLoginWithEmail = { email, name ->
                    viewModel.loginWithEmail(email, name)
                },
                onLoginWithPhone = { phone ->
                    viewModel.loginWithPhone(phone)
                },
                onLoginWithSocial = { provider, name ->
                    viewModel.loginWithSocial(provider, name)
                },
                onContinueAsGuest = {
                    showLoginDialog = false
                }
            )
        }

        // Player Account & Cloud Sync Profile Dialog
        if (showAccountDialog) {
            AccountProfileDialog(
                profile = profile,
                onDismiss = { showAccountDialog = false },
                onOpenLoginDialog = {
                    showAccountDialog = false
                    showLoginDialog = true
                },
                onOpenSecurityDialog = {
                    showAccountDialog = false
                    showSecurityDialog = true
                },
                onLogout = {
                    viewModel.logout()
                },
                onDeleteAccount = {
                    viewModel.deleteAccount()
                }
            )
        }

        // Anti-Cheat & Fair Play Security Guard Dialog
        if (showSecurityDialog) {
            SecurityStatusDialog(
                profile = profile,
                onDismiss = { showSecurityDialog = false },
                onClearSecurityAlert = {
                    viewModel.clearSecurityAlert()
                }
            )
        }

        // Settings, Audio, Privacy & Compliance Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                profile = profile,
                activity = activity,
                onDismiss = { showSettingsDialog = false },
                onToggleSound = { viewModel.toggleSound() },
                onToggleMusic = { viewModel.toggleMusic() },
                onToggleHaptics = { viewModel.toggleHaptics(it) },
                onLanguageSelected = { viewModel.setLanguage(it) },
                onDeleteMyData = { viewModel.deleteAccount() },
                onSandboxAddCoins = { viewModel.addCoins(5000) },
                onSandboxAddDiamonds = { viewModel.addDiamonds(200) },
                onSandboxRefillLives = { viewModel.refillLivesFree() },
                onSandboxGrantInfiniteLives = { viewModel.grantInfiniteLives(2) },
                onSandboxGrantBoosters = { viewModel.grantAllBoosters(5) },
                onSandboxUnlockAllLevels = { viewModel.unlockLevels(1000) },
                onSandboxRemoveAds = { viewModel.grantSandboxRemoveAds() },
                onSandboxGrantVip = { viewModel.subscribeVip(7) }
            )
        }

        // Educational Notification Permission Request Dialog (Android 13+ deferred after level 1)
        if (showNotificationRationaleDialog) {
            AlertDialog(
                onDismissRequest = {
                    showNotificationRationaleDialog = false
                    viewModel.markNotificationPermissionAsked()
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🌸 Stay in Full Bloom!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF673AB7))
                    }
                },
                text = {
                    Text(
                        "Great job completing your first level! Enable notifications so we can alert you when your 5 hearts are fully refilled and your daily lucky spin is ready.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotificationRationaleDialog = false
                            viewModel.markNotificationPermissionAsked()
                            (activity as? MainActivity)?.requestPermissionLauncher?.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                    ) {
                        Text("Turn On", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotificationRationaleDialog = false
                            viewModel.markNotificationPermissionAsked()
                        }
                    ) {
                        Text("Not Now", color = Color.Gray)
                    }
                }
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
