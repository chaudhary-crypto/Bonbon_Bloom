package com.example.auth

import android.util.Log
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

/**
 * CloudSyncManager handles two-way Firestore cloud synchronization for player profiles.
 * Backed by zero-trust Firestore security rules and Firebase Auth.
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    
    suspend fun syncToCloud(profile: UserProfile) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        try {
            val db = FirebaseFirestore.getInstance()
            val data = mapOf(
                "username" to profile.username,
                "email" to (profile.email ?: ""),
                "authProvider" to profile.authProvider,
                "highestUnlockedLevel" to profile.highestUnlockedLevel,
                "coins" to profile.coins,
                "diamonds" to profile.diamonds,
                "lives" to profile.lives,
                "isVipSubscribed" to profile.isVipSubscribed,
                "vipExpiryTimestamp" to profile.vipExpiryTimestamp,
                "infiniteLivesExpiryTimestamp" to profile.infiniteLivesExpiryTimestamp,
                "hammerCount" to profile.hammerCount,
                "shuffleCount" to profile.shuffleCount,
                "colorBlastCount" to profile.colorBlastCount,
                "extraMovesCount" to profile.extraMovesCount,
                "magicWandCount" to profile.magicWandCount,
                "cosmicBlastCount" to profile.cosmicBlastCount,
                "hasRemovedAds" to profile.hasRemovedAds,
                "lastCloudSyncTime" to System.currentTimeMillis()
            )

            db.collection("users").document(user.uid)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully synced player profile to Firestore.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile to cloud: ${e.message}")
        }
    }

    suspend fun fetchFromCloud(repository: UserRepository) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                val coins = (doc.getLong("coins") ?: 500L).toInt()
                val diamonds = (doc.getLong("diamonds") ?: 25L).toInt()
                val highestUnlockedLevel = (doc.getLong("highestUnlockedLevel") ?: 1L).toInt()
                val isVip = doc.getBoolean("isVipSubscribed") ?: false
                val hammerCount = (doc.getLong("hammerCount") ?: 3L).toInt()
                val shuffleCount = (doc.getLong("shuffleCount") ?: 2L).toInt()
                val colorBlastCount = (doc.getLong("colorBlastCount") ?: 2L).toInt()
                val extraMovesCount = (doc.getLong("extraMovesCount") ?: 2L).toInt()
                val magicWandCount = (doc.getLong("magicWandCount") ?: 1L).toInt()
                val cosmicBlastCount = (doc.getLong("cosmicBlastCount") ?: 1L).toInt()
                val hasRemovedAds = doc.getBoolean("hasRemovedAds") ?: false

                val current = repository.userProfile.firstOrNull() ?: UserProfile()
                val merged = current.copy(
                    coins = maxOf(current.coins, coins),
                    diamonds = maxOf(current.diamonds, diamonds),
                    highestUnlockedLevel = maxOf(current.highestUnlockedLevel, highestUnlockedLevel),
                    isVipSubscribed = current.isVipSubscribed || isVip,
                    hammerCount = maxOf(current.hammerCount, hammerCount),
                    shuffleCount = maxOf(current.shuffleCount, shuffleCount),
                    colorBlastCount = maxOf(current.colorBlastCount, colorBlastCount),
                    extraMovesCount = maxOf(current.extraMovesCount, extraMovesCount),
                    magicWandCount = maxOf(current.magicWandCount, magicWandCount),
                    cosmicBlastCount = maxOf(current.cosmicBlastCount, cosmicBlastCount),
                    hasRemovedAds = current.hasRemovedAds || hasRemovedAds,
                    lastCloudSyncTime = System.currentTimeMillis()
                )
                repository.saveProfile(merged)
                Log.d(TAG, "Successfully fetched and merged profile from Firestore.")
            } else {
                Log.d(TAG, "No cloud profile exists for this user yet.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile from cloud: ${e.message}")
        }
    }
}
