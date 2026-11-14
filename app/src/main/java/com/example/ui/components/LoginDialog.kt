package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.audio.SoundManager
import com.example.auth.GoogleAuthManager
import kotlinx.coroutines.launch

@Composable
fun LoginDialog(
    currentEmail: String?,
    onDismiss: () -> Unit,
    onLoginWithGoogle: (email: String, displayName: String, photoUrl: String?, authUid: String?) -> Unit,
    onContinueAsGuest: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Connecting with Google...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerGoogleSignIn() {
        SoundManager.playButtonClick()
        errorMessage = null
        isLoading = true
        loadingMessage = "Connecting with Google..."

        coroutineScope.launch {
            val targetAct = GoogleAuthManager.findActivity(context)
            if (targetAct == null) {
                isLoading = false
                errorMessage = "Unable to open Google Sign-In because the active window is unavailable."
                return@launch
            }

            val result = GoogleAuthManager.signInWithGoogle(targetAct)
            isLoading = false
            if (result.isSuccess) {
                val firebaseUser = result.getOrNull()
                val googleEmail = firebaseUser?.email
                if (firebaseUser == null || googleEmail.isNullOrBlank()) {
                    errorMessage = "Google account did not return an email address. Please choose another Google account."
                    return@launch
                }
                val displayName = firebaseUser.displayName?.takeIf { it.isNotBlank() }
                    ?: googleEmail.substringBefore("@")
                onLoginWithGoogle(
                    googleEmail,
                    displayName,
                    firebaseUser.photoUrl?.toString(),
                    firebaseUser.uid
                )
                SoundManager.playLevelWin()
                onDismiss()
            } else {
                val err = result.exceptionOrNull()
                errorMessage = when (err) {
                    is GetCredentialCancellationException ->
                        "Google Sign-In was cancelled. Tap Continue with Google to try again or play as a local Guest."
                    is NoCredentialException ->
                        "No Google account is signed in on this device. Add a Google account in device Settings or play as a local Guest."
                    else ->
                        err?.message ?: "Google Sign-In failed. Check your internet connection and Google Play Services."
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7C4DFF).copy(alpha = 0.35f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🍬", fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Player Account",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (currentEmail.isNullOrBlank()) {
                                    "Cloud backup & cross-device sync"
                                } else {
                                    "Signed in: $currentEmail"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFB388FF)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clickable(onClick = onDismiss)
                                .testTag("login_close_button")
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFFD54F), modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = loadingMessage,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF2C164D),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sign in with Google to enable encrypted Firestore cloud saves, restore purchases across devices, and protect your VIP subscription.",
                                fontSize = 12.sp,
                                color = Color(0xFFE1BEE7),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        errorMessage?.let { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF4A1822),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = msg,
                                    color = Color(0xFFFF8A80),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                                .clickable { triggerGoogleSignIn() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🌐", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (errorMessage != null) "Retry Google Sign-In" else "Continue with Google",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F1F1F)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                            Text(
                                text = "  OR  ",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.2f))
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF7C4DFF),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("guest_play_button")
                                .clickable {
                                    SoundManager.playButtonClick()
                                    onContinueAsGuest()
                                    onDismiss()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF8E24AA), Color(0xFFE91E63))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text("⚡", fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Play as Guest (Local Only)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Guest progress stays on this device only. Cloud backup & cross-device VIP sync require Google Sign-In.",
                            fontSize = 11.sp,
                            color = Color(0xFFB388FF),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🛡️", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Credential Manager Secured • Fair Play Protected",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
