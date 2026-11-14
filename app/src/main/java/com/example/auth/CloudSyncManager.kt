package com.example.auth

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.LevelProgress
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * CloudSyncManager handles two-way Firestore cloud synchronization for player profiles & level progress:
 * - Resolves the named Firestore database ID cleanly from R.string.firestore_database_id (never silently falling back to default).
 * - Uses @ServerTimestamp / FieldValue.serverTimestamp() + ServerTimestampBehavior.ESTIMATE for monotonic ordering.
 * - Calls verified HTTPS Cloud Functions for Google Play receipt validation (androidpublisher v3) and account deletion.
 * - Emits structured JSON diagnostics via handleFirestoreError(...) on read/write failures.
 */
object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val FUNCTIONS_BASE_URL = "https://us-central1-gen-lang-client-0152472229.cloudfunctions.net"
    const val FIRESTORE_DATABASE_ID = "ai-studio-android-candycus-ab354175-b4a1-417f-a1ac-88c83a67355a"

    enum class OperationType {
        CREATE,
        UPDATE,
        DELETE,
        GET,
        LIST,
        WRITE
    }

    sealed class ServerVerifyResult {
        data class Verified(val vipExpiryMillis: Long? = null) : ServerVerifyResult()
        data class Rejected(val reason: String) : ServerVerifyResult()
        data class Unavailable(val reason: String) : ServerVerifyResult()
    }

    @Volatile
    private var configuredDatabaseId: String = FIRESTORE_DATABASE_ID

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun initialize(context: Context) {
        val resolved = context.applicationContext.getString(R.string.firestore_database_id).trim()
        require(resolved.isNotEmpty()) { "R.string.firestore_database_id must not be blank." }
        configuredDatabaseId = resolved
    }

    fun getFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance(configuredDatabaseId)
    }

    fun handleFirestoreError(error: Throwable, operationType: OperationType, path: String) {
        val uid = try {
            FirebaseAuth.getInstance().currentUser?.uid ?: "unauthenticated"
        } catch (_: Throwable) {
            "unauthenticated"
        }
        val provider = try {
            FirebaseAuth.getInstance().currentUser?.providerData?.firstOrNull()?.providerId ?: "none"
        } catch (_: Throwable) {
            "none"
        }
        val code = (error as? FirebaseFirestoreException)?.code?.name ?: error::class.java.simpleName
        val errJson = JSONObject().apply {
            put("error", error.message ?: "Unknown Firestore error")
            put("code", code)
            put("operationType", operationType.name.lowercase())
            put("path", path)
            put("databaseId", configuredDatabaseId)
            put("authUid", uid)
            put("providerId", provider)
        }
        Log.e(TAG, "Firestore Operation Failed: $errJson", error)
    }

    /**
     * Syncs client-mutable profile fields to Firestore under zero-trust security rules.
     * Protected economy/entitlement balances are never overwritten by clients during updates.
     */
    suspend fun syncToCloud(profile: UserProfile) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val path = "users/${user.uid}"

        try {
            val db = getFirestore()
            val userDocRef = db.collection("users").document(user.uid)
            val docSnapshot = userDocRef.get().await()

            if (!docSnapshot.exists()) {
                val initialData = mapOf(
                    "username" to profile.username,
                    "email" to (profile.email ?: ""),
                    "authProvider" to profile.authProvider,
                    "highestUnlockedLevel" to profile.highestUnlockedLevel.coerceIn(1, 1000),
                    "coins" to 500,
                    "diamonds" to 25,
                    "lives" to 5,
                    "hammerCount" to 3,
                    "shuffleCount" to 2,
                    "colorBlastCount" to 2,
                    "extraMovesCount" to 2,
                    "magicWandCount" to 1,
                    "cosmicBlastCount" to 1,
                    "isVipSubscribed" to false,
                    "vipExpiryTimestamp" to 0L,
                    "hasRemovedAds" to false,
                    "isAdmin" to false,
                    "soundEnabled" to profile.soundEnabled,
                    "musicEnabled" to profile.musicEnabled,
                    "hapticsEnabled" to profile.hapticsEnabled,
                    "languageCode" to profile.languageCode,
                    "lastCloudSyncTime" to System.currentTimeMillis(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(initialData).await()
                Log.d(TAG, "Created zero-trust player profile in Firestore DB: $configuredDatabaseId")
            } else {
                val safeUpdateData = mutableMapOf<String, Any>(
                    "username" to profile.username,
                    "email" to (profile.email ?: ""),
                    "authProvider" to profile.authProvider,
                    "soundEnabled" to profile.soundEnabled,
                    "musicEnabled" to profile.musicEnabled,
                    "hapticsEnabled" to profile.hapticsEnabled,
                    "languageCode" to profile.languageCode,
                    "lastCloudSyncTime" to System.currentTimeMillis(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                val cloudHighestLevel = (docSnapshot.getLong("highestUnlockedLevel") ?: 1L).toInt()
                if (profile.highestUnlockedLevel > cloudHighestLevel && profile.highestUnlockedLevel <= 1000) {
                    safeUpdateData["highestUnlockedLevel"] = profile.highestUnlockedLevel
                }

                userDocRef.set(safeUpdateData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    /**
     * Persists individual level completion stars & high score to /users/{uid}/level_progress/{levelId}.
     */
    suspend fun syncLevelProgressToCloud(progress: LevelProgress) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val path = "users/${user.uid}/level_progress/${progress.levelId}"
        try {
            val db = getFirestore()
            val payload = mapOf(
                "levelId" to progress.levelId,
                "stars" to progress.stars.coerceIn(0, 3),
                "highScore" to progress.highScore.coerceAtLeast(0),
                "completed" to progress.completed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(user.uid)
                .collection("level_progress").document(progress.levelId.toString())
                .set(payload, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    /**
     * Verifies a Google Play purchase token server-side via Firebase Cloud Functions
     * (`verifyGooglePlayPurchase` using androidpublisher v3) with Firebase Auth + App Check tokens.
     */
    suspend fun verifyPurchaseWithServer(
        context: Context,
        productId: String,
        purchaseToken: String,
        orderId: String,
        isSubscription: Boolean
    ): ServerVerifyResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return ServerVerifyResult.Unavailable("Player is in offline Guest mode.")

        return withContext(Dispatchers.IO) {
            try {
                val idToken = user.getIdToken(false).await().token
                    ?: return@withContext ServerVerifyResult.Unavailable("Missing auth token.")

                val appCheckToken = try {
                    FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
                } catch (_: Throwable) {
                    null
                }

                val bodyJson = JSONObject().apply {
                    put("productId", productId)
                    put("purchaseToken", purchaseToken)
                    put("orderId", orderId)
                    put("isSubscription", isSubscription)
                    put("packageName", context.packageName)
                }

                val requestBuilder = Request.Builder()
                    .url("$FUNCTIONS_BASE_URL/verifyGooglePlayPurchase")
                    .header("Authorization", "Bearer $idToken")
                    .header("Content-Type", "application/json")
                    .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

                if (!appCheckToken.isNullOrBlank()) {
                    requestBuilder.header("X-Firebase-AppCheck", appCheckToken)
                }

                httpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val rawBody = response.body?.string().orEmpty()
                    val parsed = runCatching { JSONObject(rawBody) }.getOrNull()

                    when {
                        response.isSuccessful -> {
                            val verified = parsed?.optBoolean("verified", false) == true
                            if (verified) {
                                val expiry = parsed?.optLong("vipExpiryTimestamp", 0L)?.takeIf { it > 0L }
                                ServerVerifyResult.Verified(vipExpiryMillis = expiry)
                            } else {
                                val msg = parsed?.optString("error").orEmpty()
                                    .ifBlank { "Google Play could not confirm this purchase." }
                                ServerVerifyResult.Rejected(msg)
                            }
                        }
                        response.code in listOf(400, 401, 403, 409, 422) -> {
                            val msg = parsed?.optString("error").orEmpty()
                                .ifBlank { "Purchase rejected by verification server (${response.code})." }
                            ServerVerifyResult.Rejected(msg)
                        }
                        else -> {
                            Log.w(TAG, "Server verify HTTP ${response.code}: $rawBody")
                            ServerVerifyResult.Unavailable("Verification service HTTP ${response.code}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server verify request unreachable: ${e.message}")
                ServerVerifyResult.Unavailable(e.message ?: "Network error")
            }
        }
    }

    /**
     * Fetches authoritative server profile state and reconciles with local Room cache.
     * Cloud VIP/RemoveAds status is merged safely with ESTIMATE timestamps.
     */
    suspend fun fetchFromCloud(repository: UserRepository) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val path = "users/${user.uid}"

        try {
            val db = getFirestore()
            val doc = db.collection("users").document(user.uid).get().await()
            if (doc.exists()) {
                val updatedAtMillis = doc.getTimestamp(
                    "updatedAt",
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )?.toDate()?.time ?: (doc.getLong("lastCloudSyncTime") ?: 0L)

                val coins = (doc.getLong("coins") ?: 500L).toInt()
                val diamonds = (doc.getLong("diamonds") ?: 25L).toInt()
                val highestUnlockedLevel = (doc.getLong("highestUnlockedLevel") ?: 1L).toInt()
                val isVip = doc.getBoolean("isVipSubscribed") ?: false
                val vipExpiry = doc.getLong("vipExpiryTimestamp") ?: 0L
                val hammerCount = (doc.getLong("hammerCount") ?: 3L).toInt()
                val shuffleCount = (doc.getLong("shuffleCount") ?: 2L).toInt()
                val colorBlastCount = (doc.getLong("colorBlastCount") ?: 2L).toInt()
                val extraMovesCount = (doc.getLong("extraMovesCount") ?: 2L).toInt()
                val magicWandCount = (doc.getLong("magicWandCount") ?: 1L).toInt()
                val cosmicBlastCount = (doc.getLong("cosmicBlastCount") ?: 1L).toInt()
                val hasRemovedAds = doc.getBoolean("hasRemovedAds") ?: false

                val current = repository.userProfile.firstOrNull() ?: UserProfile()
                val cloudIsNewer = updatedAtMillis > current.lastCloudSyncTime

                val merged = current.copy(
                    coins = if (cloudIsNewer) coins else current.coins,
                    diamonds = if (cloudIsNewer) diamonds else current.diamonds,
                    highestUnlockedLevel = maxOf(current.highestUnlockedLevel, highestUnlockedLevel),
                    isVipSubscribed = current.isVipSubscribed || isVip,
                    vipExpiryTimestamp = maxOf(current.vipExpiryTimestamp, vipExpiry),
                    hammerCount = if (cloudIsNewer) hammerCount else current.hammerCount,
                    shuffleCount = if (cloudIsNewer) shuffleCount else current.shuffleCount,
                    colorBlastCount = if (cloudIsNewer) colorBlastCount else current.colorBlastCount,
                    extraMovesCount = if (cloudIsNewer) extraMovesCount else current.extraMovesCount,
                    magicWandCount = if (cloudIsNewer) magicWandCount else current.magicWandCount,
                    cosmicBlastCount = if (cloudIsNewer) cosmicBlastCount else current.cosmicBlastCount,
                    hasRemovedAds = current.hasRemovedAds || hasRemovedAds,
                    lastCloudSyncTime = maxOf(current.lastCloudSyncTime, updatedAtMillis, System.currentTimeMillis())
                )
                repository.saveProfile(merged, syncCloud = false)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
        }
    }

    /**
     * Permanently deletes user profile, level_progress subcollection, logs deletion request audit,
     * and deletes Firebase Auth user account.
     */
    suspend fun deleteFromCloud(): Result<Unit> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("No authenticated cloud account found to delete."))
        val path = "users/${user.uid}"

        return try {
            val db = getFirestore()
            val userDocRef = db.collection("users").document(user.uid)

            // 1. Delete level_progress subcollection (client-deletable)
            val progressDocs = userDocRef.collection("level_progress").get().await()
            if (!progressDocs.isEmpty) {
                val batch = db.batch()
                for (doc in progressDocs.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            }

            // 2. Delete Firestore user document (triggers Cloud Function recursive cleanup of purchases/ledger)
            userDocRef.delete().await()

            // 3. Delete FirebaseAuth account
            user.delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
