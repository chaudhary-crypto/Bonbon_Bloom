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
    val lastDailyClaimDay: Long = 0L
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
