package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "Player",
    val coins: Int = 500,
    val diamonds: Int = 25,
    val lives: Int = 5,
    val maxLives: Int = 5,
    val lastLifeUpdate: Long = System.currentTimeMillis(),
    val highestUnlockedLevel: Int = 1,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val hammerCount: Int = 3,
    val shuffleCount: Int = 2,
    val colorBlastCount: Int = 2,
    val extraMovesCount: Int = 2,
    val magicWandCount: Int = 1,
    val cosmicBlastCount: Int = 1,
    val isVipSubscribed: Boolean = false,
    val vipExpiryTimestamp: Long = 0L,
    val infiniteLivesExpiryTimestamp: Long = 0L,
    val lastVipDailyClaim: Long = 0L,
    val lastDailyClaimDay: Long = 0L,
    // Authentication & Account Options
    val isLoggedIn: Boolean = false,
    val authProvider: String = "GUEST", // "GUEST", "GOOGLE", "EMAIL", "PHONE", "FACEBOOK", "APPLE"
    val email: String? = null,
    val authUid: String? = null,
    val avatarUrl: String? = null,
    val lastCloudSyncTime: Long = 0L,
    // Anti-Hacking & Security Protection
    val securitySignature: String = "",
    val isCheatingDetected: Boolean = false,
    val lastSecurityScanTime: Long = 0L,
    val cheatAttemptsCount: Int = 0,
    // Payment & Alternative Checkout
    val lastPaymentMethod: String = "GOOGLE_PLAY", // "GOOGLE_PLAY", "PAYPAL", "CREDIT_CARD"
    val paypalEmail: String? = null,
    val totalPurchasesCount: Int = 0,
    // Premium Meta Features: Piggy Bank & Lucky Spin Wheel
    val piggyBankCoins: Int = 0,
    val piggyBankCapacity: Int = 1000,
    val lastLuckyWheelSpinDate: Long = 0L,
    val freeLuckySpinsCount: Int = 1,
    // Compliance, Settings, and Preferences
    val hasRemovedAds: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val languageCode: String = "en",
    val hasAskedNotificationPermission: Boolean = false
) {
    val isVipActive: Boolean
        get() = isVipSubscribed && vipExpiryTimestamp > System.currentTimeMillis()

    val hasInfiniteLives: Boolean
        get() = isVipActive || infiniteLivesExpiryTimestamp > System.currentTimeMillis()

    val infiniteLivesRemainingMinutes: Long
        get() {
            val expiry = if (isVipActive) vipExpiryTimestamp else infiniteLivesExpiryTimestamp
            val diff = expiry - System.currentTimeMillis()
            return if (diff > 0) diff / (60 * 1000) else 0L
        }
}
