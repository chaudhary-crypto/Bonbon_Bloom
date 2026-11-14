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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolvedActivity = remember(activity, context) {
        activity ?: run {
            var cur: android.content.Context? = context
            while (cur is android.content.ContextWrapper) {
                if (cur is Activity) return@run cur
                cur = cur.baseContext
            }
            null
        }
    }

    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var wonRewardText by remember { mutableStateOf<String?>(null) }
    val rotationAngle = remember { Animatable(0f) }
    val isAdLoading by AdManager.isAdLoading.collectAsState()

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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211438).copy(alpha = 0.96f)),
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
                modifier = Modifier.padding(18.dp),
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
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = if (canSpinFree) "Free Daily Spin Ready!" else "Watch Ad for Extra Spin (No real money)",
                            fontSize = 11.sp,
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

                // Prominent Player Coins & Gems Balance Section
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF140B22),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFD54F)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🟡", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("YOUR COINS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F).copy(alpha = 0.8f))
                                Text("${profile.coins}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
                            }
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💎", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("YOUR GEMS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF80D8FF).copy(alpha = 0.8f))
                                Text("${profile.diamonds}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF80D8FF))
                            }
                        }
                    }
                }

                // Wheel Container with Top Pointer
                Box(
                    modifier = Modifier.size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating Wheel
                    Canvas(
                        modifier = Modifier
                            .size(215.dp)
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
                                color = Color.White.copy(alpha = 0.45f),
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

                        // Draw Emoji and Prize labels on each slice
                        drawIntoCanvas { canvas ->
                            val nativeCanvas = canvas.nativeCanvas
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 9.sp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                                setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
                            }
                            val emojiPaint = android.graphics.Paint().apply {
                                textSize = 15.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }

                            for (i in prizes.indices) {
                                val midAngle = i * wedgeAngle - 90f
                                val rad = Math.toRadians(midAngle.toDouble())
                                val dist = radius * 0.65f
                                val cx = (radius + dist * cos(rad)).toFloat()
                                val cy = (radius + dist * sin(rad)).toFloat()

                                nativeCanvas.save()
                                nativeCanvas.rotate(midAngle + 90f, cx, cy)
                                nativeCanvas.drawText(prizes[i].emoji, cx, cy - 2f, emojiPaint)
                                nativeCanvas.drawText(prizes[i].name.split(" ").firstOrNull() ?: "", cx, cy + 14f, textPaint)
                                nativeCanvas.restore()
                            }
                        }
                    }

                    // Static Top Needle Pointer pointing down into the wheel
                    Canvas(
                        modifier = Modifier
                            .size(30.dp)
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

                // Prizes Grid Summary (Coins & Items clearly visible)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🟡 250 Coins", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F))
                        Text("💰 500 Coins", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💎 10 Gems", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF40C4FF))
                        Text("🔨 Hammer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄 +2 Moves", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF69F0AE))
                        Text("💥 Blast", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE040FB))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("♾️ Inf Lives", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4081))
                        Text("🔀 2 Shuffles", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Action Buttons
                val startSpin = {
                    if (!isSpinning) {
                        isSpinning = true
                        wonRewardText = null
                        SoundManager.playButtonClick()
                        coroutineScope.launch {
                            val prizeIdx = Random.nextInt(prizes.size)
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

                // Drop Odds Disclosure
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Text(
                            text = "🎁 Transparent Drop Odds:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Text(
                            text = "250 Coins: 30% • 10 Gems: 15% • Hammer: 15% • +2 Moves: 15% • 500 Coins: 10% • Color Blast: 5% • 1h Inf Lives: 5% • 2 Shuffles: 5%",
                            fontSize = 8.5.sp,
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
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF4081),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = if (isSpinning) "SPINNING... 🌀" else "FREE SPIN (${profile.freeLuckySpinsCount}) 🎲",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isSpinning && !isAdLoading) {
                                val act = resolvedActivity ?: com.example.auth.GoogleAuthManager.findActivity(context)
                                val unavailableMsg = context.getString(com.example.R.string.bonbon_ad_unavailable)
                                if (act != null) {
                                    AdManager.showRewarded(
                                        activity = act,
                                        onRewardEarned = {
                                            onGrantFreeSpin()
                                            startSpin()
                                        },
                                        onUnavailable = {
                                            android.widget.Toast.makeText(context, unavailableMsg, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    android.widget.Toast.makeText(context, unavailableMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSpinning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C4DFF),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        if (isAdLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LOADING AD... 🎬", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Text(
                                text = if (isSpinning) "SPINNING... 🌀" else "📺 WATCH AD FOR EXTRA SPIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
