package com.example.game

object LevelCatalog {
    private val WORLDS = listOf(
        1 to "Candy Garden",
        2 to "Chocolate Valley",
        3 to "Frozen Sweetlands",
        4 to "Rainbow Hills",
        5 to "Mystery Dessert Island"
    )

    fun getWorldName(levelId: Int): String {
        val worldNum = ((levelId - 1) / 20) + 1
        return WORLDS.find { it.first == worldNum }?.second ?: "Candy Garden"
    }

    fun getWorldNumber(levelId: Int): Int {
        return ((levelId - 1) / 20) + 1
    }

    fun getLevel(levelId: Int): LevelConfig {
        val id = levelId.coerceIn(1, 100)
        val worldNum = getWorldNumber(id)
        val worldName = getWorldName(id)

        // Generate tailored objectives and obstacles based on world and difficulty
        val baseMoves = when {
            id <= 5 -> 25
            id <= 15 -> 22
            id <= 30 -> 20
            id <= 60 -> 18
            else -> 16
        }

        val baseTarget = 3000 + (id * 750)
        val star1 = baseTarget
        val star2 = (baseTarget * 1.5).toInt()
        val star3 = (baseTarget * 2.2).toInt()

        // Distribute objectives
        val objective = when {
            id % 4 == 0 -> {
                // Jelly objective
                val jellyCount = (8 + (id % 12)).coerceAtMost(24)
                LevelObjective.ClearAllJelly(jellyCount)
            }
            id % 4 == 1 -> {
                // Collect Red Strawberry
                LevelObjective.CollectCandy(CandyType.Strawberry, 15 + (id % 15))
            }
            id % 4 == 2 -> {
                // Collect Blue Blueberry
                LevelObjective.CollectCandy(CandyType.Blueberry, 15 + (id % 15))
            }
            else -> {
                LevelObjective.TargetScore(baseTarget)
            }
        }

        // Layout obstacles
        val jellyList = mutableListOf<GridPosition>()
        val iceList = mutableListOf<GridPosition>()
        val chocolateList = mutableListOf<GridPosition>()

        if (objective is LevelObjective.ClearAllJelly || id > 10) {
            val count = if (objective is LevelObjective.ClearAllJelly) objective.total else 12
            // Symmetric aesthetic jelly patterns in center
            for (r in 2..5) {
                for (c in 2..5) {
                    if (jellyList.size < count) {
                        jellyList.add(GridPosition(r, c))
                    }
                }
            }
        }

        if (worldNum >= 2 && id % 3 == 0) {
            // Add chocolate blocks along borders
            chocolateList.add(GridPosition(1, 1))
            chocolateList.add(GridPosition(1, 6))
            chocolateList.add(GridPosition(6, 1))
            chocolateList.add(GridPosition(6, 6))
        }

        if (worldNum >= 3 && id % 2 == 1) {
            // Add ice blocks
            iceList.add(GridPosition(3, 3))
            iceList.add(GridPosition(3, 4))
            iceList.add(GridPosition(4, 3))
            iceList.add(GridPosition(4, 4))
        }

        return LevelConfig(
            id = id,
            worldNumber = worldNum,
            worldName = worldName,
            targetScore = baseTarget,
            maxMoves = baseMoves,
            objective = objective,
            star1Score = star1,
            star2Score = star2,
            star3Score = star3,
            initialJellyPositions = jellyList,
            initialIcePositions = iceList,
            initialChocolatePositions = chocolateList
        )
    }
}
