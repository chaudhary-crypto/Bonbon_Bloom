package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

object GoogleAuthManager {
    private const val TAG = "GoogleAuthManager"
    
    // Generates a hashed nonce for security during Google Sign In
    private fun generateHashedNonce(): Pair<String, String> {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        return Pair(rawNonce, hashedNonce)
    }

    suspend fun signInWithGoogle(context: Context): FirebaseUser? {
        val auth = FirebaseAuth.getInstance()
        val credentialManager = CredentialManager.create(context)
        
        // This web client ID is pulled from strings.xml or google-services.json
        val webClientId = context.getString(R.string.default_web_client_id)
        if (webClientId.isEmpty() || webClientId == "YOUR_WEB_CLIENT_ID") {
            Log.e(TAG, "Missing valid default_web_client_id. Please configure Firebase.")
            return null
        }

        val (rawNonce, hashedNonce) = generateHashedNonce()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(
                request = request,
                context = context,
            )

            val credential = result.credential
            if (credential is GoogleIdTokenCredential) {
                // Pass both the ID Token and the RAW nonce to Firebase
                val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                val authResult = auth.signInWithCredential(firebaseCredential).await()
                return authResult.user
            } else {
                Log.e(TAG, "Unexpected type of credential")
                return null
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User canceled sign in")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            return null
        }
    }
    
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }
}
