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
import com.example.MainActivity
import com.example.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"

    fun findActivity(context: Context): Activity? {
        if (context is Activity) return context
        MainActivity.currentActivity?.let { return it }
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return MainActivity.currentActivity
    }

    suspend fun signInWithGoogle(context: Context): Result<FirebaseUser> {
        val auth = FirebaseAuth.getInstance()
        val activity = findActivity(context) ?: MainActivity.currentActivity
        if (activity == null) {
            Log.e(TAG, "No valid Activity found for CredentialManager")
            return Result.failure(IllegalStateException("No Activity available for Google Sign-In"))
        }

        val webClientId = activity.getString(R.string.default_web_client_id).trim()
        if (webClientId.isEmpty()) {
            return Result.failure(IllegalStateException("Google Sign-In client ID is not configured."))
        }

        val credentialManager = CredentialManager.create(activity)
        val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            val idToken: String? = when {
                credential is GoogleIdTokenCredential -> credential.idToken
                credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                }
                else -> {
                    Log.w(TAG, "Unexpected credential type: ${credential.type}")
                    null
                }
            }

            if (idToken.isNullOrEmpty()) {
                return Result.failure(IllegalArgumentException("Unable to extract Google ID token."))
            }

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
                ?: return Result.failure(IllegalStateException("Firebase user was null after Google authentication."))

            Log.i(TAG, "Google Sign-In successful: uid=${user.uid}")
            Result.success(user)
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
