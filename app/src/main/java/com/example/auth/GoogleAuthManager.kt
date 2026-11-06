package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"
    const val PROD_WEB_CLIENT_ID = "6041659106-0g9ueo8m2auke1vege4pdnlfenupbb53.apps.googleusercontent.com"

    fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        val auth = FirebaseAuth.getInstance()
        val activity = findActivity(context) ?: context as? Activity
        val targetContext = activity ?: context

        // Web client ID: use string resource or fallback to verified production Web Client ID
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else PROD_WEB_CLIENT_ID
        } catch (_: Exception) {
            PROD_WEB_CLIENT_ID
        }

        val credentialManager = CredentialManager.create(targetContext)

        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = targetContext
            )

            val credential = result.credential
            val idToken: String? = when {
                credential is GoogleIdTokenCredential -> credential.idToken
                credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    try {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse GoogleIdTokenCredential from CustomCredential: ${e.message}")
                        credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                    }
                }
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                credential.type.contains("TYPE_GOOGLE_ID_TOKEN_CREDENTIAL") -> {
                    try {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } catch (_: Exception) {
                        credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                    }
                }
                else -> {
                    Log.w(TAG, "Unrecognized credential type: ${credential.type}, checking bundle")
                    credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                }
            }

            if (!idToken.isNullOrEmpty()) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                val user = authResult.user
                if (user != null) {
                    Log.i(TAG, "Google Sign-In successful: uid=${user.uid}, email=${user.email}")
                    Result.success(user)
                } else {
                    Result.failure(IllegalStateException("Firebase user was null after Google authentication."))
                }
            } else {
                Log.e(TAG, "Unable to extract ID token from credential: ${credential.type}")
                Result.failure(IllegalArgumentException("Unable to extract Google ID token: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In sheet")
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google accounts available on device: ${e.message}")
            Result.failure(e)
        } catch (e: Throwable) {
            Log.e(TAG, "Google Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Throwable) {
            Log.w(TAG, "Sign-out error: ${e.message}")
        }
    }
}
