package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun PiggyBankDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSmash: ((Int) -> Unit) -> Unit
) {
    var smashedAmount by remember { mutableStateOf<Int?>(null) }
    val progress = (profile.piggyBankCoins.toFloat() / profile.piggyBankCapacity.toFloat()).coerceIn(0f, 1f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF26123D).copy(alpha = 0.95f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFFFF80AB), Color(0xFFFFD54F))),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🐷 CANDY PIGGY BANK",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF80AB)
                        )
                        Text(
                            text = "+50 Coins saved every won level!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Piggy Visual Display
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFFF4081), Color(0xFFC2185B))
                            )
                        )
                        .border(3.dp, Color(0xFFFFD54F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (smashedAmount != null) "💥" else "🐷",
                        fontSize = 52.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "${profile.piggyBankCoins} / ${profile.piggyBankCapacity} Coins",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD54F)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFFFD54F),
                    trackColor = Color.White.copy(alpha = 0.2f),
                )

                Spacer(modifier = Modifier.height(16.dp))

                smashedAmount?.let { amt ->
                    Text(
                        text = "🎉 Smashed & Claimed +$amt Coins!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF69F0AE),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Button(
                    onClick = {
                        SoundManager.playButtonClick()
                        onSmash { claimed ->
                            SoundManager.playLevelWin()
                            smashedAmount = claimed
                        }
                    },
                    enabled = profile.piggyBankCoins > 0 && smashedAmount == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD54F),
                        contentColor = Color(0xFF26123D),
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    Text(
                        text = if (profile.piggyBankCoins > 0) "SMASH & CLAIM COINS 🔨" else "WIN LEVELS TO FILL PIGGY 🍭",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
