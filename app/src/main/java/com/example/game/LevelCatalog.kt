package com.example.game

data class ChapterInfo(
    val chapterNumber: Int,
    val name: String,
    val emoji: String,
    val startLevel: Int,
    val endLevel: Int,
    val themeDescription: String,
    val uniqueBlocker: String
)

object LevelCatalog {
    const val MAX_LEVELS = 1000
    const val LEVELS_PER_CHAPTER = 50
    const val TOTAL_CHAPTERS = 20

    val CHAPTERS: List<ChapterInfo> = listOf(
        ChapterInfo(1, "Candy Garden", "🌸", 1, 50, "Sunny floral garden with sugar blooms", "Sugar Jelly"),
        ChapterInfo(2, "Chocolate Valley", "🍫", 51, 100, "Rich cocoa rapids and fudge cliffs", "Chocolate Squares"),
        ChapterInfo(3, "Frozen Sweetlands", "❄️", 101, 150, "Crystalline sugar glaciers and ice mints", "Frosting Ice Cages"),
        ChapterInfo(4, "Rainbow Hills", "🌈", 151, 200, "Vibrant multi-colored prism peaks", "Double Sugar Frosting"),
        ChapterInfo(5, "Mystery Dessert Island", "🏝️", 201, 250, "Exotic tropical fruit confection paradise", "Spreading Honey"),
        ChapterInfo(6, "Caramel Canyon", "🍯", 251, 300, "Golden gooey ridges and butter brittle", "Sticky Caramel"),
        ChapterInfo(7, "Cotton Candy Clouds", "☁️", 301, 350, "Airy pastel puffs soaring in the sky", "Sugar Clouds"),
        ChapterInfo(8, "Gummy Bear Lagoon", "🐻", 351, 400, "Sparkling soda waters and bouncy gummies", "Licorice Swirls"),
        ChapterInfo(9, "Licorice Labyrinth", "🌀", 401, 450, "Twisted black and red licorice mazes", "Licorice Locks"),
        ChapterInfo(10, "Marshmallow Meadows", "🍡", 451, 500, "Soft pillowy plains and sugar dusting", "Marshmallow Pillows"),
        ChapterInfo(11, "Peppermint Peaks", "🍬", 501, 550, "Crisp minty slopes and candy cane spires", "Peppermint Cages"),
        ChapterInfo(12, "Bubblegum Bay", "🎈", 551, 600, "Chewy pink shores and popping bubbles", "Bubblegum Cages"),
        ChapterInfo(13, "Taffy Tundra", "🏔️", 601, 650, "Stretchy saltwater taffy snowfields", "Stretchy Taffy Cages"),
        ChapterInfo(14, "Sugar Plum Forest", "🌲", 651, 700, "Enchanted violet candy groves", "Thorned Licorice"),
        ChapterInfo(15, "Butterscotch Basin", "🍮", 701, 750, "Velvety caramel pools and wafer bridges", "Hard Wafer Blocks"),
        ChapterInfo(16, "Toffee Volcano", "🌋", 751, 800, "Molten chocolate magma and crunchy honeycomb", "Molten Chocolate"),
        ChapterInfo(17, "Jelly Bean Jungle", "🌴", 801, 850, "Densely populated vibrant sugar beans", "Dense Sugar Vines"),
        ChapterInfo(18, "Lollipop Lane", "🍭", 851, 900, "Giant swirling lollipops lighting the path", "Lollipop Barricades"),
        ChapterInfo(19, "Macaron Mirage", "🧁", 901, 950, "Delicate French pastry towers in the desert", "Macaron Shells"),
        ChapterInfo(20, "Cosmic Confectionery", "🚀", 951, 1000, "Starlight sprinkles and gravity-defying treats", "Cosmic Crystal Gates")
    )

    fun getChapterForLevel(levelId: Int): ChapterInfo {
        val clamped = levelId.coerceIn(1, MAX_LEVELS)
        val chapterIdx = ((clamped - 1) / LEVELS_PER_CHAPTER).coerceIn(0, CHAPTERS.lastIndex)
        return CHAPTERS[chapterIdx]
    }

    fun getWorldName(levelId: Int): String = getChapterForLevel(levelId).name
    fun getWorldNumber(levelId: Int): Int = getChapterForLevel(levelId).chapterNumber

    /**
     * Difficulty classification for player transparency and balanced telemetry:
     * - Easy (85-95% win rate)
     * - Normal (55-70% win rate)
     * - Hard (25-40% win rate) — every 10th level
     * - Relief (85-95% win rate) — immediately after hard level
     * - Boss (25-35% win rate) — final level of each chapter (50, 100, 150... 1000)
     */
    enum class LevelDifficulty {
        Tutorial,
        Easy,
        Normal,
        Hard,
        Relief,
        Boss
    }

