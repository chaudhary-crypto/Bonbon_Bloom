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
        initialValue = 0.94f,
        targetValue = 1.06f,
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
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stripe_offset"
    )

    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scale = if (isHinted) pulseScale else 1f

            // Hint highlight ring
            if (isHinted) {
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = 0.6f),
                    radius = (w / 2f) * 1.12f,
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            if (candy.type == CandyType.RainbowBomb || candy.specialType == SpecialType.Rainbow) {
                drawRainbowColorBomb(w, h, pulseScale, sparkleRotation)
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

/**
 * Draws ultra-juicy, high-definition 3D candy gems with specular bevels and depth.
 */
private fun DrawScope.drawBaseCandy(type: CandyType, w: Float, h: Float, scale: Float) {
    val center = Offset(w / 2f, h / 2f)
    val cw = w * scale
    val ch = h * scale
    val left = center.x - cw / 2f
    val top = center.y - ch / 2f

    // Soft candy ambient shadow underneath
    if (type != CandyType.Empty) {
        drawOval(
            color = Color(0x38000000),
            topLeft = Offset(center.x - cw * 0.38f, center.y + ch * 0.28f),
            size = Size(cw * 0.76f, ch * 0.22f)
        )
    }

    when (type) {
        CandyType.Strawberry -> {
            // Heart-shaped/strawberry red gem
            val path = Path().apply {
                moveTo(center.x, top + ch * 0.08f)
                cubicTo(left + cw * 0.98f, top + ch * 0.12f, left + cw * 0.95f, top + ch * 0.72f, center.x, top + ch * 0.95f)
                cubicTo(left + cw * 0.05f, top + ch * 0.72f, left + cw * 0.02f, top + ch * 0.12f, center.x, top + ch * 0.08f)
                close()
            }
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2A6D), Color(0xFFD80032), Color(0xFF7F001D)),
                center = Offset(left + cw * 0.35f, top + ch * 0.32f),
                radius = cw * 0.82f
            )
            drawPath(path, brush)

            // Outer bevel rim
            drawPath(
                path,
                color = Color(0x40FFFFFF),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // Sugar seed crystals
            val seedColor = Color(0xFFFFEB3B).copy(alpha = 0.85f)
            drawCircle(seedColor, radius = 2.2.dp.toPx(), center = Offset(center.x, top + ch * 0.38f))
            drawCircle(seedColor, radius = 2.2.dp.toPx(), center = Offset(left + cw * 0.32f, top + ch * 0.58f))
            drawCircle(seedColor, radius = 2.2.dp.toPx(), center = Offset(left + cw * 0.68f, top + ch * 0.58f))
            drawCircle(seedColor, radius = 1.8.dp.toPx(), center = Offset(center.x, top + ch * 0.76f))
        }

        CandyType.Lemon -> {
            // 3D Lemon drop jewel
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF95B), Color(0xFFFFC107), Color(0xFFFF8F00)),
                center = Offset(left + cw * 0.34f, top + ch * 0.30f),
                radius = cw * 0.8f
            )
            drawOval(
                brush = brush,
                topLeft = Offset(left + cw * 0.12f, top + ch * 0.14f),
                size = Size(cw * 0.76f, ch * 0.72f)
            )

            // Inner citrus facet curve
            drawArc(
                color = Color.White.copy(alpha = 0.45f),
                startAngle = 180f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(left + cw * 0.18f, top + ch * 0.20f),
                size = Size(cw * 0.64f, ch * 0.60f),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        CandyType.Blueberry -> {
            // 3D Sapphire sphere
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFF80D8FF), Color(0xFF00B0FF), Color(0xFF0D47A1)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.55f
            )
            drawCircle(brush, radius = cw * 0.43f, center = center)

            // Glassy refraction ring
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = cw * 0.39f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        CandyType.Grape -> {
            // Purple square crystal gem with faceted beveled corners
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEA80FC), Color(0xFFAA00FF), Color(0xFF4A148C)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.72f
            )
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left + cw * 0.12f, top + ch * 0.12f),
                size = Size(cw * 0.76f, ch * 0.76f),
                cornerRadius = CornerRadius(cw * 0.28f, ch * 0.28f)
            )

            // Facet edge highlights
            drawRoundRect(
                color = Color.White.copy(alpha = 0.4f),
                topLeft = Offset(left + cw * 0.18f, top + ch * 0.18f),
                size = Size(cw * 0.64f, ch * 0.64f),
                cornerRadius = CornerRadius(cw * 0.22f, ch * 0.22f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        CandyType.Orange -> {
            // Juicy bright Sunkist orange sphere
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFE082), Color(0xFFFF9800), Color(0xFFD84315)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.55f
            )
            drawCircle(brush, radius = cw * 0.43f, center = center)

            // Inner citrus pulp ring
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = cw * 0.36f,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
        }

        CandyType.Mint -> {
            // Emerald green diamond crystal
            val path = Path().apply {
                moveTo(center.x, top + ch * 0.06f)
                lineTo(left + cw * 0.94f, center.y)
                lineTo(center.x, top + ch * 0.94f)
                lineTo(left + cw * 0.06f, center.y)
                close()
            }
            val brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB9F6CA), Color(0xFF00E676), Color(0xFF004D40)),
                center = Offset(left + cw * 0.35f, top + ch * 0.35f),
                radius = cw * 0.78f
            )
            drawPath(path, brush)

            // Inner crystal facet lines
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(center.x, top + ch * 0.06f),
                end = Offset(center.x, top + ch * 0.94f),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(left + cw * 0.06f, center.y),
                end = Offset(left + cw * 0.94f, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        CandyType.Empty, CandyType.RainbowBomb -> {}
    }

    // Specular 3D shine gloss crescent on upper-left
    if (type != CandyType.Empty && type != CandyType.RainbowBomb) {
        val shineBrush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
            center = Offset(left + cw * 0.30f, top + ch * 0.28f),
            radius = cw * 0.28f
        )
        drawCircle(
            brush = shineBrush,
            radius = cw * 0.22f,
            center = Offset(left + cw * 0.30f, top + ch * 0.28f)
        )
        // Tiny extra pinpoint sparkle
        drawCircle(
            color = Color.White,
            radius = 1.8.dp.toPx(),
            center = Offset(left + cw * 0.30f, top + ch * 0.28f)
        )
    }
}

