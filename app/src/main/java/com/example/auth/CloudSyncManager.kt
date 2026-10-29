package com.example.auth

import android.util.Log
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    
    suspend fun syncToCloud(profile: UserProfile) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        try {
            val db = FirebaseFirestore.getInstance()
            // Write the current profile to Firestore
            db.collection("users").document(user.uid)
                .set(profile, SetOptions.merge())
                .await()
            Log.d(TAG, "Successfully synced profile to cloud.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync profile to cloud: ${e.message}")
        }
    }

    suspend fun fetchFromCloud(repository: UserRepository) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        try {
            val db = FirebaseFirestore.getInstance()
            val document = db.collection("users").document(user.uid).get().await()
            if (document.exists()) {
                val cloudProfile = document.toObject(UserProfile::class.java)
                if (cloudProfile != null) {
                    // Update local Room database with cloud data
                    repository.saveProfile(cloudProfile)
                    Log.d(TAG, "Successfully fetched profile from cloud and updated local db.")
                }
            } else {
                Log.d(TAG, "No cloud profile exists for this user yet.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile from cloud: ${e.message}")
        }
    }
}
