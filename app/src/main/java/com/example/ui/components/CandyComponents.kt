package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.game.Candy
import com.example.game.CandyType
import com.example.game.ObstacleType
import com.example.game.SpecialType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CandyView(
    candy: Candy,
    isHinted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "candy_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val stripeOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stripe_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scale = if (isHinted) pulseScale else 1f

            // Hint highlight ring
            if (isHinted) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.45f),
                    radius = (w / 2f) * 1.08f,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            if (candy.type == CandyType.RainbowBomb || candy.specialType == SpecialType.Rainbow) {
                drawRainbowColorBomb(w, h, pulseScale)
            } else {
                drawBaseCandy(candy.type, w, h, scale)

                // Draw Special candy overlays (Stripes or Wrapped ribbons)
                when (candy.specialType) {
                    SpecialType.RocketRow -> drawStripes(w, h, isHorizontal = true, stripeOffset)
                    SpecialType.RocketCol -> drawStripes(w, h, isHorizontal = false, stripeOffset)
                    SpecialType.Bomb -> drawWrappedCandyEffects(w, h, pulseScale)
                    else -> {}
                }
            }
        }
    }
}

private fun DrawScope.drawBaseCandy(type: CandyType, w: Float, h: Float, scale: Float) {
    val center = Offset(w / 2f, h / 2f)
    val cw = w * scale
    val ch = h * scale
    val left = center.x - cw / 2f
    val top = center.y - ch / 2f

    when (type) {
        CandyType.Strawberry -> {
            // Heart-shaped/triangle red strawberry
            val path = Path().apply {
                moveTo(center.x, top + ch * 0.05f)
                lineTo(left + cw * 0.95f, top + ch * 0.75f)
                quadraticBezierTo(center.x, top + ch * 0.98f, left + cw * 0.05f, top + ch * 0.75f)
                close()
            }
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF416C), Color(0xFFC0002F)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.8f
            )
            drawPath(path, brush)

            // Seed speckles
            val seedColor = Color(0xFFFFEB3B).copy(alpha = 0.7f)
            drawCircle(seedColor, radius = 2.dp.toPx(), center = Offset(center.x, top + ch * 0.4f))
            drawCircle(seedColor, radius = 2.dp.toPx(), center = Offset(left + cw * 0.35f, top + ch * 0.6f))
            drawCircle(seedColor, radius = 2.dp.toPx(), center = Offset(left + cw * 0.65f, top + ch * 0.6f))
        }

        CandyType.Lemon -> {
            // Yellow teardrop / rounded drop
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF95B), Color(0xFFFFB300)),
                center = Offset(left + cw * 0.35f, top + ch * 0.3f),
                radius = cw * 0.8f
            )
            drawOval(
                brush = brush,
                topLeft = Offset(left + cw * 0.1f, top + ch * 0.12f),
                size = Size(cw * 0.8f, ch * 0.76f)
            )
        }

        CandyType.Blueberry -> {
            // Blue glossy sapphire sphere
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFF64D8CB), Color(0xFF1E88E5), Color(0xFF0D47A1)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.55f
            )
            drawCircle(brush, radius = cw * 0.42f, center = center)
        }

        CandyType.Grape -> {
            // Purple square crystal gem with rounded corners
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE040FB), Color(0xFF8E24AA), Color(0xFF4A148C)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.7f
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left + cw * 0.1f, top + ch * 0.1f),
                size = Size(cw * 0.8f, ch * 0.8f),
                cornerRadius = CornerRadius(cw * 0.3f, ch * 0.3f)
            )
        }

        CandyType.Orange -> {
            // Juicy bright orange sphere
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFD54F), Color(0xFFFF9800), Color(0xFFE65100)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.55f
            )
            drawCircle(brush, radius = cw * 0.42f, center = center)
        }

        CandyType.Mint -> {
            // Emerald green diamond crystal
            val path = Path().apply {
                moveTo(center.x, top + ch * 0.05f)
                lineTo(left + cw * 0.95f, center.y)
                lineTo(center.x, top + ch * 0.95f)
                lineTo(left + cw * 0.05f, center.y)
                close()
            }
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA7FFEB), Color(0xFF00E676), Color(0xFF00796B)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.75f
            )
            drawPath(path, brush)
        }

        CandyType.Empty, CandyType.RainbowBomb -> {}
    }

    // Universal glossy 3D shine reflection on upper-left
    if (type != CandyType.Empty && type != CandyType.RainbowBomb) {
        val shineBrush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.75f), Color.Transparent),
            center = Offset(left + cw * 0.32f, top + ch * 0.3f),
            radius = cw * 0.3f
        )
        drawCircle(shineBrush, radius = cw * 0.22f, center = Offset(left + cw * 0.32f, top + ch * 0.3f))
    }
}

/**
 * Draws animated crisp white Candy-Crush style rocket stripes!
 */
private fun DrawScope.drawStripes(w: Float, h: Float, isHorizontal: Boolean, offsetFraction: Float) {
    val stripeColor = Color.White.copy(alpha = 0.88f)
    val stripeWidth = 4.dp.toPx()

    if (isHorizontal) {
        // Horizontal stripes
        val step = h / 4f
        for (i in 0..4) {
            val y = (i * step + offsetFraction * step) % h
            drawLine(
                color = stripeColor,
                start = Offset(w * 0.15f, y),
                end = Offset(w * 0.85f, y),
                strokeWidth = stripeWidth
            )
        }
    } else {
        // Vertical stripes
        val step = w / 4f
        for (i in 0..4) {
            val x = (i * step + offsetFraction * step) % w
            drawLine(
                color = stripeColor,
                start = Offset(x, h * 0.15f),
                end = Offset(x, h * 0.85f),
                strokeWidth = stripeWidth
            )
        }
    }
}

