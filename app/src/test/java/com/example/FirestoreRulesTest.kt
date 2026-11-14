package com.example

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirestoreRulesTest : FirestoreEmulatorTestBase() {

    private fun validInitialPayload(username: String = "Alice"): Map<String, Any> = mapOf(
        "username" to username,
        "email" to "${username.lowercase()}@test.com",
        "authProvider" to "GOOGLE",
        "highestUnlockedLevel" to 1,
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
        "soundEnabled" to true,
        "musicEnabled" to true,
        "hapticsEnabled" to true,
        "languageCode" to "en",
        "lastCloudSyncTime" to System.currentTimeMillis(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    @Test
    fun ownerCanCreateAndUpdateSafeFieldsButCannotEscalateVipOrCoins(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
        val docRef = firestore.collection("users").document(aliceUid)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            docRef.set(validInitialPayload("Alice")).await()
        }

        // Allowed update: soundEnabled & level advancement
        withTimeout(DEFAULT_TIMEOUT_MS) {
            docRef.set(
                mapOf(
                    "soundEnabled" to false,
                    "highestUnlockedLevel" to 7,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        }

        // Forbidden: grant self VIP
        assertPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                docRef.update(
                    mapOf(
                        "isVipSubscribed" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        // Forbidden: grant self coins
        assertPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                docRef.update(
                    mapOf(
                        "coins" to 999999,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    @Test
    fun clientCannotForgePurchasesOrReadAnotherPlayersProfile(): Unit = runBlocking {
        val aliceUid = signInTestUser("alice2_${UUID.randomUUID()}@test.com")
        val aliceDoc = firestore.collection("users").document(aliceUid)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceDoc.set(validInitialPayload("Alice2")).await()
        }

        // Client purchase forgery blocked
        val fakeTokenId = "fake_${UUID.randomUUID().toString().replace("-", "_")}"
        assertPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceDoc.collection("purchases").document(fakeTokenId).set(
                    mapOf(
                        "productId" to "vip_monthly",
                        "purchaseToken" to "forged",
                        "status" to "VERIFIED"
                    )
                ).await()
            }
        }

        // Cross-user read blocked
        signInTestUser("bob_${UUID.randomUUID()}@test.com")
        assertPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceDoc.get().await()
            }
        }

        // Unauthenticated read blocked
        auth.signOut()
        assertPermissionDenied {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                aliceDoc.get().await()
            }
        }
    }

    @Test
    fun ownerCanSyncValidLevelProgressAndFileDeletionRequest(): Unit = runBlocking {
        val uid = signInTestUser("progress_${UUID.randomUUID()}@test.com")
        val levelRef = firestore.collection("users").document(uid)
            .collection("level_progress").document("4")

        withTimeout(DEFAULT_TIMEOUT_MS) {
            levelRef.set(
                mapOf(
                    "levelId" to 4,
                    "stars" to 3,
                    "highScore" to 12400,
                    "completed" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        }

        val snap = withTimeout(DEFAULT_TIMEOUT_MS) { levelRef.get().await() }
        assertTrue(snap.exists())
        assertEquals(3L, snap.getLong("stars"))

        val delId = "del_${UUID.randomUUID().toString().replace("-", "_")}"
        withTimeout(DEFAULT_TIMEOUT_MS) {
            firestore.collection("users").document(uid)
                .collection("deletion_requests").document(delId)
                .set(
                    mapOf(
                        "requestId" to delId,
                        "reason" to "Unit test deletion",
                        "status" to "PENDING",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    private suspend fun assertPermissionDenied(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (t: Throwable) {
            val fsEx = t.unwrapFirestoreException()
                ?: throw AssertionError("Expected FirebaseFirestoreException, got $t", t)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx.code)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
    }
}
