package com.example.data

import com.example.audio.SoundManager
import com.example.game.BoosterType
import com.example.security.AntiCheatManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserRepository(private val userDao: UserDao) {
    val userProfile: Flow<UserProfile> = userDao.getUserProfile().map { raw ->
        val profile = raw ?: UserProfile()
        // Verify cryptographic signature integrity
        val isTampered = !AntiCheatManager.verifySignature(profile) && profile.securitySignature.isNotEmpty()
        val checked = if (isTampered) {
            profile.copy(isCheatingDetected = true, cheatAttemptsCount = profile.cheatAttemptsCount + 1)
        } else {
            profile
        }
        // Check life regeneration (1 life every 30 minutes, max 5)
        checkLifeRegeneration(checked)
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
        val signed = AntiCheatManager.signProfile(profile)
        userDao.saveUserProfile(signed)
        try {
            com.example.auth.CloudSyncManager.syncToCloud(signed)
        } catch (e: Exception) {
            // Ignore error
        }
    }

    suspend fun addCoins(amount: Int) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(coins = (current.coins + amount).coerceAtLeast(0))
        saveProfile(updated)
    }

    suspend fun addDiamonds(amount: Int) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(diamonds = (current.diamonds + amount).coerceAtLeast(0))
        saveProfile(updated)
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
        val nextLevel = (levelId + 1).coerceAtMost(1000)
        val highest = maxOf(current.highestUnlockedLevel, nextLevel)

        // VIP Pass gives 2x Coins
        val finalReward = if (current.isVipActive) coinReward * 2 else coinReward
        val newPiggy = (current.piggyBankCoins + 50).coerceAtMost(current.piggyBankCapacity)

        val updated = current.copy(
            coins = current.coins + finalReward,
            piggyBankCoins = newPiggy,
            highestUnlockedLevel = highest
        )
        saveProfile(updated)

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
        val nextLevel = (levelId + 1).coerceAtMost(1000)
        val highest = maxOf(current.highestUnlockedLevel, nextLevel)
        val updated = current.copy(
            diamonds = current.diamonds - cost,
            highestUnlockedLevel = highest
        )
        saveProfile(updated)
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

    // --- Piggy Bank & Lucky Spin Wheel Operations ---

    suspend fun smashPiggyBank(): Int {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val collected = current.piggyBankCoins
        if (collected <= 0) return 0
        val updated = current.copy(
            coins = current.coins + collected,
            piggyBankCoins = 0
        )
        saveProfile(updated)
        return collected
    }

    suspend fun claimLuckyWheelReward(rewardIndex: Int): Pair<String, Int> {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = when (rewardIndex % 8) {
            0 -> current.copy(coins = current.coins + 250) // 250 Coins
            1 -> current.copy(diamonds = current.diamonds + 10) // 10 Diamonds
            2 -> current.copy(hammerCount = current.hammerCount + 1) // 1 Hammer
            3 -> current.copy(extraMovesCount = current.extraMovesCount + 2) // +2 Moves
            4 -> current.copy(coins = current.coins + 500) // 500 Coins
            5 -> current.copy(colorBlastCount = current.colorBlastCount + 1) // 1 Color Blast
            6 -> {
                // 1 hour infinite lives
                val currentExpiry = maxOf(current.infiniteLivesExpiryTimestamp, System.currentTimeMillis())
                current.copy(infiniteLivesExpiryTimestamp = currentExpiry + 3600 * 1000L)
            }
            else -> current.copy(shuffleCount = current.shuffleCount + 2) // 2 Shuffles
        }.let {
            it.copy(
                lastLuckyWheelSpinDate = System.currentTimeMillis(),
                freeLuckySpinsCount = maxOf(0, it.freeLuckySpinsCount - 1)
            )
        }
        saveProfile(updated)

        val rewardInfo = when (rewardIndex % 8) {
            0 -> "250 Coins" to 250
            1 -> "10 Diamonds" to 10
            2 -> "1 Lollipop Hammer" to 1
            3 -> "+2 Extra Moves" to 2
            4 -> "500 Coins" to 500
            5 -> "1 Color Blast" to 1
            6 -> "1 Hr Infinite Lives" to 1
            else -> "2 Shuffles" to 2
        }
        return rewardInfo
    }

    suspend fun grantFreeLuckySpin() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(freeLuckySpinsCount = current.freeLuckySpinsCount + 1)
        saveProfile(updated)
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

    suspend fun toggleHaptics(enabled: Boolean) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        com.example.audio.HapticManager.hapticsEnabled = enabled
        saveProfile(current.copy(hapticsEnabled = enabled))
    }

    suspend fun setLanguage(languageCode: String) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        saveProfile(current.copy(languageCode = languageCode))
    }

    suspend fun setHasRemovedAds(removed: Boolean) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        saveProfile(current.copy(hasRemovedAds = removed))
    }

    suspend fun markNotificationPermissionAsked() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        saveProfile(current.copy(hasAskedNotificationPermission = true))
    }

    // --- Authentication & Multi-Provider Login ---

    suspend fun loginWithGoogle(email: String, displayName: String, photoUrl: String? = null) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val name = displayName.ifBlank { email.substringBefore("@") }
        val updated = current.copy(
            isLoggedIn = true,
            authProvider = "GOOGLE",
            username = name,
            email = email,
            avatarUrl = photoUrl,
            lastCloudSyncTime = System.currentTimeMillis()
        )
        saveProfile(updated)
        try {
            com.example.auth.CloudSyncManager.fetchFromCloud(this)
        } catch (e: Exception) {}
    }

    suspend fun loginWithEmail(email: String, displayName: String? = null) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val name = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        val updated = current.copy(
            isLoggedIn = true,
            authProvider = "EMAIL",
            username = name,
            email = email,
            lastCloudSyncTime = System.currentTimeMillis()
        )
        saveProfile(updated)
    }

    suspend fun loginAsAdmin() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val adminProfile = com.example.auth.AdminAuthManager.buildAdminProfile(current)
        saveProfile(adminProfile)
    }

    suspend fun loginWithPhone(phoneNumber: String) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val maskedPhone = if (phoneNumber.length > 4) "User ***" + phoneNumber.takeLast(4) else "Player"
        val updated = current.copy(
            isLoggedIn = true,
            authProvider = "PHONE",
            username = maskedPhone,
            email = "$phoneNumber@phone.login",
            lastCloudSyncTime = System.currentTimeMillis()
        )
        saveProfile(updated)
    }

    suspend fun loginWithSocial(provider: String, displayName: String) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            isLoggedIn = true,
            authProvider = provider,
            username = displayName,
            lastCloudSyncTime = System.currentTimeMillis()
        )
        saveProfile(updated)
    }

    suspend fun logout() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            isLoggedIn = false,
            authProvider = "GUEST",
            username = "Player",
            email = null,
            avatarUrl = null
        )
        saveProfile(updated)
    }

    suspend fun deleteAccount() {
        val fresh = UserProfile(id = 1, username = "Player")
        saveProfile(fresh)
    }

    // --- Payment & Alternative Checkout Recording ---

    suspend fun recordPurchase(method: String, paypalEmail: String? = null) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            lastPaymentMethod = method,
            paypalEmail = paypalEmail ?: current.paypalEmail,
            totalPurchasesCount = current.totalPurchasesCount + 1
        )
        saveProfile(updated)
    }

    // --- Anti-Cheat & Security Controls ---

    suspend fun clearSecurityAlert() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            isCheatingDetected = false,
            lastSecurityScanTime = System.currentTimeMillis()
        )
        saveProfile(updated)
    }

    // --- Developer & QA Sandbox Helpers ---

    suspend fun unlockLevels(upToLevel: Int) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val target = upToLevel.coerceIn(1, 1000)
        val updated = current.copy(highestUnlockedLevel = target)
        saveProfile(updated)
    }

    suspend fun grantAllBoosters(count: Int = 5) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            hammerCount = current.hammerCount + count,
            shuffleCount = current.shuffleCount + count,
            colorBlastCount = current.colorBlastCount + count,
            extraMovesCount = current.extraMovesCount + count,
            magicWandCount = current.magicWandCount + count,
            cosmicBlastCount = current.cosmicBlastCount + count
        )
        saveProfile(updated)
    }

    suspend fun grantInfiniteLives(hours: Int = 2) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val currentExpiry = maxOf(current.infiniteLivesExpiryTimestamp, System.currentTimeMillis())
        val updated = current.copy(
            infiniteLivesExpiryTimestamp = currentExpiry + (hours * 3600 * 1000L),
            lives = 5
        )
        saveProfile(updated)
    }
}
