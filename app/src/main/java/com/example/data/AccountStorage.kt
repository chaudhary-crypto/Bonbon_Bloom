package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.auth.AdminAuthManager
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * AccountStorage provides strict account-level data isolation.
 * In release builds, administrative flags are never restored unless an active verified
 * Firebase owner session matches AdminAuthManager.isVerifiedFirebaseOwnerSession().
 */
object AccountStorage {
    private const val PREFS_NAME = "bonbon_bloom_account_profiles"
    private var prefs: SharedPreferences? = null

    private val memoryCache = ConcurrentHashMap<String, UserProfile>()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun normalizeKey(emailOrId: String?): String {
        return emailOrId?.trim()?.lowercase() ?: "guest"
    }

    private fun allowAdminForEmail(email: String?): Boolean {
        if (email.isNullOrBlank()) return false
        return if (BuildConfig.DEBUG) {
            AdminAuthManager.isOwnerOrAdmin(email)
        } else {
            AdminAuthManager.isVerifiedFirebaseOwnerSession() && AdminAuthManager.isOwnerOrAdmin(email)
        }
    }

    fun saveAccountProfile(profile: UserProfile) {
        val key = normalizeKey(profile.email)
        val allowAdmin = profile.isAdmin && allowAdminForEmail(profile.email)
        val sanitized = profile.copy(isAdmin = allowAdmin)
        memoryCache[key] = sanitized

        prefs?.let { p ->
            try {
                val json = JSONObject().apply {
                    put("username", sanitized.username)
                    put("email", sanitized.email ?: "")
                    put("authProvider", sanitized.authProvider)
                    put("coins", sanitized.coins)
                    put("diamonds", sanitized.diamonds)
                    put("lives", sanitized.lives)
                    put("highestUnlockedLevel", sanitized.highestUnlockedLevel)
                    put("hammerCount", sanitized.hammerCount)
                    put("shuffleCount", sanitized.shuffleCount)
                    put("colorBlastCount", sanitized.colorBlastCount)
                    put("extraMovesCount", sanitized.extraMovesCount)
                    put("magicWandCount", sanitized.magicWandCount)
                    put("cosmicBlastCount", sanitized.cosmicBlastCount)
                    put("isVipSubscribed", sanitized.isVipSubscribed)
                    put("vipExpiryTimestamp", sanitized.vipExpiryTimestamp)
                    put("infiniteLivesExpiryTimestamp", sanitized.infiniteLivesExpiryTimestamp)
                    put("hasRemovedAds", sanitized.hasRemovedAds)
                    put("isAdmin", allowAdmin)
                }
                p.edit().putString("acc_$key", json.toString()).apply()
            } catch (e: Exception) {
                android.util.Log.e("AccountStorage", "Failed to persist account profile for $key", e)
            }
        }
    }

    fun loadAccountProfile(email: String, displayName: String, authProvider: String): UserProfile {
        val key = normalizeKey(email)
        val isVerifiedOwner = allowAdminForEmail(email)

        if (isVerifiedOwner) {
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

        memoryCache[key]?.let { cached ->
            return cached.copy(
                username = displayName.ifBlank { cached.username },
                email = email,
                authProvider = authProvider,
                isLoggedIn = true,
                isAdmin = false
            )
        }

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

    fun getSavedAccounts(): List<UserProfile> {
        val result = mutableListOf<UserProfile>()
        prefs?.all?.forEach { (k, v) ->
            if (k.startsWith("acc_") && v is String) {
                try {
                    val rawKey = k.removePrefix("acc_")
                    if (rawKey != "guest") {
                        loadFromPrefs(rawKey)?.let { result.add(it) }
                    }
                } catch (_: Exception) {}
            }
        }
        return result.distinctBy { it.email ?: it.username }
    }

    private fun loadFromPrefs(key: String): UserProfile? {
        val raw = prefs?.getString("acc_$key", null) ?: return null
        return try {
            val json = JSONObject(raw)
            val savedEmail = json.optString("email", "").takeIf { it.isNotBlank() }
            UserProfile(
                id = 1,
                username = json.optString("username", "Player"),
                email = savedEmail,
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
                isAdmin = json.optBoolean("isAdmin", false) && allowAdminForEmail(savedEmail),
                isLoggedIn = true
            )
        } catch (_: Exception) {
            null
        }
    }
}