/**
 * Draws Wrapped Candy (Bomb) wrapper ribbons and pulsating explosive corners!
 */
private fun DrawScope.drawWrappedCandyEffects(w: Float, h: Float, pulseScale: Float) {
    val ribbonColor = Color.White.copy(alpha = 0.9f)
    val stroke = 3.dp.toPx()

    // Outer wrapper twist ties
    val twistLength = w * 0.2f
    drawLine(ribbonColor, Offset(0f, 0f), Offset(twistLength, twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(w, 0f), Offset(w - twistLength, twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(0f, h), Offset(twistLength, h - twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(w, h), Offset(w - twistLength, h - twistLength), strokeWidth = stroke)

    // Glowing wrapper cross
    drawLine(ribbonColor.copy(alpha = 0.5f), Offset(w / 2f, h * 0.15f), Offset(w / 2f, h * 0.85f), strokeWidth = stroke * 1.5f)
    drawLine(ribbonColor.copy(alpha = 0.5f), Offset(w * 0.15f, h / 2f), Offset(w * 0.85f, h / 2f), strokeWidth = stroke * 1.5f)
}

/**
 * Draws the iconic Color Bomb: Dark chocolate truffle coated with colorful candy sprinkles and shimmering rainbow aura!
 */
private fun DrawScope.drawRainbowColorBomb(w: Float, h: Float, pulseScale: Float) {
    val center = Offset(w / 2f, h / 2f)
    val r = (w * 0.42f) * pulseScale

    // Glowing rainbow aura ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Magenta, Color.Red
            ),
            center = center
        ),
        radius = r * 1.15f,
        center = center,
        style = Stroke(width = 3.dp.toPx())
    )

    // Dark rich chocolate core
    val chocolateBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF211513)),
        center = Offset(w * 0.35f, h * 0.35f),
        radius = r
    )
    drawCircle(chocolateBrush, radius = r, center = center)

    // Candy sprinkle dots in a circle
    val sprinkleColors = listOf(
        Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
        Color(0xFF2979FF), Color(0xFFFF4081), Color(0xFFFF9100),
        Color(0xFF00E5FF), Color(0xFFE040FB)
    )

    for (i in sprinkleColors.indices) {
        val angle = (i * (360f / sprinkleColors.size)) * (Math.PI / 180f).toFloat()
        val sprinkleRadius = r * 0.62f
        val sx = center.x + sprinkleRadius * cos(angle)
        val sy = center.y + sprinkleRadius * sin(angle)
        drawCircle(sprinkleColors[i], radius = 3.5.dp.toPx(), center = Offset(sx, sy))
    }

    // Center sprinkle
    drawCircle(Color.White, radius = 3.dp.toPx(), center = center)

    // Shimmer reflection
    val shineBrush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
        center = Offset(w * 0.35f, h * 0.35f),
        radius = r * 0.5f
    )
    drawCircle(shineBrush, radius = r * 0.4f, center = Offset(w * 0.35f, h * 0.35f))
}

/**
 * Draws cell background with Jelly, Ice, or Chocolate
 */
@Composable
fun CellBackgroundView(
    obstacle: ObstacleType,
    jellyCount: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize().padding(1.dp)) {
        val w = size.width
        val h = size.height

        // Base tile board slot
        drawRoundRect(
            color = Color(0x35FFFFFF),
            size = Size(w, h),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Jelly background (translucent sweet berry pink/magenta)
        if (jellyCount > 0) {
            val jellyAlpha = if (jellyCount >= 2) 0.65f else 0.40f
            val jellyBrush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF1493).copy(alpha = jellyAlpha), Color(0xFFC71585).copy(alpha = jellyAlpha * 0.8f)),
                center = Offset(w / 2f, h / 2f),
                radius = w * 0.7f
            )
            drawRoundRect(
                brush = jellyBrush,
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            // Jelly gloss outline
            drawRoundRect(
                color = Color(0xFFFF69B4).copy(alpha = 0.5f),
                size = Size(w - 6.dp.toPx(), h - 6.dp.toPx()),
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Chocolate obstacle
        if (obstacle == ObstacleType.Chocolate) {
            val chocBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723))
            )
            drawRoundRect(
                brush = chocBrush,
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            // Chocolate tablet groove
            drawLine(
                color = Color(0xFF2E1C14),
                start = Offset(w / 2f, 4.dp.toPx()),
                end = Offset(w / 2f, h - 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color(0xFF2E1C14),
                start = Offset(4.dp.toPx(), h / 2f),
                end = Offset(w - 4.dp.toPx(), h / 2f),
                strokeWidth = 2.dp.toPx()
            )
        }

        // Ice obstacle overlay
        if (obstacle == ObstacleType.Ice) {
            val iceColor = Color(0x99B3E5FC)
            drawRoundRect(
                color = iceColor,
                size = Size(w - 4.dp.toPx(), h - 4.dp.toPx()),
                topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
            // Ice crack lines
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(w * 0.2f, h * 0.2f),
                end = Offset(w * 0.5f, h * 0.5f),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.7f),
                start = Offset(w * 0.5f, h * 0.5f),
                end = Offset(w * 0.8f, h * 0.3f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}
