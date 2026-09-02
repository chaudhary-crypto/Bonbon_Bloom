package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.ads.AdManager
import com.example.audio.SoundManager
import com.example.game.BoosterType
import com.example.game.Candy
import com.example.game.Cell
import com.example.game.GameState
import com.example.game.GameViewModel
import com.example.game.GridPosition
import com.example.game.LevelObjective
import com.example.game.Particle
import com.example.ui.components.CandyView
import com.example.ui.components.CellBackgroundView
import com.example.ui.components.VipAndShopDialog
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    levelId: Int,
    onNavigateBack: () -> Unit
) {
    LaunchedEffect(levelId) {
        viewModel.startLevel(levelId)
    }

    val board by viewModel.board.collectAsState()
    val score by viewModel.score.collectAsState()
    val movesLeft by viewModel.movesLeft.collectAsState()
    val gameState by viewModel.gameState.collectAsState()
    val level by viewModel.currentLevel.collectAsState()
    val objectiveProgress by viewModel.objectiveProgress.collectAsState()
    val comboBadges by viewModel.comboBadges.collectAsState()
    val floatingScores by viewModel.floatingScores.collectAsState()
    val particles by viewModel.particles.collectAsState()
    val shakeIntensity by viewModel.shakeIntensity.collectAsState()
    val hintCandies by viewModel.hintCandies.collectAsState()
    val activeBooster by viewModel.activeBooster.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var showWinDialogHandled by remember { mutableStateOf(false) }
    var showInGameShopDialog by remember { mutableStateOf(false) }
    var inGameShopTab by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(gameState) {
        if (gameState == GameState.LevelComplete && !showWinDialogHandled) {
            viewModel.onLevelCompleted()
            showWinDialogHandled = true
        }
        if (gameState == GameState.Idle) {
            showWinDialogHandled = false
        }
    }

    val shakeX = remember(shakeIntensity) {
        if (shakeIntensity > 0f) (Random.nextFloat() * 2 - 1) * shakeIntensity else 0f
    }
    val shakeY = remember(shakeIntensity) {
        if (shakeIntensity > 0f) (Random.nextFloat() * 2 - 1) * shakeIntensity else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF880E4F),
                        Color(0xFF4A148C),
                        Color(0xFF1A237E)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .offset(x = shakeX.dp, y = shakeY.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            GameTopHeader(
                levelId = level.id,
                worldName = level.worldName,
                profile = profile,
                soundEnabled = profile.soundEnabled,
                musicEnabled = profile.musicEnabled,
                onBack = onNavigateBack,
                onToggleSound = { viewModel.toggleSound() },
                onToggleMusic = { viewModel.toggleMusic() },
                onOpenShop = { tab ->
                    inGameShopTab = tab
                    showInGameShopDialog = true
                }
            )

            // Objectives & Moves Status Shelf
            GameObjectiveShelf(
                movesLeft = movesLeft,
                score = score,
                objective = level.objective,
                objectiveProgress = objectiveProgress,
                star1 = level.star1Score,
                star2 = level.star2Score,
                star3 = level.star3Score
            )

            // Active Booster Banner Indicator
            if (activeBooster != BoosterType.None) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFD54F),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = when (activeBooster) {
                            BoosterType.Hammer -> "🔨 Tap any candy to smash it!"
                            BoosterType.ColorBlast -> "🌈 Tap any candy to blast all of that color!"
                            BoosterType.MagicSwap -> "🪄 Tap two candies to swap freely!"
                            else -> ""
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // 8x8 Match-3 Game Board with Layered Rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (board.isNotEmpty()) {
                    GameBoardView(
                        board = board,
                        hintCandies = hintCandies,
                        activeBooster = activeBooster,
                        onSwap = { p1, p2 -> viewModel.swap(p1, p2) },
                        onCellClick = { p -> viewModel.onCellClicked(p) }
                    )

                    // Particles Overlay Canvas
                    ParticlesOverlay(particles = particles)

                    // Floating Scores Overlay
                    FloatingScoresOverlay(floatingScores = floatingScores)

                    // Combo Banner Alert ("Sweet!", "Tasty!", "CANDY CUSH COMBO!")
                    ComboBadgesOverlay(comboBadges = comboBadges)
                }
            }

            // Booster Tool Shelf
            BoosterBottomBar(
                profile = profile,
                activeBooster = activeBooster,
                onSelectBooster = { booster -> viewModel.selectBooster(booster) },
                onOpenShop = { tab ->
                    inGameShopTab = tab
                    showInGameShopDialog = true
                }
            )
        }

        // Level Complete Dialog
        if (gameState == GameState.LevelComplete) {
            LevelWinDialog(
                levelId = level.id,
                score = score,
                star1 = level.star1Score,
                star2 = level.star2Score,
                star3 = level.star3Score,
                activity = activity,
                onNextLevel = {
                    val proceed = {
                        if (level.id < 100) {
                            viewModel.startLevel(level.id + 1)
                        } else {
                            onNavigateBack()
                        }
                    }
                    activity?.let { act ->
                        AdManager.showInterstitial(act) { proceed() }
                    } ?: proceed()
                },
                onDoubleCoins = { extraCoins ->
                    viewModel.doubleLevelCoinReward(extraCoins)
                },
                onReplay = {
                    activity?.let { act ->
                        AdManager.showInterstitial(act) { viewModel.retryLevel() }
                    } ?: viewModel.retryLevel()
                },
                onBackToMap = onNavigateBack
            )
        }

        // Out of Moves Dialog
        if (gameState == GameState.GameOver) {
            LevelGameOverDialog(
                levelId = level.id,
                profile = profile,
                activity = activity,
                onBuyExtraMovesWithCoins = { viewModel.buyExtraMovesWithCoins(100) },
                onBuyMovesWithDiamonds = { viewModel.buyMovesWithDiamonds(5, 5) },
                onSkipLevelWithDiamonds = { viewModel.skipLevelWithDiamonds(30) },
                onWatchAdForFreeMoves = { viewModel.grantFreeMovesFromAd(5) },
                onOpenShop = { tab ->
                    inGameShopTab = tab
                    showInGameShopDialog = true
                },
                onRetry = {
                    activity?.let { act ->
                        AdManager.showInterstitial(act) { viewModel.retryLevel() }
                    } ?: viewModel.retryLevel()
                },
                onBackToMap = onNavigateBack
            )
        }

        // In-Game VIP & Diamond Store Dialog
        if (showInGameShopDialog) {
            VipAndShopDialog(
                profile = profile,
                activity = activity,
                initialTab = inGameShopTab,
                onDismiss = { showInGameShopDialog = false },
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
fun GameTopHeader(
    levelId: Int,
    worldName: String,
    profile: com.example.data.UserProfile,
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
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.25f),
            modifier = Modifier.size(36.dp)
        ) {
            IconButton(onClick = onBack) {
                Text("◀", fontSize = 15.sp, color = Color.White)
            }
        }

        // Level Title & Currency Status
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL $levelId",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            // Lives
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.clickable { onOpenShop(0) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("❤️", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (profile.hasInfiniteLives) "♾️" else "${profile.lives}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD81B60)
                    )
                }
            }

            // Coins
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.clickable { onOpenShop(3) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🟡", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${profile.coins}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17)
                    )
                }
            }

            // Diamonds
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF004D40),
                modifier = Modifier.clickable { onOpenShop(1) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💎", fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${profile.diamonds}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF18FFFF)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Surface(
                shape = CircleShape,
                color = if (soundEnabled) Color(0xFF00E676) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(32.dp)
            ) {
                IconButton(onClick = onToggleSound) {
                    Text(if (soundEnabled) "🔊" else "🔇", fontSize = 13.sp)
                }
            }

            Surface(
                shape = CircleShape,
                color = if (musicEnabled) Color(0xFFFF4081) else Color.White.copy(alpha = 0.25f),
                modifier = Modifier.size(32.dp)
            ) {
                IconButton(onClick = onToggleMusic) {
                    Text(if (musicEnabled) "🎵" else "🔕", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun GameObjectiveShelf(
    movesLeft: Int,
    score: Int,
    objective: LevelObjective,
    objectiveProgress: Int,
    star1: Int,
    star2: Int,
    star3: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Moves Left Badge
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (movesLeft <= 5) Brush.radialGradient(listOf(Color(0xFFFF5252), Color(0xFFC62828)))
                        else Brush.radialGradient(listOf(Color(0xFFFFEA00), Color(0xFFFF8F00)))
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$movesLeft",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "MOVES",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Target Objective Status
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (objective) {
                    is LevelObjective.TargetScore -> {
                        Text(
                            text = "Score Target: ${objective.score}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    is LevelObjective.ClearAllJelly -> {
                        Text(
                            text = "🍮 Jelly Left: ${(objective.total - objectiveProgress).coerceAtLeast(0)} / ${objective.total}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFF80AB)
                        )
                    }
                    is LevelObjective.CollectCandy -> {
                        Text(
                            text = "🍬 ${objective.candyType.name}: ${objectiveProgress} / ${objective.count}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFD54F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Score progress bar with Stars milestones
                val progressFraction = (score.toFloat() / star3.toFloat()).coerceIn(0f, 1f)
                Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.CenterStart) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFFFD700),
                        trackColor = Color.White.copy(alpha = 0.25f),
                    )
                    // Star markers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (score >= star1) "⭐" else "☆", fontSize = 12.sp)
                        Text(if (score >= star2) "⭐" else "☆", fontSize = 12.sp)
                        Text(if (score >= star3) "⭐" else "☆", fontSize = 12.sp)
                    }
                }
            }

            // Current Score Display
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "SCORE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$score",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun GameBoardView(
    board: List<List<Cell>>,
    hintCandies: List<GridPosition>,
    activeBooster: BoosterType,
    onSwap: (GridPosition, GridPosition) -> Unit,
    onCellClick: (GridPosition) -> Unit
) {
    val rows = board.size
    val cols = board[0].size

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x22000000))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        val cellSize = maxWidth / cols

        // Layer 1: Background Cells (Jelly, Ice, Chocolate)
        Column {
            for (r in 0 until rows) {
                Row {
                    for (c in 0 until cols) {
                        val cell = board[r][c]
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clickable { onCellClick(GridPosition(r, c)) }
                        ) {
                            CellBackgroundView(
                                obstacle = cell.obstacle,
                                jellyCount = cell.jellyCount
                            )
                        }
                    }
                }
            }
        }

        // Layer 2: Candies Floating Overlay with animation
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val cell = board[r][c]
                val candy = cell.candy
                if (candy != null) {
                    val isHinted = hintCandies.contains(GridPosition(r, c))
                    key(candy.id) {
                        AnimatedCandyTile(
                            candy = candy,
                            row = r,
                            col = c,
                            cellSize = cellSize,
                            cols = cols,
                            rows = rows,
                            isHinted = isHinted,
                            onSwap = onSwap,
                            onClick = { onCellClick(GridPosition(r, c)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCandyTile(
    candy: Candy,
    row: Int,
    col: Int,
    cellSize: Dp,
    cols: Int,
    rows: Int,
    isHinted: Boolean,
    onSwap: (GridPosition, GridPosition) -> Unit,
    onClick: () -> Unit
) {
    val animatedX by animateDpAsState(
        targetValue = cellSize * col,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "x"
    )
    val animatedY by animateDpAsState(
        targetValue = cellSize * row,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "y"
    )

    Box(
        modifier = Modifier
            .offset(x = animatedX, y = animatedY)
            .size(cellSize)
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = {
                        dragX = 0f
                        dragY = 0f
                    },
                    onDragEnd = {
                        if (abs(dragX) > abs(dragY) && abs(dragX) > 35f) {
                            if (dragX > 0 && col < cols - 1) onSwap(GridPosition(row, col), GridPosition(row, col + 1))
                            else if (dragX < 0 && col > 0) onSwap(GridPosition(row, col), GridPosition(row, col - 1))
                        } else if (abs(dragY) > abs(dragX) && abs(dragY) > 35f) {
                            if (dragY > 0 && row < rows - 1) onSwap(GridPosition(row, col), GridPosition(row + 1, col))
                            else if (dragY < 0 && row > 0) onSwap(GridPosition(row, col), GridPosition(row - 1, col))
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragX += dragAmount.x
                    dragY += dragAmount.y
                }
            }
            .clickable(onClick = onClick)
    ) {
        CandyView(candy = candy, isHinted = isHinted)
    }
}

@Composable
fun ParticlesOverlay(particles: List<Particle>) {
    if (particles.isEmpty()) return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}

@Composable
fun FloatingScoresOverlay(floatingScores: List<com.example.game.FloatingScore>) {
    for (item in floatingScores) {
        key(item.id) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = (item.col * 42).dp, y = (item.row * 42 - 18).dp)
            ) {
                Text(
                    text = item.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = item.color
                )
            }
        }
    }
}

@Composable
fun ComboBadgesOverlay(comboBadges: List<com.example.game.ComboBadge>) {
    for (badge in comboBadges) {
        key(badge.id) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = badge.color,
                        shadowElevation = 8.dp
                    ) {
                        Text(
                            text = "  ${badge.text}  ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoosterBottomBar(
    profile: com.example.data.UserProfile,
    activeBooster: BoosterType,
    onSelectBooster: (BoosterType) -> Unit,
    onOpenShop: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoosterItem(
            icon = "🔨",
            name = "Hammer",
            count = profile.hammerCount,
            isSelected = activeBooster == BoosterType.Hammer,
            onClick = {
                if (profile.hammerCount > 0) onSelectBooster(BoosterType.Hammer)
                else onOpenShop(3)
            }
        )
        BoosterItem(
            icon = "🔄",
            name = "Shuffle",
            count = profile.shuffleCount,
            isSelected = activeBooster == BoosterType.Shuffle,
            onClick = {
                if (profile.shuffleCount > 0) onSelectBooster(BoosterType.Shuffle)
                else onOpenShop(3)
            }
        )
        BoosterItem(
            icon = "🌈",
            name = "Color",
            count = profile.colorBlastCount,
            isSelected = activeBooster == BoosterType.ColorBlast,
            onClick = {
                if (profile.colorBlastCount > 0) onSelectBooster(BoosterType.ColorBlast)
                else onOpenShop(3)
            }
        )
        // High-Value Diamond Booster: Magic Wand
        BoosterItem(
            icon = "🪄",
            name = "Wand",
            count = profile.magicWandCount,
            isSelected = activeBooster == BoosterType.MagicWand,
            isHighValue = true,
            onClick = {
                if (profile.magicWandCount > 0) onSelectBooster(BoosterType.MagicWand)
                else onOpenShop(2)
            }
        )
        // High-Value Diamond Booster: Cosmic Blast
        BoosterItem(
            icon = "🌌",
            name = "Cosmic",
            count = profile.cosmicBlastCount,
            isSelected = activeBooster == BoosterType.CosmicBlast,
            isHighValue = true,
            onClick = {
                if (profile.cosmicBlastCount > 0) onSelectBooster(BoosterType.CosmicBlast)
                else onOpenShop(2)
            }
        )
        BoosterItem(
            icon = "➕",
            name = "+5",
            count = profile.extraMovesCount,
            isSelected = false,
            onClick = {
                if (profile.extraMovesCount > 0) onSelectBooster(BoosterType.ExtraMoves)
                else onOpenShop(3)
            }
        )
    }
}

@Composable
fun BoosterItem(
    icon: String,
    name: String,
    count: Int,
    isSelected: Boolean,
    isHighValue: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isSelected -> Color(0xFFFFD54F)
                        isHighValue -> Color(0xFF311B92).copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.25f)
                    }
                )
                .border(
                    width = if (isSelected) 2.5.dp else if (isHighValue) 1.5.dp else 1.dp,
                    color = when {
                        isSelected -> Color.White
                        isHighValue -> Color(0xFF00E5FF)
                        else -> Color.White.copy(alpha = 0.5f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 18.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isHighValue && count == 0) {
                Text("💎", fontSize = 8.sp)
            }
            Text(
                text = "x$count",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighValue) Color(0xFF18FFFF) else Color.White
            )
        }
    }
}

