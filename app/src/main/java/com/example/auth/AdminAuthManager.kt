package com.example.auth

import com.example.data.UserProfile
import com.example.security.AntiCheatManager
import java.security.MessageDigest

/**
 * AdminAuthManager provides secure, cryptographically verified authentication
 * for the secret administrator account.
 *
 * Security Guarantee:
 * The raw admin password is NEVER stored in plaintext in the codebase, resources,
 * or APK binaries. Authentication checks the salted SHA-256 cryptographic digest.
 */
object AdminAuthManager {
    // Secret salt to defeat rainbow tables and precomputed hash dictionaries
    private const val ADMIN_SALT = "bonbon_bloom_admin_salt_"
    
    // Salted SHA-256 hash of the administrative credentials (computed securely)
    private const val ADMIN_HASH = "40dfe921b342de87004e62c8457544ca7e74cc81307f78f0815629a3b5f925c8"
    
    // Canonical admin username
    const val ADMIN_USERNAME = "omtesh"
    const val ADMIN_EMAIL = "omtesh@bonbonbloom.game"

    fun isAdminIdentifier(input: String): Boolean {
        val clean = input.trim().lowercase()
        return clean == ADMIN_USERNAME || clean == ADMIN_EMAIL
    }

    /**
     * Verifies the administrator credentials using irreversible salted SHA-256 hashing.
     */
    fun verifyCredentials(identifier: String, passwordCandidate: String): Boolean {
        if (!isAdminIdentifier(identifier)) return false
        val computedHash = sha256(ADMIN_SALT + passwordCandidate.trim())
        return computedHash == ADMIN_HASH
    }

    /**
     * Grants the administrator master privileges:
     * - Unrestricted access to all 1,000 levels
     * - Permanent VIP Gold Pass & Infinite Lives
     * - Infinite Booster inventory (+99 of every power-up)
     * - Max coins and gems
     * - Complete anti-cheat exemption & clean security status
     */
    fun buildAdminProfile(base: UserProfile): UserProfile {
        val admin = base.copy(
            isAdmin = true,
            username = "omtesh (Admin)",
            email = ADMIN_EMAIL,
            authProvider = "ADMIN",
            isLoggedIn = true,
            isVipSubscribed = true,
            vipExpiryTimestamp = Long.MAX_VALUE,
            infiniteLivesExpiryTimestamp = Long.MAX_VALUE,
            highestUnlockedLevel = 1000,
            coins = 99999,
            diamonds = 9999,
            lives = 5,
            hammerCount = 99,
            shuffleCount = 99,
            colorBlastCount = 99,
            extraMovesCount = 99,
            magicWandCount = 99,
            cosmicBlastCount = 99,
            hasRemovedAds = true,
            isCheatingDetected = false
        )
        return AntiCheatManager.signProfile(admin)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
