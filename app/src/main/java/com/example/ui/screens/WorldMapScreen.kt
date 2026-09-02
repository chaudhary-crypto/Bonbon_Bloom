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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.ads.AdManager
import com.example.ads.BannerAdView
import com.example.audio.SoundManager
import com.example.data.LevelProgress
import com.example.game.BoosterType
import com.example.game.LevelCatalog
import com.example.game.LevelObjective
import com.example.ui.components.VipAndShopDialog

@Composable
fun WorldMapScreen(
    viewModel: HomeViewModel,
    onLevelSelected: (Int) -> Unit,
    onBackToHome: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsState()
    val progressList by viewModel.allLevelProgress.collectAsState(emptyList())
    val progressMap = remember(progressList) { progressList.associateBy { it.levelId } }

    val context = LocalContext.current
    val activity = context as? Activity

    var selectedLevelForDialog by remember { mutableStateOf<Int?>(null) }
    var showShopDialog by remember { mutableStateOf(false) }
    var shopInitialTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val listState = rememberLazyListState()

    // Scroll to player's current unlocked level
    LaunchedEffect(profile.highestUnlockedLevel) {
        val targetIndex = (profile.highestUnlockedLevel - 1).coerceIn(0, 99)
        listState.scrollToItem(targetIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF80AB),
                        Color(0xFFFF4081),
                        Color(0xFF7C4DFF),
                        Color(0xFF536DFE)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            WorldMapTopBar(
                lives = profile.lives,
                hasInfiniteLives = profile.hasInfiniteLives,
                coins = profile.coins,
                diamonds = profile.diamonds,
                isVip = profile.isVipActive,
                soundEnabled = profile.soundEnabled,
                musicEnabled = profile.musicEnabled,
                onBack = onBackToHome,
                onToggleSound = { viewModel.toggleSound() },
                onToggleMusic = { viewModel.toggleMusic() },
                onOpenShop = { tab ->
                    shopInitialTab = tab
                    showShopDialog = true
                }
            )

            // 100-Level Adventure Map Scroll View
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                reverseLayout = false
            ) {
                val levels = (1..100).toList()

                itemsIndexed(levels) { index, levelId ->
                    val isWorldDivider = (levelId - 1) % 20 == 0
                    if (isWorldDivider) {
                        WorldBanner(LevelCatalog.getWorldNumber(levelId), LevelCatalog.getWorldName(levelId))
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val isUnlocked = levelId <= profile.highestUnlockedLevel
                    val isCurrent = levelId == profile.highestUnlockedLevel
                    val progress = progressMap[levelId]

                    // Calculate winding offset for Candy Crush winding path
                    val horizontalShift = when (index % 6) {
                        0 -> 0.dp
                        1 -> 60.dp
                        2 -> 90.dp
                        3 -> 50.dp
                        4 -> (-50).dp
                        else -> (-80).dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LevelNode(
                            levelId = levelId,
                            isUnlocked = isUnlocked,
                            isCurrent = isCurrent,
                            stars = progress?.stars ?: 0,
                            modifier = Modifier.offset(x = horizontalShift),
                            onClick = {
                                SoundManager.playButtonClick()
                                if (isUnlocked) {
                                    selectedLevelForDialog = levelId
                                }
                            }
                        )
                    }
                }
            }

            // Google Mobile Ads Banner
            BannerAdView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Level Start Dialog
        selectedLevelForDialog?.let { lvlId ->
            LevelPreviewDialog(
                levelId = lvlId,
                profile = profile,
                progress = progressMap[lvlId],
                activity = activity,
                onDismiss = { selectedLevelForDialog = null },
                onPlay = {
                    selectedLevelForDialog = null
                    onLevelSelected(lvlId)
                },
                onRefillLives = {
                    viewModel.refillLives()
                },
                onRefillLivesFree = {
                    viewModel.refillLivesFree()
                },
                onSkipLevel = {
                    viewModel.skipLevel(lvlId)
                    selectedLevelForDialog = null
                },
                onOpenDiamonds = {
                    shopInitialTab = 1
                    showShopDialog = true
                }
            )
        }

        // Shop / Boosters & VIP Dialog
        if (showShopDialog) {
            VipAndShopDialog(
                profile = profile,
                activity = activity,
                initialTab = shopInitialTab,
                onDismiss = { showShopDialog = false },
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
fun WorldBanner(worldNum: Int, worldName: String) {
    val worldGradients = listOf(
        listOf(Color(0xFFFF80AB), Color(0xFFFF4081)),
        listOf(Color(0xFF8D6E63), Color(0xFF4E342E)),
        listOf(Color(0xFF80D8FF), Color(0xFF00B0FF)),
        listOf(Color(0xFFFFD54F), Color(0xFFFF6D00)),
        listOf(Color(0xFFB388FF), Color(0xFF651FFF))
    )
    val colors = worldGradients.getOrElse((worldNum - 1) % worldGradients.size) { worldGradients[0] }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "WORLD $worldNum",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 2.sp
                )
                Text(
                    text = worldName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun LevelNode(
    levelId: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_node")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = modifier.clickable(enabled = isUnlocked, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(if (isCurrent) pulseScale else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        isCurrent -> Brush.radialGradient(listOf(Color(0xFFFFEA00), Color(0xFFFF6D00)))
                        isUnlocked -> Brush.radialGradient(listOf(Color(0xFF00E676), Color(0xFF00897B)))
                        else -> Brush.radialGradient(listOf(Color(0xFF9E9E9E), Color(0xFF616161)))
                    }
                )
                .border(
                    width = 3.dp,
                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.6f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "$levelId",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            } else {
                Text(text = "🔒", fontSize = 20.sp)
            }
        }

        // Stars below node
        if (isUnlocked) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { starIndex ->
                    val isEarned = starIndex < stars
                    Text(
                        text = if (isEarned) "⭐" else "☆",
                        fontSize = 12.sp,
                        color = if (isEarned) Color(0xFFFFD700) else Color.White.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun WorldMapTopBar(
    lives: Int,
    hasInfiniteLives: Boolean,
    coins: Int,
    diamonds: Int,
    isVip: Boolean,
    soundEnabled: Boolean,
    musicEnabled: Boolean,
    onBack: () -> Unit,
    onToggleSound: () -> Unit,
    onToggleMusic: () -> Unit,
    onOpenShop: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(38.dp)
        ) {
            IconButton(onClick = onBack) {
                Text("◀", fontSize = 16.sp, color = Color.White)
            }
        }

        // Lives, Coins & Diamonds
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Lives
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.clickable { onOpenShop(0) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("❤️", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (hasInfiniteLives) "♾️ VIP" else "$lives/5",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD81B60)
                    )
                }
            }

            // Coins
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.clickable { onOpenShop(3) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$coins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00897B))
                }
            }

            // Diamonds
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF004D40),
                modifier = Modifier.clickable { onOpenShop(1) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💎", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$diamonds",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF18FFFF)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Audio controls
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                shape = CircleShape,
                color = if (soundEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(34.dp)
            ) {
                IconButton(onClick = onToggleSound) {
                    Text(if (soundEnabled) "🔊" else "🔇", fontSize = 14.sp)
                }
            }

            Surface(
                shape = CircleShape,
                color = if (musicEnabled) Color(0xFFFF4081) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(34.dp)
            ) {
                IconButton(onClick = onToggleMusic) {
                    Text(if (musicEnabled) "🎵" else "🔕", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun LevelPreviewDialog(
    levelId: Int,
    profile: com.example.data.UserProfile,
    progress: LevelProgress?,
    activity: Activity?,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onRefillLives: () -> Unit,
    onRefillLivesFree: () -> Unit,
    onSkipLevel: () -> Unit,
    onOpenDiamonds: () -> Unit
) {
    val level = remember(levelId) { LevelCatalog.getLevel(levelId) }
    val playerLives = profile.lives
    val hasInfiniteLives = profile.hasInfiniteLives

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
                Text(
                    text = "LEVEL $levelId",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD81B60)
                )
                Text(
                    text = level.worldName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Target Objective Display
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3E5F5),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("OBJECTIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E24AA))
                        Spacer(modifier = Modifier.height(6.dp))
                        when (val obj = level.objective) {
                            is LevelObjective.TargetScore -> {
                                Text("🎯 Score ${obj.score} Points", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            is LevelObjective.ClearAllJelly -> {
                                Text("🍮 Clear All ${obj.total} Jelly Tiles", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            is LevelObjective.CollectCandy -> {
                                Text("🍬 Collect ${obj.count} ${obj.candyType.name} Candies", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Moves: ${level.maxMoves}", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stars and High Score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Best Stars", fontSize = 12.sp, color = Color.Gray)
                        Row {
                            repeat(3) { starIdx ->
                                val earned = starIdx < (progress?.stars ?: 0)
                                Text(if (earned) "⭐" else "☆", fontSize = 18.sp)
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("High Score", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "${progress?.highScore ?: 0}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (playerLives > 0 || hasInfiniteLives) {
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text(
                            text = if (hasInfiniteLives) "PLAY (♾️ VIP FREE)" else "PLAY (-1 ❤️)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRefillLives,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                        ) {
                            Text("Refill 5 ❤️ (100 🟡)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                                } ?: run {
                                    onRefillLivesFree()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                        ) {
                            Text("📺 Watch Ad: Refill 5 ❤️ (FREE)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // High-Value Diamond Perk: Skip Level
                if (levelId == profile.highestUnlockedLevel && levelId < 100) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            if (profile.diamonds >= 30) {
                                onSkipLevel()
                            } else {
                                onOpenDiamonds()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00B0FF))
                    ) {
                        Text(
                            text = if (profile.diamonds >= 30) "💎 Skip Level with 30 Diamonds" else "💎 Skip Level (Get 30 💎)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShopDialog(
    profile: com.example.data.UserProfile,
    activity: Activity?,
    onDismiss: () -> Unit,
    onBuyBooster: (com.example.game.BoosterType, Int) -> Unit,
    onGrantFreeReward: (com.example.game.BoosterType) -> Unit,
    onAddFreeCoins: (Int) -> Unit,
    onRefillLivesFree: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CANDY SHOP & REWARDS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD81B60))
                Text("Your Coins: ${profile.coins} 🟡 | Lives: ${profile.lives} ❤️", fontSize = 13.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(14.dp))

                // Watch Ad Free Rewards Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF3E5F5),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎁 FREE REWARDS VIA GOOGLE ADS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF6A1B9A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    } ?: run {
                                        onAddFreeCoins(150)
                                    }
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
                                    } ?: run {
                                        onRefillLivesFree()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(21.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63))
                            ) {
                                Text("📺 Full 5 ❤️", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    } ?: run {
                                        onGrantFreeReward(BoosterType.Hammer)
                                    }
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
                                    } ?: run {
                                        onGrantFreeReward(BoosterType.ColorBlast)
                                    }
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

                // Coin Shop Items
                ShopItemRow(
                    icon = "🔨",
                    name = "Lollipop Hammer (x3)",
                    cost = 150,
                    canAfford = profile.coins >= 150,
                    onBuy = { onBuyBooster(com.example.game.BoosterType.Hammer, 150) }
                )
                ShopItemRow(
                    icon = "🔄",
                    name = "Free Shuffle (x3)",
                    cost = 100,
                    canAfford = profile.coins >= 100,
                    onBuy = { onBuyBooster(com.example.game.BoosterType.Shuffle, 100) }
                )
                ShopItemRow(
                    icon = "🌈",
                    name = "Color Blast (x2)",
                    cost = 200,
                    canAfford = profile.coins >= 200,
                    onBuy = { onBuyBooster(com.example.game.BoosterType.ColorBlast, 200) }
                )
                ShopItemRow(
                    icon = "➕",
                    name = "Extra +5 Moves (x3)",
                    cost = 120,
                    canAfford = profile.coins >= 120,
                    onBuy = { onBuyBooster(com.example.game.BoosterType.ExtraMoves, 120) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text("Close", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShopItemRow(
    icon: String,
    name: String,
    cost: Int,
    canAfford: Boolean,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onBuy,
            enabled = canAfford,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF9100),
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text("$cost 🟡", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
