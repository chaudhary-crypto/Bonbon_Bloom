package com.example.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.GameApplication
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.data.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(
    private val repository: UserRepository
) : ViewModel() {
    private val engine = GameEngine()

    val board = engine.board
    val score = engine.score
    val movesLeft = engine.movesLeft
    val gameState = engine.gameState
    val currentLevel = engine.currentLevel
    val objectiveProgress = engine.objectiveProgress
    val comboBadges = engine.comboBadges
    val floatingScores = engine.floatingScores
    val particles = engine.particles
    val shakeIntensity = engine.shakeIntensity
    val hintCandies = engine.hintCandies
    val activeBooster = engine.activeBooster

    val userProfile = repository.userProfile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserProfile()
    )

    private var hasDeductedLifeForCurrentGame = false

    fun startLevel(levelId: Int) {
        viewModelScope.launch {
            if (!hasDeductedLifeForCurrentGame) {
                val hasLife = repository.deductLife()
                if (hasLife) {
                    hasDeductedLifeForCurrentGame = true
                }
            }
            // Always load the level so the game board is never blank or hanging
            engine.loadLevel(levelId)
        }
    }

    fun swap(pos1: GridPosition, pos2: GridPosition) {
        val booster = activeBooster.value
        if (booster == BoosterType.MagicSwap) {
            // Magic swap bypasses match condition!
            viewModelScope.launch {
                val success = repository.useBooster(BoosterType.MagicSwap)
                if (success) {
                    SoundManager.playBooster()
                    engine.setActiveBooster(BoosterType.None)
                    // Perform swap freely
                    engine.swapCandies(pos1, pos2)
                }
            }
            return
        }
        engine.swapCandies(pos1, pos2)
    }

    fun onCellClicked(pos: GridPosition) {
        val booster = activeBooster.value
        if (booster != BoosterType.None) {
            viewModelScope.launch {
                val used = repository.useBooster(booster)
                if (used) {
                    engine.onCellClicked(pos)
                }
            }
        }
    }

    fun selectBooster(booster: BoosterType) {
        SoundManager.playButtonClick()
        val profile = userProfile.value
        when (booster) {
            BoosterType.ExtraMoves -> {
                if (profile.extraMovesCount > 0) {
                    viewModelScope.launch {
                        if (repository.useBooster(BoosterType.ExtraMoves)) {
                            engine.addExtraMoves(5)
                        }
                    }
                }
            }
            BoosterType.Shuffle -> {
                if (profile.shuffleCount > 0) {
                    viewModelScope.launch {
                        if (repository.useBooster(BoosterType.Shuffle)) {
                            engine.applyShuffle()
                        }
                    }
                }
            }
            BoosterType.Hammer -> {
                if (profile.hammerCount > 0) {
                    engine.setActiveBooster(BoosterType.Hammer)
                }
            }
            BoosterType.ColorBlast -> {
                if (profile.colorBlastCount > 0) {
                    engine.setActiveBooster(BoosterType.ColorBlast)
                }
            }
            BoosterType.MagicWand -> {
                if (profile.magicWandCount > 0) {
                    engine.setActiveBooster(BoosterType.MagicWand)
                }
            }
            BoosterType.CosmicBlast -> {
                if (profile.cosmicBlastCount > 0) {
                    viewModelScope.launch {
                        if (repository.useBooster(BoosterType.CosmicBlast)) {
                            engine.applyCosmicBlast()
                        }
                    }
                }
            }
            BoosterType.MagicSwap -> {
                engine.setActiveBooster(BoosterType.MagicSwap)
            }
            BoosterType.None -> {
                engine.setActiveBooster(BoosterType.None)
            }
        }
    }

    fun buyExtraMovesWithCoins(cost: Int = 100) {
        viewModelScope.launch {
            val profile = userProfile.value
            if (profile.coins >= cost) {
                repository.addCoins(-cost)
                engine.addExtraMoves(5)
            }
        }
    }

    fun grantFreeMovesFromAd(moves: Int = 5) {
        engine.addExtraMoves(moves)
    }

    fun buyMovesWithDiamonds(diamondCost: Int = 5, moves: Int = 5) {
        viewModelScope.launch {
            if (repository.deductDiamonds(diamondCost)) {
                engine.addExtraMoves(moves)
            }
        }
    }

    fun skipLevelWithDiamonds(diamondCost: Int = 30) {
        viewModelScope.launch {
            val lvl = currentLevel.value
            if (repository.skipLevelWithDiamonds(lvl.id, diamondCost)) {
                engine.setGameState(GameState.LevelComplete)
            }
        }
    }

    fun subscribeVip(days: Int = 7) {
        viewModelScope.launch {
            repository.subscribeVip(days)
        }
    }

    fun claimVipDailyDiamonds() {
        viewModelScope.launch {
            repository.claimVipDailyDiamonds()
        }
    }

    fun buyDiamondsPack(diamonds: Int, bonusCoins: Int = 0) {
        viewModelScope.launch {
            repository.buyDiamondsPack(diamonds, bonusCoins)
        }
    }

    fun buyCoinsWithDiamonds(coinsAmount: Int, diamondCost: Int) {
        viewModelScope.launch {
            repository.buyCoinsWithDiamonds(coinsAmount, diamondCost)
        }
    }

    fun buyHighValueBooster(booster: BoosterType, count: Int, diamondCost: Int) {
        viewModelScope.launch {
            repository.buyHighValueBoosterWithDiamonds(booster, count, diamondCost)
        }
    }

    fun activateInfiniteLives(hours: Int = 2, diamondCost: Int = 15) {
        viewModelScope.launch {
            repository.activateInfiniteLivesWithDiamonds(hours, diamondCost)
        }
    }

    fun buyBooster(booster: BoosterType, cost: Int) {
        viewModelScope.launch {
            repository.buyBooster(booster, cost)
        }
    }

    fun grantBoosterReward(booster: BoosterType, count: Int = 1) {
        viewModelScope.launch {
            repository.grantBoosterReward(booster, count)
        }
    }

    fun addCoins(amount: Int) {
        viewModelScope.launch {
            repository.addCoins(amount)
        }
    }

    fun refillLivesFree() {
        viewModelScope.launch {
            repository.refillLives()
        }
    }

    fun doubleLevelCoinReward(extraCoins: Int) {
        viewModelScope.launch {
            repository.addCoins(extraCoins)
        }
    }

    fun onLevelCompleted() {
        viewModelScope.launch {
            val lvl = currentLevel.value
            val sc = score.value
            val stars = when {
                sc >= lvl.star3Score -> 3
                sc >= lvl.star2Score -> 2
                else -> 1
            }
            val coinsEarned = 50 + stars * 25
            repository.completeLevel(lvl.id, stars, sc, coinsEarned)
            hasDeductedLifeForCurrentGame = false
        }
    }

    fun retryLevel() {
        hasDeductedLifeForCurrentGame = false
        startLevel(currentLevel.value.id)
    }

    fun toggleSound() {
        viewModelScope.launch {
            repository.toggleSound()
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            repository.toggleMusic()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GameApplication)
                GameViewModel(UserRepository(app.database.userDao()))
            }
        }
    }
}