    fun getDifficultyForLevel(levelId: Int): LevelDifficulty {
        return when {
            levelId <= 5 -> LevelDifficulty.Tutorial
            levelId % 50 == 0 -> LevelDifficulty.Boss
            levelId % 10 == 0 -> LevelDifficulty.Hard
            levelId % 10 == 1 && levelId > 10 -> LevelDifficulty.Relief
            levelId % 3 == 0 -> LevelDifficulty.Normal
            else -> LevelDifficulty.Easy
        }
    }

    fun getLevel(levelId: Int): LevelConfig {
        val id = levelId.coerceIn(1, MAX_LEVELS)
        val chapter = getChapterForLevel(id)
        val chapterNum = chapter.chapterNumber
        val difficulty = getDifficultyForLevel(id)

        // Balanced move curve strictly calibrated to target win-rate curves
        val maxMoves = when (difficulty) {
            LevelDifficulty.Tutorial -> 28
            LevelDifficulty.Relief -> 25
            LevelDifficulty.Easy -> 23
            LevelDifficulty.Normal -> 20
            LevelDifficulty.Hard -> 16
            LevelDifficulty.Boss -> 15
        }

        val baseTarget = when (difficulty) {
            LevelDifficulty.Tutorial -> 1800 + (id * 150)
            LevelDifficulty.Relief -> 2500 + (id * 200)
            LevelDifficulty.Easy -> 3000 + (id * 250)
            LevelDifficulty.Normal -> 3800 + (id * 320)
            LevelDifficulty.Hard -> 4600 + (id * 400)
            LevelDifficulty.Boss -> 5500 + (id * 450)
        }

        val star1 = baseTarget
        val star2 = (baseTarget * 1.45).toInt()
        val star3 = (baseTarget * 2.1).toInt()

        // Objectives cycle predictably
        val objective = when (id % 5) {
            0 -> {
                val jellyTarget = when (difficulty) {
                    LevelDifficulty.Tutorial, LevelDifficulty.Relief -> 8
                    LevelDifficulty.Easy -> 12
                    LevelDifficulty.Normal -> 16
                    LevelDifficulty.Hard, LevelDifficulty.Boss -> 20
                }
                LevelObjective.ClearAllJelly(jellyTarget)
            }
            1 -> {
                val count = when (difficulty) {
                    LevelDifficulty.Tutorial -> 10
                    LevelDifficulty.Relief, LevelDifficulty.Easy -> 16
                    LevelDifficulty.Normal -> 22
                    LevelDifficulty.Hard, LevelDifficulty.Boss -> 28
                }
                LevelObjective.CollectCandy(CandyType.Strawberry, count)
            }
            2 -> {
                val count = when (difficulty) {
                    LevelDifficulty.Tutorial -> 10
                    LevelDifficulty.Relief, LevelDifficulty.Easy -> 16
                    LevelDifficulty.Normal -> 22
                    LevelDifficulty.Hard, LevelDifficulty.Boss -> 28
                }
                LevelObjective.CollectCandy(CandyType.Blueberry, count)
            }
            3 -> {
                val count = when (difficulty) {
                    LevelDifficulty.Tutorial -> 10
                    LevelDifficulty.Relief, LevelDifficulty.Easy -> 16
                    LevelDifficulty.Normal -> 22
                    LevelDifficulty.Hard, LevelDifficulty.Boss -> 28
                }
                LevelObjective.CollectCandy(CandyType.Grape, count)
            }
            else -> LevelObjective.TargetScore(baseTarget)
        }

        // Obstacles placed according to chapter and level difficulty
        val jellyList = mutableListOf<GridPosition>()
        val iceList = mutableListOf<GridPosition>()
        val chocolateList = mutableListOf<GridPosition>()

        if (objective is LevelObjective.ClearAllJelly || (id > 5 && id % 2 == 0)) {
            val count = if (objective is LevelObjective.ClearAllJelly) objective.total else 8
            for (r in 2..5) {
                for (c in 2..5) {
                    if (jellyList.size < count) {
                        jellyList.add(GridPosition(r, c))
                    }
                }
            }
        }

        // Chocolate blockers from Chapter 2 onward
        if (chapterNum >= 2 && (difficulty == LevelDifficulty.Hard || difficulty == LevelDifficulty.Boss || id % 4 == 0)) {
            chocolateList.add(GridPosition(1, 1))
            chocolateList.add(GridPosition(1, 6))
            if (difficulty == LevelDifficulty.Boss || chapterNum >= 5) {
                chocolateList.add(GridPosition(6, 1))
                chocolateList.add(GridPosition(6, 6))
            }
        }

        // Ice cages from Chapter 3 onward
        if (chapterNum >= 3 && id > 10 && (difficulty == LevelDifficulty.Hard || difficulty == LevelDifficulty.Boss || id % 3 == 0)) {
            iceList.add(GridPosition(3, 3))
            iceList.add(GridPosition(3, 4))
            iceList.add(GridPosition(4, 3))
            iceList.add(GridPosition(4, 4))
        }

        return LevelConfig(
            id = id,
            worldNumber = chapterNum,
            worldName = chapter.name,
            targetScore = baseTarget,
            maxMoves = maxMoves,
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
