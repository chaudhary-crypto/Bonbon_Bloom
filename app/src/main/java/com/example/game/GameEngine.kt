package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

class GameEngine {
    private val _board = MutableStateFlow<List<List<Cell>>>(emptyList())
    val board: StateFlow<List<List<Cell>>> = _board.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _movesLeft = MutableStateFlow(20)
    val movesLeft: StateFlow<Int> = _movesLeft.asStateFlow()

    private val _gameState = MutableStateFlow(GameState.Idle)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentLevel = MutableStateFlow(LevelCatalog.getLevel(1))
    val currentLevel: StateFlow<LevelConfig> = _currentLevel.asStateFlow()

    private val _objectiveProgress = MutableStateFlow(0)
    val objectiveProgress: StateFlow<Int> = _objectiveProgress.asStateFlow()

    private val _comboBadges = MutableStateFlow<List<ComboBadge>>(emptyList())
    val comboBadges: StateFlow<List<ComboBadge>> = _comboBadges.asStateFlow()

    private val _floatingScores = MutableStateFlow<List<FloatingScore>>(emptyList())
    val floatingScores: StateFlow<List<FloatingScore>> = _floatingScores.asStateFlow()

    private val _particles = MutableStateFlow<List<Particle>>(emptyList())
    val particles: StateFlow<List<Particle>> = _particles.asStateFlow()

    private val _shakeIntensity = MutableStateFlow(0f)
    val shakeIntensity: StateFlow<Float> = _shakeIntensity.asStateFlow()

    private val _hintCandies = MutableStateFlow<List<GridPosition>>(emptyList())
    val hintCandies: StateFlow<List<GridPosition>> = _hintCandies.asStateFlow()

    private val _activeBooster = MutableStateFlow(BoosterType.None)
    val activeBooster: StateFlow<BoosterType> = _activeBooster.asStateFlow()

    private var candyIdCounter = 0
    private val engineScope = CoroutineScope(Dispatchers.Default)
    private var width = 8
    private var height = 8
    private var comboCount = 0
    private var hintJob: Job? = null

    fun loadLevel(levelId: Int) {
        val config = LevelCatalog.getLevel(levelId)
        _currentLevel.value = config
        width = 8
        height = 8
        _movesLeft.value = config.maxMoves
        _score.value = 0
        _objectiveProgress.value = 0
        _gameState.value = GameState.Idle
        _comboBadges.value = emptyList()
        _floatingScores.value = emptyList()
        _particles.value = emptyList()
        _activeBooster.value = BoosterType.None

        var initialBoard = createBoardForLevel(config)
        // Ensure no initial matches
        while (findMatchGroups(initialBoard).isNotEmpty()) {
            initialBoard = createBoardForLevel(config)
        }
        _board.value = initialBoard
        resetHintTimer()
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
    }

    private fun createBoardForLevel(config: LevelConfig): List<List<Cell>> {
        val jellySet = config.initialJellyPositions.toSet()
        val iceSet = config.initialIcePositions.toSet()
        val chocSet = config.initialChocolatePositions.toSet()

        return List(height) { r ->
            List(width) { c ->
                val pos = GridPosition(r, c)
                val isChoc = chocSet.contains(pos)
                val isIce = iceSet.contains(pos)
                val hasJelly = jellySet.contains(pos)

                Cell(
                    position = pos,
                    candy = if (isChoc) null else generateRandomCandy(),
                    obstacle = when {
                        isChoc -> ObstacleType.Chocolate
                        isIce -> ObstacleType.Ice
                        else -> ObstacleType.None
                    },
                    jellyCount = if (hasJelly) 1 else 0
                )
            }
        }
    }

    private fun generateRandomCandy(allowRainbow: Boolean = false): Candy {
        val standardTypes = listOf(
            CandyType.Strawberry,
            CandyType.Lemon,
            CandyType.Blueberry,
            CandyType.Grape,
            CandyType.Orange,
            CandyType.Mint
        )
        val selectedType = if (allowRainbow && Random.nextInt(100) < 2) {
            CandyType.RainbowBomb
        } else {
            standardTypes[Random.nextInt(standardTypes.size)]
        }
        return Candy(
            id = ++candyIdCounter,
            type = selectedType,
            specialType = if (selectedType == CandyType.RainbowBomb) SpecialType.Rainbow else SpecialType.None
        )
    }

    fun setActiveBooster(booster: BoosterType) {
        if (_activeBooster.value == booster) {
            _activeBooster.value = BoosterType.None
        } else {
            _activeBooster.value = booster
        }
    }

    fun onCellClicked(pos: GridPosition) {
        val booster = _activeBooster.value
        if (booster == BoosterType.None || _gameState.value != GameState.Idle) return

        when (booster) {
            BoosterType.Hammer -> {
                applyHammer(pos)
            }
            BoosterType.ColorBlast -> {
                val candy = _board.value[pos.row][pos.col].candy
                if (candy != null && candy.type != CandyType.RainbowBomb) {
                    applyColorBlast(candy.type)
                }
            }
            BoosterType.MagicWand -> {
                applyMagicWand(pos)
            }
            else -> {}
        }
        _activeBooster.value = BoosterType.None
    }

