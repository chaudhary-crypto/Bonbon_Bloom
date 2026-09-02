package com.example.game

import androidx.compose.ui.graphics.Color

enum class CandyType {
    Strawberry, // Red pop
    Lemon,      // Yellow drop
    Blueberry,  // Blue orb
    Grape,      // Purple gem
    Orange,     // Orange twist
    Mint,       // Mint green crystal
    RainbowBomb,// Multi-colored disco sprinkle
    Empty
}

enum class SpecialType {
    None,
    RocketRow,  // Clears horizontal row
    RocketCol,  // Clears vertical column
    Bomb,       // Wrapped candy: 3x3 blast twice
    Rainbow     // Color Bomb: destroys all candies of chosen color
}

enum class ObstacleType {
    None,
    Jelly,      // Must match on top to clear
    Ice,        // Trapped candy, break with adjacent match
    Chocolate   // Block that can be cleared with adjacent match
}

data class GridPosition(val row: Int, val col: Int)

data class Candy(
    val id: Int,
    val type: CandyType,
    val specialType: SpecialType = SpecialType.None
)

data class Cell(
    val position: GridPosition,
    val candy: Candy? = null,
    val obstacle: ObstacleType = ObstacleType.None,
    val jellyCount: Int = 0 // 0 = none, 1 = single jelly, 2 = double jelly
)

enum class GameState {
    Idle,
    Swapping,
    Matching,
    Falling,
    SpecialAction,
    GameOver,
    LevelComplete
}

sealed class LevelObjective {
    data class TargetScore(val score: Int) : LevelObjective()
    data class ClearAllJelly(val total: Int) : LevelObjective()
    data class CollectCandy(val candyType: CandyType, val count: Int) : LevelObjective()
}

data class LevelConfig(
    val id: Int,
    val worldNumber: Int,
    val worldName: String,
    val targetScore: Int,
    val maxMoves: Int,
    val objective: LevelObjective,
    val star1Score: Int,
    val star2Score: Int,
    val star3Score: Int,
    val initialJellyPositions: List<GridPosition> = emptyList(),
    val initialIcePositions: List<GridPosition> = emptyList(),
    val initialChocolatePositions: List<GridPosition> = emptyList()
)

data class ComboBadge(
    val id: Long,
    val text: String,
    val color: Color
)

data class FloatingScore(
    val id: Long,
    val text: String,
    val row: Int,
    val col: Int,
    val color: Color
)

data class Particle(
    val id: Long,
    val x: Float, // Normalized 0..1
    val y: Float, // Normalized 0..1
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float = 1f
)

enum class BoosterType {
    None,
    Hammer,     // Smash one candy/obstacle
    Shuffle,    // Scramble board
    ColorBlast, // Clear all of 1 color
    MagicSwap,  // Swap any two candies
    ExtraMoves, // +5 Moves
    MagicWand,  // High-value item: turns 3x3 into Color Bombs and wrapped blasts
    CosmicBlast // High-value item: screen-wide super rainbow shockwave
}
