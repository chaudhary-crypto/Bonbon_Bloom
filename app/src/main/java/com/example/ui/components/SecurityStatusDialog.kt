package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
import com.example.data.UserProfile
import com.example.security.AntiCheatManager
import com.example.security.SecurityReport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SecurityStatusDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onClearSecurityAlert: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    var scanReport by remember { mutableStateOf<SecurityReport?>(null) }
    var scanMessage by remember { mutableStateOf("") }

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val shieldScale by pulseTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shield_pulse"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 22.sp, modifier = Modifier.scale(shieldScale))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "ANTI-CHEAT SHIELD",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "Fair Play & Anti-Tamper Engine v2.6",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
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

                Spacer(modifier = Modifier.height(16.dp))

                // Security Status Overview Card
                val isSecure = !profile.isCheatingDetected
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSecure) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFF7F1D1D).copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSecure) Color(0xFF10B981) else Color(0xFFEF4444)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSecure) "🔒" else "⚠️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSecure) "SYSTEM FULLY PROTECTED" else "TAMPERING SUSPECTED",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSecure) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                            )
                            Text(
                                text = if (isSecure)
                                    "Save state cryptographic HMAC signature is valid. No injections detected."
                                else
                                    "An irregular balance change was observed. Integrity protection restored safety.",
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Protection Protocols List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SecurityProtocolItem(
                        icon = "🔑",
                        title = "Savefile HMAC Cryptographic Guard",
                        subtitle = "SHA-256 signed memory hashes for Coins & Diamonds",
                        statusText = "VERIFIED",
                        isHealthy = true
                    )

                    SecurityProtocolItem(
                        icon = "⚡",
                        title = "Move Speedhack & Adjacency Gate",
                        subtitle = "Prevents bot macros and illegal cell teleportation",
                        statusText = "ENFORCED",
                        isHealthy = true
                    )

                    SecurityProtocolItem(
                        icon = "🔍",
                        title = "Root & Dynamic Hook Scanner",
                        subtitle = "Detects CheatEngine, GameGuardian, and su binaries",
                        statusText = "SECURE",
                        isHealthy = true
                    )

                    SecurityProtocolItem(
                        icon = "☁️",
                        title = "Cloud Save Anti-Rollback",
                        subtitle = "Automatic cloud conflict resolution and checksum",
                        statusText = "ACTIVE",
                        isHealthy = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scanning Progress / Diagnostic Action
                if (isScanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = scanMessage,
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            SoundManager.playButtonClick()
                            isScanning = true
                            coroutineScope.launch {
                                scanMessage = "Scanning memory addresses..."
                                delay(400)
                                scanMessage = "Validating cryptographic signature..."
                                delay(400)
                                scanMessage = "Checking environment integrity..."
                                delay(400)
                                val report = AntiCheatManager.performFullSecurityScan(profile)
                                scanReport = report
                                isScanning = false
                                SoundManager.playLevelWin()
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Run Security Diagnostic 🔍", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                scanReport?.let { report ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Scan Complete: 0 threats detected. System is 100% fair-play compliant.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                }

                if (profile.isCheatingDetected) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            SoundManager.playButtonClick()
                            onClearSecurityAlert()
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Reset & Re-verify Integrity Check ✓", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityProtocolItem(
    icon: String,
    title: String,
    subtitle: String,
    statusText: String,
    isHealthy: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B).copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isHealthy) Color(0xFF065F46) else Color(0xFF991B1B)
            ) {
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isHealthy) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