/**
 * Draws animated crisp white Candy-Crush style rocket stripes!
 */
private fun DrawScope.drawStripes(w: Float, h: Float, isHorizontal: Boolean, offsetFraction: Float) {
    val stripeColor = Color.White.copy(alpha = 0.92f)
    val stripeWidth = 4.5.dp.toPx()

    if (isHorizontal) {
        // Horizontal stripes
        val step = h / 4f
        for (i in 0..4) {
            val y = (i * step + offsetFraction * step) % h
            drawLine(
                color = stripeColor,
                start = Offset(w * 0.12f, y),
                end = Offset(w * 0.88f, y),
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
                start = Offset(x, h * 0.12f),
                end = Offset(x, h * 0.88f),
                strokeWidth = stripeWidth
            )
        }
    }
}

/**
 * Draws Wrapped Candy (Bomb) wrapper ribbons and pulsating explosive corners!
 */
private fun DrawScope.drawWrappedCandyEffects(w: Float, h: Float, pulseScale: Float) {
    val ribbonColor = Color.White.copy(alpha = 0.95f)
    val stroke = 3.5.dp.toPx()

    // Outer wrapper twist ties
    val twistLength = w * 0.24f
    drawLine(ribbonColor, Offset(0f, 0f), Offset(twistLength, twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(w, 0f), Offset(w - twistLength, twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(0f, h), Offset(twistLength, h - twistLength), strokeWidth = stroke)
    drawLine(ribbonColor, Offset(w, h), Offset(w - twistLength, h - twistLength), strokeWidth = stroke)

    // Glowing wrapper cross
    drawLine(ribbonColor.copy(alpha = 0.6f), Offset(w / 2f, h * 0.12f), Offset(w / 2f, h * 0.88f), strokeWidth = stroke * 1.5f)
    drawLine(ribbonColor.copy(alpha = 0.6f), Offset(w * 0.12f, h / 2f), Offset(w * 0.88f, h / 2f), strokeWidth = stroke * 1.5f)
}

/**
 * Draws the iconic Color Bomb: Dark chocolate truffle coated with colorful candy sprinkles and shimmering rainbow aura!
 */
private fun DrawScope.drawRainbowColorBomb(w: Float, h: Float, pulseScale: Float, rotationDeg: Float) {
    val center = Offset(w / 2f, h / 2f)
    val r = (w * 0.43f) * pulseScale

    // Glowing rotating rainbow corona ring
    drawCircle(
        brush = Brush.sweepGradient(
            colors = listOf(
                Color.Red, Color(0xFFFF9100), Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color(0xFFE040FB), Color.Red
            ),
            center = center
        ),
        radius = r * 1.16f,
        center = center,
        style = Stroke(width = 3.5.dp.toPx())
    )

    // Dark rich chocolate core
    val chocolateBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF1B0000)),
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
        val angle = ((i * (360f / sprinkleColors.size)) + rotationDeg * 0.3f) * (Math.PI / 180f).toFloat()
        val sprinkleRadius = r * 0.63f
        val sx = center.x + sprinkleRadius * cos(angle)
        val sy = center.y + sprinkleRadius * sin(angle)
        drawCircle(sprinkleColors[i], radius = 3.6.dp.toPx(), center = Offset(sx, sy))
        // Tiny highlight on each sprinkle
        drawCircle(Color.White.copy(alpha = 0.8f), radius = 1.2.dp.toPx(), center = Offset(sx - 1f, sy - 1f))
    }

    // Center sparkling star
    val starColor = Color(0xFFFFEB3B)
    drawCircle(starColor, radius = 3.5.dp.toPx(), center = center)
    drawCircle(Color.White, radius = 2.dp.toPx(), center = center)

    // Specular shine reflection
    val shineBrush = Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
        center = Offset(w * 0.32f, h * 0.32f),
        radius = r * 0.5f
    )
    drawCircle(shineBrush, radius = r * 0.38f, center = Offset(w * 0.32f, h * 0.32f))
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

        // Base tile board slot (frosted candy tile)
        drawRoundRect(
            color = Color(0x35FFFFFF),
            size = Size(w, h),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        // Jelly background (translucent sweet berry pink/magenta)
        if (jellyCount > 0) {
            val jellyAlpha = if (jellyCount >= 2) 0.68f else 0.42f
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
                color = Color.White.copy(alpha = 0.5f),
                size = Size(w - 6.dp.toPx(), h - 6.dp.toPx()),
                topLeft = Offset(3.dp.toPx(), 3.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Obstacles (Chocolate, Ice, Jelly, None)
        when (obstacle) {
            ObstacleType.Chocolate -> {
                // Rich milk chocolate square tile
                val chocoBrush = Brush.linearGradient(
                    colors = listOf(Color(0xFF5D4037), Color(0xFF3E2723)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                drawRoundRect(
                    brush = chocoBrush,
                    size = Size(w - 2.dp.toPx(), h - 2.dp.toPx()),
                    topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Chocolate bevel inner border
                drawRoundRect(
                    color = Color(0x55FFFFFF),
                    size = Size(w - 8.dp.toPx(), h - 8.dp.toPx()),
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
            ObstacleType.Ice -> {
                // Frosted crystalline ice block
                val iceBrush = Brush.linearGradient(
                    colors = listOf(Color(0xBB80D8FF), Color(0x9900B0FF)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                drawRoundRect(
                    brush = iceBrush,
                    size = Size(w, h),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Ice cracks
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.2f, h * 0.2f), Offset(w * 0.5f, h * 0.5f), strokeWidth = 1.5.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.8f, h * 0.3f), strokeWidth = 1.5.dp.toPx())
                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * 0.5f, h * 0.5f), Offset(w * 0.4f, h * 0.8f), strokeWidth = 1.5.dp.toPx())
            }
            ObstacleType.Jelly,
            ObstacleType.None -> {}
        }
    }
}