    fun applyMagicWand(center: GridPosition) {
        engineScope.launch {
            _gameState.value = GameState.SpecialAction
            SoundManager.playBooster()
            triggerScreenShake(10f)

            val b = _board.value.map { it.toMutableList() }.toMutableList()

            // Turn center into RainbowBomb
            b[center.row][center.col] = b[center.row][center.col].copy(
                candy = Candy(candyIdCounter++, CandyType.RainbowBomb, SpecialType.Rainbow),
                obstacle = ObstacleType.None
            )

            // Turn surrounding cells into explosive wrapped candies and rockets
            val deltas = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, 1 to 1, -1 to 1, 1 to -1)
            for ((dr, dc) in deltas) {
                val nr = center.row + dr
                val nc = center.col + dc
                if (nr in 0 until height && nc in 0 until width) {
                    val rndType = listOf(CandyType.Strawberry, CandyType.Lemon, CandyType.Blueberry, CandyType.Grape, CandyType.Orange, CandyType.Mint).random()
                    val rndSpecial = listOf(SpecialType.Bomb, SpecialType.RocketRow, SpecialType.RocketCol).random()
                    b[nr][nc] = b[nr][nc].copy(
                        candy = Candy(candyIdCounter++, rndType, rndSpecial),
                        obstacle = ObstacleType.None
                    )
                    spawnParticles(nr, nc, Color(0xFFE040FB), 15)
                }
            }

            spawnParticles(center.row, center.col, Color(0xFF00E5FF), 35)
            spawnFloatingScore(center.row, center.col, "+2500 🪄 MAGIC WAND!", Color(0xFF00E5FF))
            _score.value += 2500
            _board.value = b.map { it.toList() }

            delay(350)
            applyCascadesAndRefill(b)
        }
    }

    fun applyCosmicBlast() {
        engineScope.launch {
            _gameState.value = GameState.SpecialAction
            SoundManager.playRainbowBlast()
            triggerScreenShake(16f)

            val b = _board.value.map { it.toMutableList() }.toMutableList()

            // Destroy all obstacles on the entire board
            for (r in 0 until height) {
                for (c in 0 until width) {
                    val cell = b[r][c]
                    if (cell.obstacle != ObstacleType.None || cell.jellyCount > 0) {
                        damageCell(b, GridPosition(r, c), isDirectMatch = true)
                    }
                }
            }

            // Clear cross pattern across entire board
            val midR = height / 2
            val midC = width / 2
            for (c in 0 until width) {
                damageCell(b, GridPosition(midR, c), isDirectMatch = true)
                spawnParticles(midR, c, Color(0xFF00E5FF), 15)
            }
            for (r in 0 until height) {
                damageCell(b, GridPosition(r, midC), isDirectMatch = true)
                spawnParticles(r, midC, Color(0xFFFF4081), 15)
            }

            spawnFloatingScore(midR, midC, "+5000 🌌 COSMIC BLAST!", Color(0xFFFFD700))
            _score.value += 5000
            _board.value = b.map { it.toList() }

            delay(450)
            applyCascadesAndRefill(b)
        }
    }

    fun applyHammer(pos: GridPosition) {
        engineScope.launch {
            _gameState.value = GameState.SpecialAction
            SoundManager.playBooster()
            triggerScreenShake(8f)

            val b = _board.value.map { it.toMutableList() }.toMutableList()
            val cell = b[pos.row][pos.col]
            damageCell(b, pos, isDirectMatch = true)
            spawnParticles(pos.row, pos.col, Color(0xFFFFD700), 25)
            spawnFloatingScore(pos.row, pos.col, "+500 🔨", Color(0xFFFFD700))
            _score.value += 500
            _board.value = b.map { it.toList() }

            delay(250)
            applyCascadesAndRefill(b)
        }
    }

    fun applyShuffle() {
        engineScope.launch {
            _gameState.value = GameState.SpecialAction
            SoundManager.playBooster()
            val b = _board.value.map { it.toMutableList() }.toMutableList()

            val existingCandies = mutableListOf<Candy>()
            for (r in 0 until height) {
                for (c in 0 until width) {
                    if (b[r][c].obstacle != ObstacleType.Chocolate && b[r][c].candy != null) {
                        existingCandies.add(b[r][c].candy!!)
                    }
                }
            }
            existingCandies.shuffle()

            var idx = 0
            for (r in 0 until height) {
                for (c in 0 until width) {
                    if (b[r][c].obstacle != ObstacleType.Chocolate && b[r][c].candy != null) {
                        b[r][c] = b[r][c].copy(candy = existingCandies[idx++])
                    }
                }
            }
            _board.value = b.map { it.toList() }
            delay(300)
            _gameState.value = GameState.Idle
            resetHintTimer()
        }
    }

    fun applyColorBlast(type: CandyType) {
        engineScope.launch {
            _gameState.value = GameState.SpecialAction
            SoundManager.playRainbowBlast()
            triggerScreenShake(12f)

            val b = _board.value.map { it.toMutableList() }.toMutableList()
            var count = 0
            for (r in 0 until height) {
                for (c in 0 until width) {
                    if (b[r][c].candy?.type == type) {
                        damageCell(b, GridPosition(r, c), isDirectMatch = true)
                        spawnParticles(r, c, getColorForCandy(type), 15)
                        count++
                    }
                }
            }
            val pts = count * 150
            _score.value += pts
            spawnFloatingScore(4, 4, "+$pts 🌈", Color(0xFFFF4081))
            _board.value = b.map { it.toList() }

            delay(350)
            applyCascadesAndRefill(b)
        }
    }

    fun addExtraMoves(count: Int = 5) {
        SoundManager.playBooster()
        _movesLeft.value += count
        if (_gameState.value == GameState.GameOver) {
            _gameState.value = GameState.Idle
            resetHintTimer()
        }
    }

    fun swapCandies(pos1: GridPosition, pos2: GridPosition) {
        if (_gameState.value != GameState.Idle) return
        if (_movesLeft.value <= 0) return

        // Check adjacency
        val isAdjacent = (abs(pos1.row - pos2.row) == 1 && pos1.col == pos2.col) ||
                         (abs(pos1.col - pos2.col) == 1 && pos1.row == pos2.row)
        if (!isAdjacent) return

        val b = _board.value.map { it.toMutableList() }.toMutableList()
        val c1 = b[pos1.row][pos1.col].candy ?: return
        val c2 = b[pos2.row][pos2.col].candy ?: return

        // Cancel hint timer while playing
        hintJob?.cancel()
        _hintCandies.value = emptyList()

        engineScope.launch {
            _gameState.value = GameState.Swapping
            SoundManager.playSwap()

            // Visual swap
            b[pos1.row][pos1.col] = b[pos1.row][pos1.col].copy(candy = c2)
            b[pos2.row][pos2.col] = b[pos2.row][pos2.col].copy(candy = c1)
            _board.value = b.map { it.toList() }
            delay(180)

            // Check Special Combinations
            val isSpecialCombo = handleSpecialCombos(b, pos1, pos2, c1, c2)
            if (isSpecialCombo) {
                _movesLeft.value -= 1
                return@launch
            }

            // Normal match check
            val matchGroups = findMatchGroups(b)
            if (matchGroups.isEmpty()) {
                // Invalid swap: Revert back!
                SoundManager.playInvalidSwap()
                val bBack = b.map { it.toMutableList() }.toMutableList()
                bBack[pos1.row][pos1.col] = bBack[pos1.row][pos1.col].copy(candy = c1)
                bBack[pos2.row][pos2.col] = bBack[pos2.row][pos2.col].copy(candy = c2)
                _board.value = bBack.map { it.toList() }
                delay(180)
                _gameState.value = GameState.Idle
                resetHintTimer()
            } else {
                _movesLeft.value -= 1
                comboCount = 0
                processMatches(b, matchGroups, pivotPos = pos2)
            }
        }
    }

    /**
     * Handles Trademark Candy Crush Special Combinations:
     * - Rainbow + Rainbow: Clears whole board!
     * - Rainbow + Rocket: All candies of rocket color become rockets and detonate!
     * - Rainbow + Bomb: All candies of bomb color become bombs and explode!
     * - Rainbow + Normal: Clears all of that color!
     * - Rocket + Bomb: 3-row & 3-column mega blast!
     * - Rocket + Rocket: Row + Column cross blast!
     * - Bomb + Bomb: 5x5 massive blast!
     */
    private suspend fun handleSpecialCombos(
        board: List<MutableList<Cell>>,
        pos1: GridPosition,
        pos2: GridPosition,
        c1: Candy,
        c2: Candy
    ): Boolean {
        val isRainbow1 = c1.specialType == SpecialType.Rainbow || c1.type == CandyType.RainbowBomb
        val isRainbow2 = c2.specialType == SpecialType.Rainbow || c2.type == CandyType.RainbowBomb

        val isRocket1 = c1.specialType == SpecialType.RocketRow || c1.specialType == SpecialType.RocketCol
        val isRocket2 = c2.specialType == SpecialType.RocketRow || c2.specialType == SpecialType.RocketCol

        val isBomb1 = c1.specialType == SpecialType.Bomb
        val isBomb2 = c2.specialType == SpecialType.Bomb

        when {
            // Rainbow + Rainbow: Complete board wipe!
            isRainbow1 && isRainbow2 -> {
                _gameState.value = GameState.SpecialAction
                SoundManager.playRainbowBlast()
                triggerScreenShake(20f)
                showComboBadge("SUPER RAINBOW WIPEOUT! 🌈", Color(0xFFFF4081))

                for (r in 0 until height) {
                    for (c in 0 until width) {
                        damageCell(board, GridPosition(r, c), isDirectMatch = true)
                        spawnParticles(r, c, Color(0xFFFFEB3B), 10)
                    }
                }
                _score.value += 10000
                _board.value = board.map { it.toList() }
                delay(400)
                applyCascadesAndRefill(board)
                return true
            }

            // Rainbow + Rocket: Transforms all of that color into rockets and fires them!
            (isRainbow1 && isRocket2) || (isRainbow2 && isRocket1) -> {
                val rocket = if (isRocket1) c1 else c2
                val targetColor = rocket.type
                _gameState.value = GameState.SpecialAction
                SoundManager.playRainbowBlast()
                triggerScreenShake(14f)
                showComboBadge("RAINBOW ROCKET STRIKE! 🚀", Color(0xFF00E5FF))

                // Transform
                for (r in 0 until height) {
                    for (c in 0 until width) {
                        if (board[r][c].candy?.type == targetColor) {
                            val special = if (Random.nextBoolean()) SpecialType.RocketRow else SpecialType.RocketCol
                            board[r][c] = board[r][c].copy(candy = board[r][c].candy?.copy(specialType = special))
                        }
                    }
                }
                // Clear the rainbow candy
                board[pos1.row][pos1.col] = board[pos1.row][pos1.col].copy(candy = null)
                board[pos2.row][pos2.col] = board[pos2.row][pos2.col].copy(candy = null)
                _board.value = board.map { it.toList() }
                delay(300)

                // Detonate all transformed rockets
                detonateAllRockets(board)
                _board.value = board.map { it.toList() }
                delay(350)
                applyCascadesAndRefill(board)
                return true
            }

            // Rainbow + Bomb: Transforms all of that color into wrapped bombs!
            (isRainbow1 && isBomb2) || (isRainbow2 && isBomb1) -> {
                val bomb = if (isBomb1) c1 else c2
                val targetColor = bomb.type
                _gameState.value = GameState.SpecialAction
                SoundManager.playRainbowBlast()
                triggerScreenShake(16f)
                showComboBadge("RAINBOW BOMBS AWAY! 💣", Color(0xFFFF5252))

                for (r in 0 until height) {
                    for (c in 0 until width) {
                        if (board[r][c].candy?.type == targetColor) {
                            board[r][c] = board[r][c].copy(candy = board[r][c].candy?.copy(specialType = SpecialType.Bomb))
                        }
                    }
                }
                board[pos1.row][pos1.col] = board[pos1.row][pos1.col].copy(candy = null)
                board[pos2.row][pos2.col] = board[pos2.row][pos2.col].copy(candy = null)
                _board.value = board.map { it.toList() }
                delay(300)

                detonateAllBombs(board)
                _board.value = board.map { it.toList() }
                delay(350)
                applyCascadesAndRefill(board)
                return true
            }

            // Rainbow + Normal Candy: Clears all of that normal candy's color
            isRainbow1 || isRainbow2 -> {
                val normalCandy = if (isRainbow1) c2 else c1
                _gameState.value = GameState.SpecialAction
                SoundManager.playRainbowBlast()
                triggerScreenShake(10f)

                var wiped = 0
                for (r in 0 until height) {
                    for (c in 0 until width) {
                        if (board[r][c].candy?.type == normalCandy.type) {
                            damageCell(board, GridPosition(r, c), isDirectMatch = true)
                            spawnParticles(r, c, getColorForCandy(normalCandy.type), 14)
                            wiped++
                        }
                    }
                }
                // Clear rainbow candy itself
                damageCell(board, pos1, isDirectMatch = true)
                damageCell(board, pos2, isDirectMatch = true)

                val pts = wiped * 120 + 500
                _score.value += pts
                spawnFloatingScore(pos2.row, pos2.col, "+$pts 🌈", Color(0xFFFF4081))
                _board.value = board.map { it.toList() }

                delay(350)
                applyCascadesAndRefill(board)
                return true
            }

            // Rocket + Bomb: Mega 3-row & 3-column blast!
            (isRocket1 && isBomb2) || (isRocket2 && isBomb1) -> {
                _gameState.value = GameState.SpecialAction
                SoundManager.playRocketBlast()
                SoundManager.playBombExplosion()
                triggerScreenShake(16f)
                showComboBadge("MEGA CROSS BLAST! 🚀💣", Color(0xFFFF9100))

                val center = pos2
                for (dr in -1..1) {
                    val r = center.row + dr
                    if (r in 0 until height) {
                        for (c in 0 until width) damageCell(board, GridPosition(r, c), isDirectMatch = true)
                    }
                }
                for (dc in -1..1) {
                    val c = center.col + dc
                    if (c in 0 until width) {
                        for (r in 0 until height) damageCell(board, GridPosition(r, c), isDirectMatch = true)
                    }
                }
                _score.value += 3000
                _board.value = board.map { it.toList() }
                delay(350)
                applyCascadesAndRefill(board)
                return true
            }

            // Rocket + Rocket: Clears row AND column
            isRocket1 && isRocket2 -> {
                _gameState.value = GameState.SpecialAction
                SoundManager.playRocketBlast()
                triggerScreenShake(12f)
                showComboBadge("CROSS BEAM! 🚀", Color(0xFF00E5FF))

                for (c in 0 until width) damageCell(board, GridPosition(pos2.row, c), isDirectMatch = true)
                for (r in 0 until height) damageCell(board, GridPosition(r, pos2.col), isDirectMatch = true)

                _score.value += 1800
                _board.value = board.map { it.toList() }
                delay(300)
                applyCascadesAndRefill(board)
                return true
            }

            // Bomb + Bomb: 5x5 massive explosion!
            isBomb1 && isBomb2 -> {
                _gameState.value = GameState.SpecialAction
                SoundManager.playBombExplosion()
                triggerScreenShake(16f)
                showComboBadge("MASSIVE BLAST! 💣💣", Color(0xFFFF1744))

                for (r in (pos2.row - 2)..(pos2.row + 2)) {
                    for (c in (pos2.col - 2)..(pos2.col + 2)) {
                        if (r in 0 until height && c in 0 until width) {
                            damageCell(board, GridPosition(r, c), isDirectMatch = true)
                        }
                    }
                }
                _score.value += 2500
                _board.value = board.map { it.toList() }
                delay(350)
                applyCascadesAndRefill(board)
                return true
            }
        }
        return false
    }

    private fun detonateAllRockets(board: List<MutableList<Cell>>) {
        SoundManager.playRocketBlast()
        val rockets = mutableListOf<Triple<Int, Int, SpecialType>>()
        for (r in 0 until height) {
            for (c in 0 until width) {
                val s = board[r][c].candy?.specialType
                if (s == SpecialType.RocketRow || s == SpecialType.RocketCol) {
                    rockets.add(Triple(r, c, s))
                }
            }
        }
        for ((r, c, s) in rockets) {
            if (s == SpecialType.RocketRow) {
                for (col in 0 until width) damageCell(board, GridPosition(r, col), isDirectMatch = true)
            } else {
                for (row in 0 until height) damageCell(board, GridPosition(row, c), isDirectMatch = true)
            }
        }
    }

    private fun detonateAllBombs(board: List<MutableList<Cell>>) {
        SoundManager.playBombExplosion()
        val bombs = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until height) {
            for (c in 0 until width) {
                if (board[r][c].candy?.specialType == SpecialType.Bomb) {
                    bombs.add(Pair(r, c))
                }
            }
        }
        for ((r, c) in bombs) {
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until height && nc in 0 until width) {
                        damageCell(board, GridPosition(nr, nc), isDirectMatch = true)
                    }
                }
            }
        }
    }

    private suspend fun processMatches(
        board: List<MutableList<Cell>>,
        matchGroups: List<MatchGroup>,
        pivotPos: GridPosition
    ) {
        comboCount++
        _gameState.value = GameState.Matching

        SoundManager.playMatch(comboCount)
        if (comboCount >= 2) {
            SoundManager.playComboFanfare(comboCount)
            val comboText = when (comboCount) {
                2 -> "Sweet! 🍬"
                3 -> "Tasty! 🍭"
                4 -> "Delicious! ✨"
                else -> "CANDY CUSH COMBO! 🔥"
            }
            val comboColor = when (comboCount) {
                2 -> Color(0xFFFF4081)
                3 -> Color(0xFFFF9100)
                4 -> Color(0xFFE040FB)
                else -> Color(0xFFFFD700)
            }
            showComboBadge(comboText, comboColor)
            triggerScreenShake(comboCount * 2.5f)
        }

        // Determine special candies to create based on match groups
        val specialsToCreate = mutableListOf<Pair<GridPosition, Candy>>()
        val positionsToClear = mutableSetOf<GridPosition>()

        for (group in matchGroups) {
            positionsToClear.addAll(group.positions)
            val candyType = group.candyType
            val size = group.positions.size

            // Preferred position is pivotPos if in group, else first position
            val targetPos = if (group.positions.contains(pivotPos)) pivotPos else group.positions.first()

            when {
                size >= 5 && group.isHorizontal && !group.isCross -> {
                    // 5-in-a-row horizontal -> Rainbow Color Bomb!
                    specialsToCreate.add(targetPos to Candy(++candyIdCounter, CandyType.RainbowBomb, SpecialType.Rainbow))
                }
                size >= 5 && group.isVertical && !group.isCross -> {
                    // 5-in-a-col vertical -> Rainbow Color Bomb!
                    specialsToCreate.add(targetPos to Candy(++candyIdCounter, CandyType.RainbowBomb, SpecialType.Rainbow))
                }
                group.isCross || (size >= 5) -> {
                    // L or T shape -> Wrapped Bomb!
                    specialsToCreate.add(targetPos to Candy(++candyIdCounter, candyType, SpecialType.Bomb))
                }
                size == 4 -> {
                    // 4-in-a-row -> Rocket!
                    val specialType = if (group.isHorizontal) SpecialType.RocketRow else SpecialType.RocketCol
                    specialsToCreate.add(targetPos to Candy(++candyIdCounter, candyType, specialType))
                }
            }
        }

        // Trigger existing special candies being cleared in this match!
        for (pos in positionsToClear) {
            val special = board[pos.row][pos.col].candy?.specialType
            when (special) {
                SpecialType.RocketRow -> {
                    SoundManager.playRocketBlast()
                    for (c in 0 until width) positionsToClear.add(GridPosition(pos.row, c))
                }
                SpecialType.RocketCol -> {
                    SoundManager.playRocketBlast()
                    for (r in 0 until height) positionsToClear.add(GridPosition(r, pos.col))
                }
                SpecialType.Bomb -> {
                    SoundManager.playBombExplosion()
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = pos.row + dr
                            val nc = pos.col + dc
                            if (nr in 0 until height && nc in 0 until width) {
                                positionsToClear.add(GridPosition(nr, nc))
                            }
                        }
                    }
                }
                else -> {}
            }
        }

        // Apply score and clear candies
        val matchScore = positionsToClear.size * 20 * comboCount
        _score.value += matchScore
        spawnFloatingScore(pivotPos.row, pivotPos.col, "+$matchScore", Color(0xFFFFD54F))

        for (pos in positionsToClear) {
            val c = board[pos.row][pos.col].candy
            if (c != null) {
                spawnParticles(pos.row, pos.col, getColorForCandy(c.type), 8)
                checkObjectiveOnCandyCleared(c.type)
            }
            damageCell(board, pos, isDirectMatch = true)
        }

        // Check adjacent damage for obstacles (Ice & Chocolate)
        for (pos in positionsToClear) {
            damageAdjacentObstacles(board, pos)
        }

        // Place newly forged Special Candies!
        for ((pos, specialCandy) in specialsToCreate) {
            board[pos.row][pos.col] = board[pos.row][pos.col].copy(candy = specialCandy)
            spawnParticles(pos.row, pos.col, Color.White, 16)
        }

        _board.value = board.map { it.toList() }
        delay(220)

        applyCascadesAndRefill(board)
    }

    private suspend fun applyCascadesAndRefill(board: List<MutableList<Cell>>) {
        // 1. Drop candies (Gravity)
        _gameState.value = GameState.Falling
        applyGravity(board)
        _board.value = board.map { it.toList() }
        delay(180)

        // 2. Refill empty cells
        refillBoard(board)
        _board.value = board.map { it.toList() }
        delay(220)

        // 3. Check cascading matches
        val nextMatches = findMatchGroups(board)
        if (nextMatches.isNotEmpty()) {
            processMatches(board, nextMatches, pivotPos = nextMatches.first().positions.first())
        } else {
            // Cascade finished! Check game outcome and deadlock
            checkGameOutcome()
            if (_gameState.value == GameState.Idle) {
                ensureValidMovesExist(board)
                resetHintTimer()
            }
        }
    }

    private fun checkObjectiveOnCandyCleared(type: CandyType) {
        val obj = _currentLevel.value.objective
        if (obj is LevelObjective.CollectCandy && obj.candyType == type) {
            _objectiveProgress.value += 1
        }
    }

    private fun damageCell(board: List<MutableList<Cell>>, pos: GridPosition, isDirectMatch: Boolean) {
        val currentCell = board[pos.row][pos.col]
        var newJelly = currentCell.jellyCount
        if (newJelly > 0 && isDirectMatch) {
            newJelly -= 1
            val obj = _currentLevel.value.objective
            if (obj is LevelObjective.ClearAllJelly) {
                _objectiveProgress.value += 1
            }
        }

        var newObstacle = currentCell.obstacle
        if (newObstacle == ObstacleType.Chocolate && isDirectMatch) {
            newObstacle = ObstacleType.None
        }

        board[pos.row][pos.col] = currentCell.copy(
            candy = null,
            jellyCount = newJelly,
            obstacle = newObstacle
        )
    }

    private fun damageAdjacentObstacles(board: List<MutableList<Cell>>, pos: GridPosition) {
        val neighbors = listOf(
            GridPosition(pos.row - 1, pos.col),
            GridPosition(pos.row + 1, pos.col),
            GridPosition(pos.row, pos.col - 1),
            GridPosition(pos.row, pos.col + 1)
        )
        for (n in neighbors) {
            if (n.row in 0 until height && n.col in 0 until width) {
                val cell = board[n.row][n.col]
                if (cell.obstacle == ObstacleType.Ice) {
                    board[n.row][n.col] = cell.copy(obstacle = ObstacleType.None)
                    spawnParticles(n.row, n.col, Color(0xFFB3E5FC), 12)
                } else if (cell.obstacle == ObstacleType.Chocolate) {
                    board[n.row][n.col] = cell.copy(obstacle = ObstacleType.None)
                    spawnParticles(n.row, n.col, Color(0xFF5D4037), 12)
                }
            }
        }
    }

    private fun applyGravity(board: List<MutableList<Cell>>) {
        for (c in 0 until width) {
            var emptyRow = height - 1
            for (r in height - 1 downTo 0) {
                if (board[r][c].obstacle != ObstacleType.Chocolate) {
                    if (board[r][c].candy != null) {
                        if (emptyRow != r) {
                            board[emptyRow][c] = board[emptyRow][c].copy(candy = board[r][c].candy)
                            board[r][c] = board[r][c].copy(candy = null)
                        }
                        emptyRow--
                    }
                } else {
                    emptyRow = r - 1
                }
            }
        }
    }

    private fun refillBoard(board: List<MutableList<Cell>>) {
        for (c in 0 until width) {
            for (r in 0 until height) {
                if (board[r][c].obstacle != ObstacleType.Chocolate && board[r][c].candy == null) {
                    board[r][c] = board[r][c].copy(candy = generateRandomCandy(allowRainbow = true))
                }
            }
        }
    }

    private fun checkGameOutcome() {
        val level = _currentLevel.value
        val isObjectiveComplete = when (val obj = level.objective) {
            is LevelObjective.TargetScore -> _score.value >= obj.score
            is LevelObjective.ClearAllJelly -> _objectiveProgress.value >= obj.total
            is LevelObjective.CollectCandy -> _objectiveProgress.value >= obj.count
        }

        if (isObjectiveComplete) {
            _gameState.value = GameState.LevelComplete
            SoundManager.playLevelWin()
            showComboBadge("LEVEL COMPLETE! 🏆", Color(0xFFFFD700))
        } else if (_movesLeft.value <= 0) {
            _gameState.value = GameState.GameOver
            SoundManager.playLevelFail()
        } else {
            _gameState.value = GameState.Idle
        }
    }

    private fun ensureValidMovesExist(board: List<MutableList<Cell>>) {
        if (!hasAnyValidMove(board)) {
            // Auto reshuffle if deadlock!
            showComboBadge("RESHUFFLING... 🔄", Color(0xFF00E5FF))
            applyShuffle()
        }
    }

    private fun hasAnyValidMove(board: List<List<Cell>>): Boolean {
        for (r in 0 until height) {
            for (c in 0 until width) {
                // Check right swap
                if (c < width - 1) {
                    if (simulatedSwapProducesMatch(board, GridPosition(r, c), GridPosition(r, c + 1))) return true
                }
                // Check down swap
                if (r < height - 1) {
                    if (simulatedSwapProducesMatch(board, GridPosition(r, c), GridPosition(r + 1, c))) return true
                }
            }
        }
        return false
    }

    private fun simulatedSwapProducesMatch(board: List<List<Cell>>, p1: GridPosition, p2: GridPosition): Boolean {
        val c1 = board[p1.row][p1.col].candy ?: return false
        val c2 = board[p2.row][p2.col].candy ?: return false

        if (c1.specialType == SpecialType.Rainbow || c2.specialType == SpecialType.Rainbow) return true
        if (c1.specialType != SpecialType.None && c2.specialType != SpecialType.None) return true

        val copy = board.map { it.toMutableList() }
        copy[p1.row][p1.col] = copy[p1.row][p1.col].copy(candy = c2)
        copy[p2.row][p2.col] = copy[p2.row][p2.col].copy(candy = c1)

        return findMatchGroups(copy).isNotEmpty()
    }

    private fun resetHintTimer() {
        hintJob?.cancel()
        _hintCandies.value = emptyList()
        hintJob = engineScope.launch {
            delay(4200) // 4.2 seconds of idle time triggers hint
            if (!isActive || _gameState.value != GameState.Idle) return@launch

            val b = _board.value
            for (r in 0 until height) {
                for (c in 0 until width) {
                    if (c < width - 1 && simulatedSwapProducesMatch(b, GridPosition(r, c), GridPosition(r, c + 1))) {
                        _hintCandies.value = listOf(GridPosition(r, c), GridPosition(r, c + 1))
                        return@launch
                    }
                    if (r < height - 1 && simulatedSwapProducesMatch(b, GridPosition(r, c), GridPosition(r + 1, c))) {
                        _hintCandies.value = listOf(GridPosition(r, c), GridPosition(r + 1, c))
                        return@launch
                    }
                }
            }
        }
    }

    private fun triggerScreenShake(intensity: Float) {
        engineScope.launch {
            _shakeIntensity.value = intensity
            delay(200)
            _shakeIntensity.value = 0f
        }
    }

    private fun showComboBadge(text: String, color: Color) {
        val badge = ComboBadge(System.currentTimeMillis(), text, color)
        _comboBadges.value = listOf(badge)
        engineScope.launch {
            delay(1200)
            _comboBadges.value = _comboBadges.value.filter { it.id != badge.id }
        }
    }

    private fun spawnFloatingScore(row: Int, col: Int, text: String, color: Color) {
        val item = FloatingScore(System.currentTimeMillis(), text, row, col, color)
        _floatingScores.value = _floatingScores.value + item
        engineScope.launch {
            delay(750)
            _floatingScores.value = _floatingScores.value.filter { it.id != item.id }
        }
    }

    private fun spawnParticles(row: Int, col: Int, color: Color, count: Int) {
        val newParticles = List(count) {
            val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
            val speed = Random.nextFloat() * 0.08f + 0.02f
            Particle(
                id = Random.nextLong(),
                x = (col + 0.5f) / width,
                y = (row + 0.5f) / height,
                vx = kotlin.math.cos(angle) * speed,
                vy = kotlin.math.sin(angle) * speed,
                color = color,
                size = Random.nextFloat() * 6f + 4f
            )
        }
        _particles.value = _particles.value + newParticles
        engineScope.launch {
            delay(400)
            _particles.value = emptyList()
        }
    }

    private fun getColorForCandy(type: CandyType): Color {
        return when (type) {
            CandyType.Strawberry -> Color(0xFFFF1744)
            CandyType.Lemon -> Color(0xFFFFEA00)
            CandyType.Blueberry -> Color(0xFF2979FF)
            CandyType.Grape -> Color(0xFFE040FB)
            CandyType.Orange -> Color(0xFFFF9100)
            CandyType.Mint -> Color(0xFF00E676)
            CandyType.RainbowBomb -> Color(0xFFFF4081)
            CandyType.Empty -> Color.Transparent
        }
    }

    data class MatchGroup(
        val positions: Set<GridPosition>,
        val candyType: CandyType,
        val isHorizontal: Boolean,
        val isVertical: Boolean,
        val isCross: Boolean
    )

    private fun findMatchGroups(board: List<List<Cell>>): List<MatchGroup> {
        val groups = mutableListOf<MatchGroup>()
        val horizontalMatches = mutableListOf<Pair<CandyType, Set<GridPosition>>>()
        val verticalMatches = mutableListOf<Pair<CandyType, Set<GridPosition>>>()

        // Horizontal matches
        for (r in 0 until height) {
            var count = 1
            for (c in 1 until width) {
                val curr = board[r][c].candy
                val prev = board[r][c - 1].candy
                if (curr != null && prev != null && curr.type == prev.type && curr.type != CandyType.RainbowBomb && curr.type != CandyType.Empty) {
                    count++
                } else {
                    if (count >= 3) {
                        val type = board[r][c - 1].candy!!.type
                        val set = (0 until count).map { GridPosition(r, c - 1 - it) }.toSet()
                        horizontalMatches.add(type to set)
                    }
                    count = 1
                }
            }
            if (count >= 3) {
                val type = board[r][width - 1].candy!!.type
                val set = (0 until count).map { GridPosition(r, width - 1 - it) }.toSet()
                horizontalMatches.add(type to set)
            }
        }

        // Vertical matches
        for (c in 0 until width) {
            var count = 1
            for (r in 1 until height) {
                val curr = board[r][c].candy
                val prev = board[r - 1][c].candy
                if (curr != null && prev != null && curr.type == prev.type && curr.type != CandyType.RainbowBomb && curr.type != CandyType.Empty) {
                    count++
                } else {
                    if (count >= 3) {
                        val type = board[r - 1][c].candy!!.type
                        val set = (0 until count).map { GridPosition(r - 1 - it, c) }.toSet()
                        verticalMatches.add(type to set)
                    }
                    count = 1
                }
            }
            if (count >= 3) {
                val type = board[height - 1][c].candy!!.type
                val set = (0 until count).map { GridPosition(height - 1 - it, c) }.toSet()
                verticalMatches.add(type to set)
            }
        }

        // Merge T/L intersections
        val usedHoriz = mutableSetOf<Int>()
        val usedVert = mutableSetOf<Int>()

        horizontalMatches.forEachIndexed { hIdx, (hType, hSet) ->
            verticalMatches.forEachIndexed { vIdx, (vType, vSet) ->
                if (hType == vType) {
                    val intersection = hSet.intersect(vSet)
                    if (intersection.isNotEmpty()) {
                        groups.add(
                            MatchGroup(
                                positions = hSet + vSet,
                                candyType = hType,
                                isHorizontal = true,
                                isVertical = true,
                                isCross = true
                            )
                        )
                        usedHoriz.add(hIdx)
                        usedVert.add(vIdx)
                    }
                }
            }
        }

        horizontalMatches.forEachIndexed { idx, (type, set) ->
            if (!usedHoriz.contains(idx)) {
                groups.add(MatchGroup(set, type, isHorizontal = true, isVertical = false, isCross = false))
            }
        }

        verticalMatches.forEachIndexed { idx, (type, set) ->
            if (!usedVert.contains(idx)) {
                groups.add(MatchGroup(set, type, isHorizontal = false, isVertical = true, isCross = false))
            }
        }

        return groups
    }
}