@Composable
fun LevelWinDialog(
    levelId: Int,
    score: Int,
    star1: Int,
    star2: Int,
    star3: Int,
    activity: Activity?,
    onNextLevel: () -> Unit,
    onDoubleCoins: (Int) -> Unit,
    onReplay: () -> Unit,
    onBackToMap: () -> Unit
) {
    val stars = when {
        score >= star3 -> 3
        score >= star2 -> 2
        else -> 1
    }
    val bonusCoins = 50 + stars * 25
    var hasClaimedDouble by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SWEET VICTORY! 🏆",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE91E63)
                )
                Text(
                    text = "Level $levelId Cleared",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stars Display
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val earned = index < stars
                        Text(
                            text = if (earned) "⭐" else "☆",
                            fontSize = if (index == 1) 48.sp else 36.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Score: $score",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = if (hasClaimedDouble) "+${bonusCoins * 2} Candy Coins 🟡 (DOUBLED!)" else "+$bonusCoins Candy Coins 🟡",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57F17)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (!hasClaimedDouble) {
                    Button(
                        onClick = {
                            activity?.let { act ->
                                AdManager.showRewarded(
                                    activity = act,
                                    onRewardEarned = {
                                        SoundManager.playLevelWin()
                                        onDoubleCoins(bonusCoins)
                                        hasClaimedDouble = true
                                    }
                                )
                            } ?: run {
                                onDoubleCoins(bonusCoins)
                                hasClaimedDouble = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9100))
                    ) {
                        Text("📺 2X COINS WITH AD (+${bonusCoins} 🟡)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                ) {
                    Text("NEXT LEVEL ▶", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onReplay,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5))
                    ) {
                        Text("Replay", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onBackToMap,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0BEC5))
                    ) {
                        Text("Map", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LevelGameOverDialog(
    levelId: Int,
    profile: com.example.data.UserProfile,
    activity: Activity?,
    onBuyExtraMovesWithCoins: () -> Unit,
    onBuyMovesWithDiamonds: () -> Unit,
    onSkipLevelWithDiamonds: () -> Unit,
    onWatchAdForFreeMoves: () -> Unit,
    onOpenShop: (Int) -> Unit,
    onRetry: () -> Unit,
    onBackToMap: () -> Unit
) {
    val playerCoins = profile.coins
    val playerDiamonds = profile.diamonds
    val hasInfiniteLives = profile.hasInfiniteLives
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OUT OF MOVES! 💔",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = "Don't give up! Keep your sweet streak!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Watch Ad for Free Moves
                Button(
                    onClick = {
                        activity?.let { act ->
                            AdManager.showRewarded(
                                activity = act,
                                onRewardEarned = {
                                    SoundManager.playLevelWin()
                                    onWatchAdForFreeMoves()
                                }
                            )
                        } ?: run {
                            onWatchAdForFreeMoves()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))
                ) {
                    Text("📺 +5 FREE MOVES: WATCH AD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buy Moves with Coins
                Button(
                    onClick = onBuyExtraMovesWithCoins,
                    enabled = playerCoins >= 100,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9100),
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Text("+5 MOVES (100 🟡)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buy Moves with Diamonds
                Button(
                    onClick = {
                        if (playerDiamonds >= 5) {
                            onBuyMovesWithDiamonds()
                        } else {
                            onOpenShop(1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0091EA))
                ) {
                    Text(
                        text = if (playerDiamonds >= 5) "+5 MOVES (5 💎)" else "+5 MOVES (Get 5 💎)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // High Value Diamond Skip
                if (levelId < 100) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            if (playerDiamonds >= 30) {
                                onSkipLevelWithDiamonds()
                            } else {
                                onOpenShop(1)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(21.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00B0FF))
                    ) {
                        Text(
                            text = if (playerDiamonds >= 30) "💎 Instant Win Level (30 💎)" else "💎 Instant Win (Get 30 💎)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                ) {
                    Text(
                        text = if (hasInfiniteLives) "Retry (♾️ VIP FREE)" else "Retry (-1 ❤️)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onBackToMap,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    shape = RoundedCornerShape(19.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Text("Return to Map", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
