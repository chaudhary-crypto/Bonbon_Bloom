package com.example.data

import com.example.audio.SoundManager
import com.example.game.BoosterType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserRepository(private val userDao: UserDao) {
    val userProfile: Flow<UserProfile> = userDao.getUserProfile().map { raw ->
        val profile = raw ?: UserProfile()
        // Check life regeneration (1 life every 30 minutes, max 5)
        checkLifeRegeneration(profile)
    }

    val allLevelProgress: Flow<List<LevelProgress>> = userDao.getAllLevelProgress()

    private fun checkLifeRegeneration(profile: UserProfile): UserProfile {
        if (profile.lives >= profile.maxLives) return profile
        val now = System.currentTimeMillis()
        val elapsedMs = now - profile.lastLifeUpdate
        val regenIntervalMs = 30 * 60 * 1000L // 30 minutes
        val livesToAdd = (elapsedMs / regenIntervalMs).toInt()
        if (livesToAdd > 0) {
            val newLives = (profile.lives + livesToAdd).coerceAtMost(profile.maxLives)
            val updatedLastTime = if (newLives >= profile.maxLives) now else (profile.lastLifeUpdate + livesToAdd * regenIntervalMs)
            return profile.copy(lives = newLives, lastLifeUpdate = updatedLastTime)
        }
        return profile
    }

    suspend fun saveProfile(profile: UserProfile) {
        userDao.saveUserProfile(profile)
    }

    suspend fun addCoins(amount: Int) {
        userDao.addCoins(amount)
    }

    suspend fun addDiamonds(amount: Int) {
        userDao.addDiamonds(amount)
    }

    suspend fun deductLife(): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        // If user has VIP active or Infinite Lives pass, never deduct life!
        if (current.hasInfiniteLives) {
            return true
        }
        if (current.lives <= 0) return false
        val updated = current.copy(
            lives = current.lives - 1,
            lastLifeUpdate = if (current.lives == current.maxLives) System.currentTimeMillis() else current.lastLifeUpdate
        )
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun refillLives(coinCost: Int = 100): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.coins < coinCost) return false
        val updated = current.copy(
            coins = current.coins - coinCost,
            lives = current.maxLives,
            lastLifeUpdate = System.currentTimeMillis()
        )
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun refillLivesFree() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            lives = current.maxLives,
            lastLifeUpdate = System.currentTimeMillis()
        )
        userDao.saveUserProfile(updated)
    }

    suspend fun grantBoosterReward(booster: BoosterType, count: Int = 1) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = when (booster) {
            BoosterType.Hammer -> current.copy(hammerCount = current.hammerCount + count)
            BoosterType.Shuffle -> current.copy(shuffleCount = current.shuffleCount + count)
            BoosterType.ColorBlast -> current.copy(colorBlastCount = current.colorBlastCount + count)
            BoosterType.ExtraMoves -> current.copy(extraMovesCount = current.extraMovesCount + count)
            BoosterType.MagicWand -> current.copy(magicWandCount = current.magicWandCount + count)
            BoosterType.CosmicBlast -> current.copy(cosmicBlastCount = current.cosmicBlastCount + count)
            else -> current
        }
        userDao.saveUserProfile(updated)
    }

    suspend fun completeLevel(levelId: Int, stars: Int, score: Int, coinReward: Int) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val nextLevel = (levelId + 1).coerceAtMost(100)
        val highest = maxOf(current.highestUnlockedLevel, nextLevel)

        // VIP Pass gives 2x Coins
        val finalReward = if (current.isVipActive) coinReward * 2 else coinReward

        val updated = current.copy(
            coins = current.coins + finalReward,
            highestUnlockedLevel = highest
        )
        userDao.saveUserProfile(updated)

        val existingProgress = userDao.getLevelProgress(levelId)
        val bestStars = maxOf(existingProgress?.stars ?: 0, stars)
        val bestScore = maxOf(existingProgress?.highScore ?: 0, score)
        userDao.saveLevelProgress(
            LevelProgress(
                levelId = levelId,
                stars = bestStars,
                highScore = bestScore,
                completed = true
            )
        )
    }

    suspend fun useBooster(booster: BoosterType): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = when (booster) {
            BoosterType.Hammer -> {
                if (current.hammerCount <= 0) return false
                current.copy(hammerCount = current.hammerCount - 1)
            }
            BoosterType.Shuffle -> {
                if (current.shuffleCount <= 0) return false
                current.copy(shuffleCount = current.shuffleCount - 1)
            }
            BoosterType.ColorBlast -> {
                if (current.colorBlastCount <= 0) return false
                current.copy(colorBlastCount = current.colorBlastCount - 1)
            }
            BoosterType.ExtraMoves -> {
                if (current.extraMovesCount <= 0) return false
                current.copy(extraMovesCount = current.extraMovesCount - 1)
            }
            BoosterType.MagicWand -> {
                if (current.magicWandCount <= 0) return false
                current.copy(magicWandCount = current.magicWandCount - 1)
            }
            BoosterType.CosmicBlast -> {
                if (current.cosmicBlastCount <= 0) return false
                current.copy(cosmicBlastCount = current.cosmicBlastCount - 1)
            }
            else -> return true
        }
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun buyBooster(booster: BoosterType, cost: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.coins < cost) return false
        val updated = when (booster) {
            BoosterType.Hammer -> current.copy(coins = current.coins - cost, hammerCount = current.hammerCount + 3)
            BoosterType.Shuffle -> current.copy(coins = current.coins - cost, shuffleCount = current.shuffleCount + 3)
            BoosterType.ColorBlast -> current.copy(coins = current.coins - cost, colorBlastCount = current.colorBlastCount + 2)
            BoosterType.ExtraMoves -> current.copy(coins = current.coins - cost, extraMovesCount = current.extraMovesCount + 3)
            else -> return false
        }
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun buyDiamondsPack(diamonds: Int, bonusCoins: Int = 0) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            diamonds = current.diamonds + diamonds,
            coins = current.coins + bonusCoins
        )
        userDao.saveUserProfile(updated)
    }

    suspend fun buyCoinsWithDiamonds(coinsAmount: Int, diamondCost: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < diamondCost) return false
        val updated = current.copy(
            diamonds = current.diamonds - diamondCost,
            coins = current.coins + coinsAmount
        )
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun buyHighValueBoosterWithDiamonds(booster: BoosterType, count: Int, diamondCost: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < diamondCost) return false
        val updated = when (booster) {
            BoosterType.MagicWand -> current.copy(
                diamonds = current.diamonds - diamondCost,
                magicWandCount = current.magicWandCount + count
            )
            BoosterType.CosmicBlast -> current.copy(
                diamonds = current.diamonds - diamondCost,
                cosmicBlastCount = current.cosmicBlastCount + count
            )
            BoosterType.Hammer -> current.copy(
                diamonds = current.diamonds - diamondCost,
                hammerCount = current.hammerCount + count
            )
            BoosterType.ColorBlast -> current.copy(
                diamonds = current.diamonds - diamondCost,
                colorBlastCount = current.colorBlastCount + count
            )
            else -> current.copy(diamonds = current.diamonds - diamondCost)
        }
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun activateInfiniteLivesWithDiamonds(hours: Int = 2, diamondCost: Int = 15): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < diamondCost) return false
        val now = System.currentTimeMillis()
        val durationMs = hours * 3600 * 1000L
        val newExpiry = maxOf(current.infiniteLivesExpiryTimestamp, now) + durationMs
        val updated = current.copy(
            diamonds = current.diamonds - diamondCost,
            lives = current.maxLives,
            infiniteLivesExpiryTimestamp = newExpiry
        )
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun subscribeVip(days: Int = 7): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val now = System.currentTimeMillis()
        val durationMs = days * 24 * 3600 * 1000L
        val newExpiry = maxOf(current.vipExpiryTimestamp, now) + durationMs
        val updated = current.copy(
            isVipSubscribed = true,
            vipExpiryTimestamp = newExpiry,
            diamonds = current.diamonds + 50, // instant VIP welcome diamond reward
            magicWandCount = current.magicWandCount + 2,
            cosmicBlastCount = current.cosmicBlastCount + 2,
            lives = current.maxLives
        )
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun deductDiamonds(amount: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < amount) return false
        val updated = current.copy(diamonds = current.diamonds - amount)
        userDao.saveUserProfile(updated)
        return true
    }

    suspend fun claimVipDailyDiamonds(): Int {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (!current.isVipActive) return 0
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000L)
        if (current.lastVipDailyClaim >= today) return 0
        val reward = 25
        val updated = current.copy(
            diamonds = current.diamonds + reward,
            lastVipDailyClaim = today
        )
        userDao.saveUserProfile(updated)
        return reward
    }

    suspend fun skipLevelWithDiamonds(levelId: Int, cost: Int = 30): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < cost) return false
        val nextLevel = (levelId + 1).coerceAtMost(100)
        val highest = maxOf(current.highestUnlockedLevel, nextLevel)
        val updated = current.copy(
            diamonds = current.diamonds - cost,
            highestUnlockedLevel = highest
        )
        userDao.saveUserProfile(updated)
        userDao.saveLevelProgress(
            LevelProgress(
                levelId = levelId,
                stars = 3,
                highScore = 50000,
                completed = true
            )
        )
        return true
    }

    suspend fun toggleSound() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val newSound = !current.soundEnabled
        SoundManager.soundEnabled = newSound
        userDao.saveUserProfile(current.copy(soundEnabled = newSound))
    }

    suspend fun toggleMusic() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val newMusic = !current.musicEnabled
        SoundManager.musicEnabled = newMusic
        userDao.saveUserProfile(current.copy(musicEnabled = newMusic))
    }
}
