package com.example.data

import com.example.audio.SoundManager
import com.example.auth.AdminAuthManager
import com.example.auth.CloudSyncManager
import com.example.game.BoosterType
import com.example.payment.PlayBillingManager
import com.example.security.AntiCheatManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class UserRepository(private val userDao: UserDao) {
    val userProfile: Flow<UserProfile> = userDao.getUserProfile().map { raw ->
        val profile = raw ?: UserProfile()
        val isTampered = !AntiCheatManager.verifySignature(profile) && profile.securitySignature.isNotEmpty()
        val checked = if (isTampered) {
            profile.copy(isCheatingDetected = true, cheatAttemptsCount = profile.cheatAttemptsCount + 1)
        } else {
            profile
        }
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

    /**
     * Central profile persistence: always signs the profile with AntiCheatManager before Room save
     * so legitimate mutations never drift from the cryptographic signature.
     */
    suspend fun saveProfile(profile: UserProfile, syncCloud: Boolean = true) {
        val signed = AntiCheatManager.signProfile(profile)
        userDao.saveUserProfile(signed)
        if (syncCloud) {
            try {
                CloudSyncManager.syncToCloud(signed)
            } catch (_: Exception) {
            }
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
        if (current.hasInfiniteLives) return true
        if (current.lives <= 0) return false
        val updated = current.copy(
            lives = current.lives - 1,
            lastLifeUpdate = if (current.lives == current.maxLives) System.currentTimeMillis() else current.lastLifeUpdate
        )
        saveProfile(updated, syncCloud = false)
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
        saveProfile(updated)
        return true
    }

    suspend fun refillLivesFree() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            lives = current.maxLives,
            lastLifeUpdate = System.currentTimeMillis()
        )
        saveProfile(updated, syncCloud = false)
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
        saveProfile(updated)
    }

    suspend fun completeLevel(levelId: Int, stars: Int, score: Int, coinReward: Int) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val nextLevel = (levelId + 1).coerceAtMost(1000)
        val highest = maxOf(current.highestUnlockedLevel, nextLevel)

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
        val progress = LevelProgress(
            levelId = levelId,
            stars = bestStars,
            highScore = bestScore,
            completed = true
        )
        userDao.saveLevelProgress(progress)
        try {
            CloudSyncManager.syncLevelProgressToCloud(progress)
        } catch (_: Exception) {
        }
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
        saveProfile(updated, syncCloud = false)
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
        saveProfile(updated)
        return true
    }

    suspend fun buyDiamondsPack(diamonds: Int, bonusCoins: Int = 0) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            diamonds = current.diamonds + diamonds,
            coins = current.coins + bonusCoins
        )
        saveProfile(updated)
    }

    suspend fun buyCoinsWithDiamonds(coinsAmount: Int, diamondCost: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < diamondCost) return false
        val updated = current.copy(
            diamonds = current.diamonds - diamondCost,
            coins = current.coins + coinsAmount
        )
        saveProfile(updated)
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
        saveProfile(updated)
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
        saveProfile(updated)
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
            diamonds = current.diamonds + 50,
            magicWandCount = current.magicWandCount + 2,
            cosmicBlastCount = current.cosmicBlastCount + 2,
            lives = current.maxLives
        )
        saveProfile(updated)
        return true
    }

    /**
     * Applies a verified Google Play purchase (deduplicated via indexed Room purchase_records table).
     * Never called for CANCELLED or PENDING purchases.
     */
    suspend fun applyVerifiedPlayPurchase(
        purchaseToken: String,
        orderId: String,
        productId: String,
        isSubscription: Boolean,
        verificationSource: String,
        vipExpiryOverrideMs: Long? = null
    ): Boolean {
        if (purchaseToken.isBlank()) return false
        val existing = userDao.getPurchaseByToken(purchaseToken)
        if (existing != null && existing.entitledApplied) {
            return false
        }

        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val now = System.currentTimeMillis()
        val updated: UserProfile = when (productId) {
            PlayBillingManager.PRODUCT_VIP_WEEKLY -> {
                val fallbackExpiry = maxOf(current.vipExpiryTimestamp, now) + (7L * 24 * 3600 * 1000L)
                current.copy(
                    isVipSubscribed = true,
                    vipExpiryTimestamp = maxOf(vipExpiryOverrideMs ?: fallbackExpiry, fallbackExpiry),
                    diamonds = current.diamonds + 50,
                    magicWandCount = current.magicWandCount + 2,
                    cosmicBlastCount = current.cosmicBlastCount + 2,
                    lives = current.maxLives,
                    lastPaymentMethod = "GOOGLE_PLAY",
                    totalPurchasesCount = current.totalPurchasesCount + 1
                )
            }
            PlayBillingManager.PRODUCT_VIP_MONTHLY -> {
                val fallbackExpiry = maxOf(current.vipExpiryTimestamp, now) + (30L * 24 * 3600 * 1000L)
                current.copy(
                    isVipSubscribed = true,
                    vipExpiryTimestamp = maxOf(vipExpiryOverrideMs ?: fallbackExpiry, fallbackExpiry),
                    diamonds = current.diamonds + 50,
                    magicWandCount = current.magicWandCount + 2,
                    cosmicBlastCount = current.cosmicBlastCount + 2,
                    lives = current.maxLives,
                    lastPaymentMethod = "GOOGLE_PLAY",
                    totalPurchasesCount = current.totalPurchasesCount + 1
                )
            }
            PlayBillingManager.PRODUCT_REMOVE_ADS -> current.copy(
                hasRemovedAds = true,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_COINS_500 -> current.copy(
                coins = current.coins + 500,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_COINS_1500 -> current.copy(
                coins = current.coins + 1500,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_COINS_5000 -> current.copy(
                coins = current.coins + 5000,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_50 -> current.copy(
                diamonds = current.diamonds + 50,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_150 -> current.copy(
                diamonds = current.diamonds + 150,
                coins = current.coins + 1000,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_200 -> current.copy(
                diamonds = current.diamonds + 200,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_500 -> current.copy(
                diamonds = current.diamonds + 500,
                coins = current.coins + 5000,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_600 -> current.copy(
                diamonds = current.diamonds + 600,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_GEMS_1500 -> current.copy(
                diamonds = current.diamonds + 1500,
                coins = current.coins + 20000,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            PlayBillingManager.PRODUCT_STARTER_PACK -> current.copy(
                coins = current.coins + 200,
                diamonds = current.diamonds + 20,
                hammerCount = current.hammerCount + 3,
                shuffleCount = current.shuffleCount + 3,
                colorBlastCount = current.colorBlastCount + 3,
                extraMovesCount = current.extraMovesCount + 3,
                lastPaymentMethod = "GOOGLE_PLAY",
                totalPurchasesCount = current.totalPurchasesCount + 1
            )
            else -> return false
        }

        userDao.upsertPurchaseRecord(
            PurchaseRecord(
                id = existing?.id ?: 0,
                purchaseToken = purchaseToken,
                orderId = orderId,
                productId = productId,
                isSubscription = isSubscription,
                status = if (verificationSource == "CLOUD_FUNCTION") "VERIFIED_SERVER" else "VERIFIED_LOCAL",
                entitledApplied = true,
                verificationSource = verificationSource,
                timestamp = existing?.timestamp ?: now,
                updatedAt = now
            )
        )
        saveProfile(updated)
        return true
    }

    suspend fun recordPendingPurchase(purchaseToken: String, orderId: String, productId: String, isSubscription: Boolean) {
        if (purchaseToken.isBlank()) return
        val existing = userDao.getPurchaseByToken(purchaseToken)
        if (existing?.entitledApplied == true) return
        val now = System.currentTimeMillis()
        userDao.upsertPurchaseRecord(
            PurchaseRecord(
                id = existing?.id ?: 0,
                purchaseToken = purchaseToken,
                orderId = orderId,
                productId = productId,
                isSubscription = isSubscription,
                status = "PENDING",
                entitledApplied = false,
                verificationSource = "PLAY_BILLING",
                timestamp = existing?.timestamp ?: now,
                updatedAt = now
            )
        )
    }

    /**
     * Reconciles live Google Play entitlements returned by queryPurchasesAsync (active subs & non-consumables).
     * Revokes unpaid/expired VIP or Remove-Ads flags unless the session is a verified owner/admin account.
     */
    suspend fun reconcilePlayEntitlements(
        hasActiveVipSub: Boolean,
        vipSubProductId: String?,
        hasActiveRemoveAds: Boolean
    ) {
        val current = userDao.getUserProfile().firstOrNull() ?: return
        val isVerifiedAdmin = current.isAdmin &&
            AdminAuthManager.isOwnerOrAdmin(current.email) &&
            (com.example.BuildConfig.DEBUG || AdminAuthManager.isVerifiedFirebaseOwnerSession())
        if (isVerifiedAdmin) return

        val now = System.currentTimeMillis()
        val desiredVip = hasActiveVipSub
        val desiredVipExpiry = if (hasActiveVipSub) {
            val defaultSpan = if (vipSubProductId == PlayBillingManager.PRODUCT_VIP_WEEKLY) {
                7L * 24 * 3600 * 1000L
            } else {
                30L * 24 * 3600 * 1000L
            }
            maxOf(current.vipExpiryTimestamp, now + defaultSpan)
        } else {
            // If Play Store reports no active subscription, expire local VIP flag cleanly
            if (current.isVipSubscribed && current.vipExpiryTimestamp > now) 0L else current.vipExpiryTimestamp
        }
        val desiredRemoveAds = hasActiveRemoveAds

        if (current.isVipSubscribed != desiredVip ||
            current.hasRemovedAds != desiredRemoveAds ||
            current.vipExpiryTimestamp != desiredVipExpiry
        ) {
            saveProfile(
                current.copy(
                    isVipSubscribed = desiredVip,
                    vipExpiryTimestamp = desiredVipExpiry,
                    hasRemovedAds = desiredRemoveAds
                ),
                syncCloud = false
            )
        }
    }

    suspend fun isTokenAlreadyEntitled(token: String): Boolean {
        return userDao.getPurchaseByToken(token)?.entitledApplied == true
    }

    suspend fun deductDiamonds(amount: Int): Boolean {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.diamonds < amount) return false
        val updated = current.copy(diamonds = current.diamonds - amount)
        saveProfile(updated)
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
        saveProfile(updated)
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
            0 -> current.copy(coins = current.coins + 250)
            1 -> current.copy(diamonds = current.diamonds + 10)
            2 -> current.copy(hammerCount = current.hammerCount + 1)
            3 -> current.copy(extraMovesCount = current.extraMovesCount + 2)
            4 -> current.copy(coins = current.coins + 500)
            5 -> current.copy(colorBlastCount = current.colorBlastCount + 1)
            6 -> {
                val currentExpiry = maxOf(current.infiniteLivesExpiryTimestamp, System.currentTimeMillis())
                current.copy(infiniteLivesExpiryTimestamp = currentExpiry + 3600 * 1000L)
            }
            else -> current.copy(shuffleCount = current.shuffleCount + 2)
        }.let {
            it.copy(
                lastLuckyWheelSpinDate = System.currentTimeMillis(),
                freeLuckySpinsCount = maxOf(0, it.freeLuckySpinsCount - 1)
            )
        }
        saveProfile(updated)

        return when (rewardIndex % 8) {
            0 -> "250 Coins" to 250
            1 -> "10 Diamonds" to 10
            2 -> "1 Lollipop Hammer" to 1
            3 -> "+2 Extra Moves" to 2
            4 -> "500 Coins" to 500
            5 -> "1 Color Blast" to 1
            6 -> "1 Hr Infinite Lives" to 1
            else -> "2 Shuffles" to 2
        }
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
        saveProfile(current.copy(soundEnabled = newSound))
    }

    suspend fun toggleMusic() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val newMusic = !current.musicEnabled
        SoundManager.musicEnabled = newMusic
        saveProfile(current.copy(musicEnabled = newMusic))
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

    // --- Authentication (Google Sign-In exclusively for cloud profiles) ---

    suspend fun loginWithGoogle(email: String, displayName: String, photoUrl: String? = null, authUid: String? = null) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.isLoggedIn && !current.email.isNullOrBlank()) {
            AccountStorage.saveAccountProfile(current)
        }

        val name = displayName.ifBlank { email.substringBefore("@") }
        val loaded = AccountStorage.loadAccountProfile(email, name, "GOOGLE").let {
            it.copy(
                avatarUrl = photoUrl ?: it.avatarUrl,
                authUid = authUid ?: it.authUid
            )
        }

        val merged = loaded.copy(
            highestUnlockedLevel = maxOf(loaded.highestUnlockedLevel, current.highestUnlockedLevel),
            coins = maxOf(loaded.coins, current.coins),
            diamonds = maxOf(loaded.diamonds, current.diamonds),
            hammerCount = maxOf(loaded.hammerCount, current.hammerCount),
            shuffleCount = maxOf(loaded.shuffleCount, current.shuffleCount),
            colorBlastCount = maxOf(loaded.colorBlastCount, current.colorBlastCount),
            extraMovesCount = maxOf(loaded.extraMovesCount, current.extraMovesCount),
            magicWandCount = maxOf(loaded.magicWandCount, current.magicWandCount),
            cosmicBlastCount = maxOf(loaded.cosmicBlastCount, current.cosmicBlastCount)
        )

        saveProfile(merged)
        try {
            CloudSyncManager.fetchFromCloud(this)
        } catch (_: Exception) {
        }
    }

    suspend fun loginAsAdmin() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.isLoggedIn && !current.email.isNullOrBlank()) {
            AccountStorage.saveAccountProfile(current)
        }
        val adminProfile = AdminAuthManager.buildAdminProfile(UserProfile())
        AccountStorage.saveAccountProfile(adminProfile)
        saveProfile(adminProfile)
    }

    suspend fun logout() {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.isLoggedIn && !current.email.isNullOrBlank()) {
            AccountStorage.saveAccountProfile(current)
        }
        com.example.auth.GoogleAuthManager.signOut()
        userDao.clearAllLevelProgress()
        val guest = AccountStorage.createGuestProfile()
        saveProfile(guest, syncCloud = false)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        if (current.isLoggedIn && !current.email.isNullOrBlank()) {
            val cloudResult = CloudSyncManager.deleteFromCloud()
            if (cloudResult.isFailure) {
                val error = cloudResult.exceptionOrNull() ?: Exception("Cloud deletion failed. Please check network.")
                return Result.failure(error)
            }
        }
        AccountStorage.deleteAccount(current.email)
        userDao.clearAllLevelProgress()
        userDao.clearAllPurchaseRecords()
        val fresh = AccountStorage.createGuestProfile()
        saveProfile(fresh, syncCloud = false)
        return Result.success(Unit)
    }

    suspend fun recordPurchase(method: String, paypalEmail: String? = null) {
        val current = userDao.getUserProfile().firstOrNull() ?: UserProfile()
        val updated = current.copy(
            lastPaymentMethod = method,
            paypalEmail = paypalEmail ?: current.paypalEmail,
            totalPurchasesCount = current.totalPurchasesCount + 1
        )
        saveProfile(updated)
    }

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
