package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.auth.AdminAuthManager
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * AccountStorage provides strict account-level data isolation.
 * Different Google/Email accounts maintain completely independent coins, diamonds,
 * levels, boosters, and administrative privileges.
 */
object AccountStorage {
    private const val PREFS_NAME = "bonbon_bloom_account_profiles"
    private var prefs: SharedPreferences? = null

    // Fast in-memory cache of saved account profiles
    private val memoryCache = ConcurrentHashMap<String, UserProfile>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun normalizeKey(emailOrId: String?): String {
        return emailOrId?.trim()?.lowercase() ?: "guest"
    }

    fun saveAccountProfile(profile: UserProfile) {
        val email = profile.email
        val key = normalizeKey(email)
        memoryCache[key] = profile

        // Persist to SharedPreferences as JSON
        prefs?.let { p ->
            try {
                val json = JSONObject().apply {
                    put("username", profile.username)
                    put("email", profile.email ?: "")
                    put("authProvider", profile.authProvider)
                    put("coins", profile.coins)
                    put("diamonds", profile.diamonds)
                    put("lives", profile.lives)
                    put("highestUnlockedLevel", profile.highestUnlockedLevel)
                    put("hammerCount", profile.hammerCount)
                    put("shuffleCount", profile.shuffleCount)
                    put("colorBlastCount", profile.colorBlastCount)
                    put("extraMovesCount", profile.extraMovesCount)
                    put("magicWandCount", profile.magicWandCount)
                    put("cosmicBlastCount", profile.cosmicBlastCount)
                    put("isVipSubscribed", profile.isVipSubscribed)
                    put("vipExpiryTimestamp", profile.vipExpiryTimestamp)
                    put("infiniteLivesExpiryTimestamp", profile.infiniteLivesExpiryTimestamp)
                    put("hasRemovedAds", profile.hasRemovedAds)
                    put("isAdmin", profile.isAdmin && AdminAuthManager.isOwnerOrAdmin(profile.email))
                }
                p.edit().putString("acc_$key", json.toString()).apply()
            } catch (e: Exception) {
                android.util.Log.e("AccountStorage", "Failed to persist account profile for $key", e)
            }
        }
    }

    fun loadAccountProfile(email: String, displayName: String, authProvider: String): UserProfile {
        val key = normalizeKey(email)
        val isOwner = AdminAuthManager.isOwnerOrAdmin(email)

        // If owner account, always ensure master admin privileges
        if (isOwner) {
            val base = memoryCache[key] ?: loadFromPrefs(key) ?: UserProfile()
            val admin = AdminAuthManager.buildAdminProfile(base).copy(
                username = displayName.ifBlank { "Omtesh (Admin)" },
                email = email,
                authProvider = authProvider,
                isLoggedIn = true,
                isAdmin = true
            )
            saveAccountProfile(admin)
            return admin
        }

        // For non-owner accounts: STRICTLY isAdmin = false
        // 1. Check memory cache
        memoryCache[key]?.let { cached ->
            val sanitized = cached.copy(
                username = displayName.ifBlank { cached.username },
                email = email,
                authProvider = authProvider,
                isLoggedIn = true,
                isAdmin = false
            )
            return sanitized
        }

        // 2. Check disk preferences
        loadFromPrefs(key)?.let { fromDisk ->
            val sanitized = fromDisk.copy(
                username = displayName.ifBlank { fromDisk.username },
                email = email,
                authProvider = authProvider,
                isLoggedIn = true,
                isAdmin = false
            )
            memoryCache[key] = sanitized
            return sanitized
        }

        // 3. New Account: Clean starting balance (independent data!)
        val freshPlayer = UserProfile(
            id = 1,
            username = displayName.ifBlank { email.substringBefore("@") },
            email = email,
            authProvider = authProvider,
            isLoggedIn = true,
            isAdmin = false,
            coins = 500,
            diamonds = 25,
            lives = 5,
            highestUnlockedLevel = 1,
            hammerCount = 3,
            shuffleCount = 2,
            colorBlastCount = 2,
            extraMovesCount = 2,
            magicWandCount = 1,
            cosmicBlastCount = 1,
            isVipSubscribed = false,
            vipExpiryTimestamp = 0L,
            infiniteLivesExpiryTimestamp = 0L,
            hasRemovedAds = false
        )
        saveAccountProfile(freshPlayer)
        return freshPlayer
    }

    fun createGuestProfile(): UserProfile {
        return UserProfile(
            id = 1,
            username = "Player",
            email = null,
            authProvider = "GUEST",
            isLoggedIn = false,
            isAdmin = false,
            coins = 500,
            diamonds = 25,
            lives = 5,
            highestUnlockedLevel = 1,
            hammerCount = 3,
            shuffleCount = 2,
            colorBlastCount = 2,
            extraMovesCount = 2,
            magicWandCount = 1,
            cosmicBlastCount = 1,
            isVipSubscribed = false,
            vipExpiryTimestamp = 0L,
            infiniteLivesExpiryTimestamp = 0L,
            hasRemovedAds = false
        )
    }

    fun deleteAccount(email: String?) {
        val key = normalizeKey(email)
        memoryCache.remove(key)
        prefs?.edit()?.remove("acc_$key")?.apply()
    }

    private fun loadFromPrefs(key: String): UserProfile? {
        val raw = prefs?.getString("acc_$key", null) ?: return null
        return try {
            val json = JSONObject(raw)
            UserProfile(
                id = 1,
                username = json.optString("username", "Player"),
                email = json.optString("email", "").takeIf { it.isNotBlank() },
                authProvider = json.optString("authProvider", "GOOGLE"),
                coins = json.optInt("coins", 500),
                diamonds = json.optInt("diamonds", 25),
                lives = json.optInt("lives", 5),
                highestUnlockedLevel = json.optInt("highestUnlockedLevel", 1),
                hammerCount = json.optInt("hammerCount", 3),
                shuffleCount = json.optInt("shuffleCount", 2),
                colorBlastCount = json.optInt("colorBlastCount", 2),
                extraMovesCount = json.optInt("extraMovesCount", 2),
                magicWandCount = json.optInt("magicWandCount", 1),
                cosmicBlastCount = json.optInt("cosmicBlastCount", 1),
                isVipSubscribed = json.optBoolean("isVipSubscribed", false),
                vipExpiryTimestamp = json.optLong("vipExpiryTimestamp", 0L),
                infiniteLivesExpiryTimestamp = json.optLong("infiniteLivesExpiryTimestamp", 0L),
                hasRemovedAds = json.optBoolean("hasRemovedAds", false),
                isAdmin = json.optBoolean("isAdmin", false) && AdminAuthManager.isOwnerOrAdmin(json.optString("email", "")),
                isLoggedIn = true
            )
        } catch (e: Exception) {
            null
        }
    }
}
