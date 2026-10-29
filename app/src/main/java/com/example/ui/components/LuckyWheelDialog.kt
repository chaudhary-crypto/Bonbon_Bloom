package com.example.ui.components

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ads.AdManager
import com.example.audio.SoundManager
import com.example.data.UserProfile
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class WheelPrize(
    val name: String,
    val emoji: String,
    val color: Color
)

@Composable
fun LuckyWheelDialog(
    profile: UserProfile,
    activity: Activity?,
    onDismiss: () -> Unit,
    onClaimReward: (Int, (Pair<String, Int>) -> Unit) -> Unit,
    onGrantFreeSpin: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var wonRewardText by remember { mutableStateOf<String?>(null) }
    val rotationAngle = remember { Animatable(0f) }

    val prizes = remember {
        listOf(
            WheelPrize("250 Coins", "🟡", Color(0xFFFFD54F)),
            WheelPrize("10 Diamonds", "💎", Color(0xFF40C4FF)),
            WheelPrize("Hammer", "🔨", Color(0xFFFF5252)),
            WheelPrize("+2 Moves", "🔄", Color(0xFF69F0AE)),
            WheelPrize("500 Coins", "💰", Color(0xFFFFB300)),
            WheelPrize("Color Blast", "💥", Color(0xFFE040FB)),
            WheelPrize("1h Inf Lives", "♾️", Color(0xFFFF4081)),
            WheelPrize("2 Shuffles", "🔀", Color(0xFF7C4DFF))
        )
    }

    val canSpinFree = profile.freeLuckySpinsCount > 0

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211438).copy(alpha = 0.94f)),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(Color(0xFFFFD700), Color(0xFFFF4081))),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎡 LUCKY BONBON WHEEL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = if (canSpinFree) "Free Daily Spin Ready!" else "Watch Ad for Extra Spin (No real money required)",
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
                            onClick = { if (!isSpinning) onDismiss() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wheel Container with Top Pointer
                Box(
                    modifier = Modifier.size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating Wheel
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(rotationAngle.value)
                    ) {
                        val radius = size.width / 2f
                        val center = Offset(radius, radius)
                        val wedgeAngle = 360f / prizes.size

                        for (i in prizes.indices) {
                            val startAngle = i * wedgeAngle - 90f - (wedgeAngle / 2f)
                            drawArc(
                                color = prizes[i].color,
                                startAngle = startAngle,
                                sweepAngle = wedgeAngle,
                                useCenter = true,
                                size = Size(size.width, size.height),
                                topLeft = Offset.Zero
                            )

                            // Wedge border
                            drawArc(
                                color = Color.White.copy(alpha = 0.4f),
                                startAngle = startAngle,
                                sweepAngle = wedgeAngle,
                                useCenter = true,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Outer ring glow
                        drawCircle(
                            color = Color(0xFFFFD700),
                            radius = radius,
                            style = Stroke(width = 5.dp.toPx())
                        )

                        // Center hub
                        drawCircle(
                            color = Color(0xFF211438),
                            radius = 28.dp.toPx()
                        )
                        drawCircle(
                            color = Color(0xFFFF4081),
                            radius = 20.dp.toPx()
                        )
                    }

                    // Static Top Needle Pointer pointing down into the wheel
                    Canvas(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFFFD700))
                        drawPath(path, color = Color(0xFFD50000), style = Stroke(width = 2.dp.toPx()))
                    }

                    // Center Star Icon
                    Text(
                        text = "⭐",
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Result announcement
                wonRewardText?.let { reward ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFD54F).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉 Won: $reward!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Action Buttons
                val startSpin = {
                    if (!isSpinning) {
                        isSpinning = true
                        wonRewardText = null
                        SoundManager.playButtonClick()
                        coroutineScope.launch {
                            val prizeIdx = Random.nextInt(prizes.size)
                            // 8 wedges = 45 deg per wedge
                            // Target wedge center angle at top: index * 45 deg
                            val extraFullSpins = 5 + Random.nextInt(3)
                            val targetAngle = (extraFullSpins * 360f) + (prizeIdx * 45f)
                            
                            rotationAngle.animateTo(
                                targetValue = targetAngle,
                                animationSpec = tween(
                                    durationMillis = 3500,
                                    easing = FastOutSlowInEasing
                                )
                            )

                            // Grant reward
                            onClaimReward(prizeIdx) { result ->
                                SoundManager.playLevelWin()
                                wonRewardText = "${result.first} (${prizes[prizeIdx].emoji})"
                                isSpinning = false
                            }
                        }
                    }
                }

                // Drop Odds Disclosure (Play Store Gambling & Loot Box Policy Transparency)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "🎁 Transparent Drop Odds:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "250 Coins: 30% • 10 Gems: 15% • Hammer: 15% • +2 Moves: 15% • 500 Coins: 10% • Color Blast: 5% • 1h Inf Lives: 5% • 2 Shuffles: 5%",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            lineHeight = 11.sp
                        )
                    }
                }

                if (canSpinFree) {
                    Button(
                        onClick = startSpin,
                        enabled = !isSpinning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4081),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = if (isSpinning) "SPINNING... 🌀" else "FREE SPIN (${profile.freeLuckySpinsCount}) 🎲",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isSpinning) {
                                activity?.let { act ->
                                    AdManager.showRewarded(
                                        activity = act,
                                        onRewardEarned = {
                                            onGrantFreeSpin()
                                            startSpin()
                                        }
                                    )
                                } ?: run {
                                    onGrantFreeSpin()
                                    startSpin()
                                }
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C4DFF),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = if (isSpinning) "SPINNING... 🌀" else "📺 WATCH AD FOR EXTRA SPIN",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
