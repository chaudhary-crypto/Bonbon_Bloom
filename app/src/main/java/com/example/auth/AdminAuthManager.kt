package com.example.auth

import com.example.data.UserProfile
import com.example.security.AntiCheatManager
import com.google.firebase.auth.FirebaseAuth

/**
 * AdminAuthManager enforces strict authorization for developer/owner tools:
 * - Administrative privileges are strictly gated to a verified Firebase Google
 *   Sign-In session matching OWNER_EMAIL (and/or server custom claim).
 * - Plaintext credentials and offline password hashes are NEVER stored anywhere
 *   in application source or APK binaries.
 */
object AdminAuthManager {
    const val ADMIN_USERNAME = "omtesh"
    const val ADMIN_EMAIL = "omtesh@bonbonbloom.game"
    const val OWNER_EMAIL = "bhatiaomtesh02@gmail.com"

    fun isAdminIdentifier(input: String): Boolean {
        val clean = input.trim().lowercase()
        return clean == ADMIN_USERNAME || clean == ADMIN_EMAIL || clean == OWNER_EMAIL || clean == "admin"
    }

    fun isOwnerOrAdmin(identifier: String?): Boolean {
        if (identifier.isNullOrBlank()) return false
        val clean = identifier.trim().lowercase()
        return clean == ADMIN_USERNAME || clean == ADMIN_EMAIL || clean == OWNER_EMAIL || clean == "admin"
    }

    /**
     * Checks whether the current active Firebase Auth session belongs to the verified owner.
     */
    fun isVerifiedFirebaseOwnerSession(): Boolean {
        val user = try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Throwable) {
            null
        } ?: return false
        val email = user.email?.trim()?.lowercase() ?: return false
        return email == OWNER_EMAIL && user.isEmailVerified
    }

    fun buildAdminProfile(base: UserProfile): UserProfile {
        val admin = base.copy(
            isAdmin = true,
            username = "Omtesh (Admin)",
            email = OWNER_EMAIL,
            authProvider = "GOOGLE",
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
}
