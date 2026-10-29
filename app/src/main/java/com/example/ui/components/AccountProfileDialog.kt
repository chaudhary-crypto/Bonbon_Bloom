package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
import com.example.data.UserProfile

@Composable
fun AccountProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onOpenLoginDialog: () -> Unit,
    onOpenSecurityDialog: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B0F2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYER PROFILE",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFFD54F)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable(onClick = onDismiss)
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // User Avatar and Name Banner
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF281545),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Badge
                        Surface(
                            shape = CircleShape,
                            color = when (profile.authProvider) {
                                "GOOGLE" -> Color(0xFF4285F4)
                                "EMAIL" -> Color(0xFF7C4DFF)
                                "PHONE" -> Color(0xFF00B0FF)
                                else -> Color(0xFFFF4081)
                            },
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when (profile.authProvider) {
                                        "GOOGLE" -> "🌐"
                                        "EMAIL" -> "📧"
                                        "PHONE" -> "📱"
                                        "APPLE" -> "🍎"
                                        "FACEBOOK" -> "📘"
                                        else -> "👤"
                                    },
                                    fontSize = 24.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.username,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                if (profile.isVipActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("👑 VIP", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD54F))
                                }
                            }
                            Text(
                                text = profile.email ?: "Guest Mode (Local Only)",
                                fontSize = 12.sp,
                                color = Color(0xFFCE93D8)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Provider: ${profile.authProvider}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF81C784),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cloud Sync & Security Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF20133A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("☁️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cloud Backup:", fontSize = 13.sp, color = Color.LightGray)
                            }
                            Text(
                                text = if (profile.isLoggedIn) "Active & Synced" else "Disabled (Guest)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (profile.isLoggedIn) Color(0xFF69F0AE) else Color(0xFFFFB74D)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    SoundManager.playButtonClick()
                                    onOpenSecurityDialog()
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛡️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Anti-Cheat Shield:", fontSize = 13.sp, color = Color.LightGray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (profile.isCheatingDetected) "⚠️ Alert" else "Protected (v2.6)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.isCheatingDetected) Color(0xFFFF5252) else Color(0xFF40C4FF)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("›", fontSize = 14.sp, color = Color.White)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💳", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Payment Support:", fontSize = 13.sp, color = Color.LightGray)
                            }
                            Text(
                                text = "Google Play Billing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD54F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Actions
                if (!profile.isLoggedIn) {
                    Button(
                        onClick = {
                            SoundManager.playButtonClick()
                            onOpenLoginDialog()
                        },
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text("Sign In with Google or Email 🔑", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                SoundManager.playButtonClick()
                                onOpenLoginDialog()
                            },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text("Switch Account", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                SoundManager.playButtonClick()
                                showLogoutConfirm = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text("Log Out", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        SoundManager.playButtonClick()
                        showDeleteConfirm = true
                    }
                ) {
                    Text("Delete Account & Reset Data", fontSize = 11.sp, color = Color(0xFFFF8A80))
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Confirm Logout") },
            text = { Text("Are you sure you want to log out? Your progress is safely stored and you can sign back in anytime.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = { Text("Are you sure you want to delete your account and reset all bonbons, coins, and level progress? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
