package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
import com.example.data.UserProfile

@Composable
fun AdminConsoleDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSetLevel: (Int) -> Unit,
    onAddCoins: (Int) -> Unit,
    onAddDiamonds: (Int) -> Unit,
    onGrantAllBoosters: (Int) -> Unit,
    onToggleInfiniteLives: () -> Unit,
    onClearSecurityAlert: () -> Unit,
    onSyncCloud: () -> Unit,
    onLogoutAdmin: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Powers, 1: User Inspector, 2: Database & Security
    var targetLevelInput by remember { mutableStateOf(profile.highestUnlockedLevel.toString()) }
    var actionStatus by remember { mutableStateOf<String?>(null) }
    var userSearchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF100820)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFFD54F),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ADMIN CONSOLE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFD54F)
                            )
                            Text(
                                text = "Logged in: omtesh (Master Admin)",
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.clickable(onClick = onDismiss)
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E1035),
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFFFFD54F),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Powers", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Inspect Users", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Database", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                actionStatus?.let { status ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = status,
                            color = Color(0xFFB9F6CA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // --- TAB 0: SUPER POWERS ---
                        AdminSectionTitle("🗺️ LEVEL & WORLD CONTROL")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = targetLevelInput,
                                onValueChange = { targetLevelInput = it.filter { char -> char.isDigit() } },
                                label = { Text("Level (1–1000)", color = Color.LightGray) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFFD54F),
                                    unfocusedBorderColor = Color(0xFF7C4DFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val lvl = targetLevelInput.toIntOrNull()?.coerceIn(1, 1000) ?: 1
                                    onSetLevel(lvl)
                                    actionStatus = "✓ Jumped and unlocked up to Level $lvl"
                                    SoundManager.playLevelWin()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF)),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Jump", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onSetLevel(1000)
                                actionStatus = "✓ Unlocked all 1,000 Levels instantly!"
                                SoundManager.playLevelWin()
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
                        ) {
                            Text("⭐ Unlock All 1,000 Levels", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFFFD54F))
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AdminSectionTitle("💰 CURRENCY POWERS")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAddCoins(100000)
                                    actionStatus = "✓ Added +100,000 Coins"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                            ) {
                                Text("+100k 🟡", color = Color(0xFF3E2723), fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    onAddDiamonds(10000)
                                    actionStatus = "✓ Added +10,000 Diamonds"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                            ) {
                                Text("+10k 💎", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        AdminSectionTitle("🚀 BOOSTERS & INVENTORY")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onGrantAllBoosters(99)
                                    actionStatus = "✓ Set all Boosters to 99x"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081))
                            ) {
                                Text("99x All Boosters 🚀", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onToggleInfiniteLives()
                                    actionStatus = "✓ Granted Unlimited Lives"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                            ) {
                                Text("Infinite Lives ♾️", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1B5E20))
                            }
                        }
                    }

                    1 -> {
                        // --- TAB 1: USER & PLAYER INSPECTOR ---
                        AdminSectionTitle("👤 ACTIVE PLAYER INSPECTION")

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E1035),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                UserDetailRow("Username", profile.username)
                                UserDetailRow("Email", profile.email ?: "bhatiaomtesh02@gmail.com")
                                UserDetailRow("Account Type", if (profile.isAdmin) "👑 Super Administrator" else "Standard Player")
                                UserDetailRow("Auth Provider", profile.authProvider)
                                UserDetailRow("Highest Level", "Level ${profile.highestUnlockedLevel}")
                                UserDetailRow("Coin Balance", "${profile.coins} 🟡")
                                UserDetailRow("Diamond Balance", "${profile.diamonds} 💎")
                                UserDetailRow("Hearts / Lives", "${profile.lives} ❤️ (VIP: ${if (profile.hasInfiniteLives) "YES ♾️" else "NO"})")
                                UserDetailRow("Lollipop Hammers", "${profile.hammerCount} 🔨")
                                UserDetailRow("Shuffles", "${profile.shuffleCount} 🔄")
                                UserDetailRow("Color Blasts", "${profile.colorBlastCount} 🌈")
                                UserDetailRow("Magic Wands", "${profile.magicWandCount} 🪄")
                                UserDetailRow("Cosmic Blasts", "${profile.cosmicBlastCount} 🌌")
                                UserDetailRow("Anti-Cheat Status", if (profile.isCheatingDetected) "⚠️ Flagged" else "✓ Clean")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AdminSectionTitle("⚡ QUICK PLAYER GIFTING")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAddCoins(25000)
                                    actionStatus = "✓ Gifted +25,000 Coins to player account"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                            ) {
                                Text("Gift +25k 🟡", color = Color(0xFF3E2723), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onAddDiamonds(1000)
                                    actionStatus = "✓ Gifted +1,000 Diamonds to player account"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF))
                            ) {
                                Text("Gift +1k 💎", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    2 -> {
                        // --- TAB 2: DATABASE & SECURITY ---
                        AdminSectionTitle("🛡️ DATA INTEGRITY & CLOUD")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSyncCloud()
                                    actionStatus = "✓ Force Cloud Sync dispatched"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3949AB))
                            ) {
                                Text("Cloud Sync ☁️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    onClearSecurityAlert()
                                    actionStatus = "✓ Anti-Cheat flags reset and save resigned"
                                    SoundManager.playLevelWin()
                                },
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                            ) {
                                Text("Clear Alerts 🛡️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Logout Admin
                OutlinedButton(
                    onClick = {
                        onLogoutAdmin()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Exit Admin Mode / Return to Normal Player", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFFFD54F),
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
    )
}

@Composable
private fun UserDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = Color.LightGray)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
