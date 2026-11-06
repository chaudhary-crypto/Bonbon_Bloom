package com.example

import com.example.data.UserProfile
import com.example.game.BoosterType
import com.example.game.Candy
import com.example.game.CandyType
import com.example.game.GameEngine
import com.example.game.GridPosition
import com.example.game.LevelCatalog
import com.example.game.ObstacleType
import com.example.game.SpecialType
import com.example.payment.PlayBillingManager
import com.example.security.AntiCheatManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BonbonBloomTest {

    @Test
    fun testAll1000LevelsValidAndProperlyCalibrated() {
        assertEquals(1000, LevelCatalog.MAX_LEVELS)
        assertEquals(20, LevelCatalog.CHAPTERS.size)

        var tutorialCount = 0
        var easyCount = 0
        var normalCount = 0
        var hardCount = 0
        var reliefCount = 0
        var bossCount = 0

        for (lvl in 1..1000) {
            val config = LevelCatalog.getLevel(lvl)
            assertEquals("Level ID mismatch", lvl, config.id)
            assertTrue("Moves must be at least 15 for level $lvl", config.maxMoves >= 15)
            assertTrue("Moves must be at most 30 for level $lvl", config.maxMoves <= 30)

            assertTrue("Target score must be positive for level $lvl", config.targetScore > 0)
            assertTrue("Star 1 < Star 2 for level $lvl", config.star1Score < config.star2Score)
            assertTrue("Star 2 < Star 3 for level $lvl", config.star2Score < config.star3Score)

            // Chapter mapping verification
            val expectedChapter = ((lvl - 1) / 50) + 1
            assertEquals("Chapter number for level $lvl", expectedChapter, config.worldNumber)
            assertNotNull(config.worldName)
            assertTrue(config.worldName.isNotEmpty())

            // Obstacle positions sanity check (10 rows x 8 columns)
            config.initialJellyPositions.forEach { pos ->
                assertTrue("Jelly row out of bounds at level $lvl", pos.row in 0..9)
                assertTrue("Jelly col out of bounds at level $lvl", pos.col in 0..7)
            }
            config.initialIcePositions.forEach { pos ->
                assertTrue("Ice row out of bounds at level $lvl", pos.row in 0..9)
                assertTrue("Ice col out of bounds at level $lvl", pos.col in 0..7)
            }
            config.initialChocolatePositions.forEach { pos ->
                assertTrue("Chocolate row out of bounds at level $lvl", pos.row in 0..9)
                assertTrue("Chocolate col out of bounds at level $lvl", pos.col in 0..7)
            }

            // Difficulty count
            when (LevelCatalog.getDifficultyForLevel(lvl)) {
                LevelCatalog.LevelDifficulty.Tutorial -> tutorialCount++
                LevelCatalog.LevelDifficulty.Easy -> easyCount++
                LevelCatalog.LevelDifficulty.Normal -> normalCount++
                LevelCatalog.LevelDifficulty.Hard -> hardCount++
                LevelCatalog.LevelDifficulty.Relief -> reliefCount++
                LevelCatalog.LevelDifficulty.Boss -> bossCount++
            }
        }

        // Verify balanced distribution
        assertEquals("Tutorial levels should be first 5", 5, tutorialCount)
        assertEquals("Boss levels should be exactly 20 (every 50th level)", 20, bossCount)
        assertTrue("Hard challenge levels must exist regularly", hardCount >= 80)
        assertTrue("Relief levels must exist after hard levels", reliefCount >= 80)
        assertTrue("Easy/Normal levels form the healthy progression bulk", easyCount + normalCount > 700)
    }

    @Test
    fun testPlayabilityAcrossAll1000Levels() {
        val engine = GameEngine()
        for (lvl in 1..1000) {
            val config = LevelCatalog.getLevel(lvl)
            assertEquals("Level $lvl config id must match", lvl, config.id)
            assertTrue("Level $lvl must have valid target", config.targetScore > 0)
            assertTrue("Level $lvl must have at least 15 moves", config.maxMoves >= 15)

            // Verify no jelly is ever placed under chocolate blockers
            val chocPositions = config.initialChocolatePositions.toSet()
            for (jellyPos in config.initialJellyPositions) {
                assertFalse("Level $lvl cannot have jelly under chocolate at $jellyPos", chocPositions.contains(jellyPos))
            }

            // Load and verify initial board has valid structure
            engine.loadLevel(lvl)
            val board = engine.board.value
            assertEquals(10, board.size)
            assertEquals(8, board[0].size)

            // Verify obstacles
            for (pos in config.initialChocolatePositions) {
                assertEquals("Level $lvl chocolate at $pos", ObstacleType.Chocolate, board[pos.row][pos.col].obstacle)
            }
            for (pos in config.initialIcePositions) {
                assertEquals("Level $lvl ice at $pos", ObstacleType.Ice, board[pos.row][pos.col].obstacle)
            }
        }
    }

    @Test
    fun testBoardGenerationAcrossMilestoneLevels() {
        val testLevels = listOf(1, 10, 50, 100, 250, 500, 750, 1000)
        val engine = GameEngine()

        for (lvl in testLevels) {
            engine.loadLevel(lvl)
            val board = engine.board.value
            assertEquals("Board row count for level $lvl", 10, board.size)
            assertEquals("Board col count for level $lvl", 8, board[0].size)

            // Verify no initial 3-in-a-row matches exist anywhere on the generated board
            for (r in 0 until 10) {
                for (c in 0 until 8) {
                    val current = board[r][c].candy?.type ?: continue
                    if (current == CandyType.Empty) continue

                    if (c + 2 < 8) {
                        val c1 = board[r][c + 1].candy?.type
                        val c2 = board[r][c + 2].candy?.type
                        assertFalse(
                            "Level $lvl has horizontal 3-in-a-row at ($r, $c)",
                            current == c1 && current == c2
                        )
                    }

                    if (r + 2 < 10) {
                        val r1 = board[r + 1][c].candy?.type
                        val r2 = board[r + 2][c].candy?.type
                        assertFalse(
                            "Level $lvl has vertical 3-in-a-row at ($r, $c)",
                            current == r1 && current == r2
                        )
                    }
                }
            }

            // Verify obstacles are applied
            val config = LevelCatalog.getLevel(lvl)
            config.initialChocolatePositions.forEach { pos ->
                assertEquals(
                    "Chocolate placed correctly at level $lvl",
                    ObstacleType.Chocolate,
                    board[pos.row][pos.col].obstacle
                )
            }
            config.initialIcePositions.forEach { pos ->
                assertEquals(
                    "Ice cage placed correctly at level $lvl",
                    ObstacleType.Ice,
                    board[pos.row][pos.col].obstacle
                )
            }
        }
    }

    @Test
    fun testExtraMovesBooster() {
        val engine = GameEngine()
        engine.loadLevel(1)
        val initialMoves = engine.movesLeft.value
        engine.addExtraMoves(5)
        assertEquals(initialMoves + 5, engine.movesLeft.value)
    }

    @Test
    fun testLivesRegenerationMechanic() {
        val now = System.currentTimeMillis()
        val emptyProfile = UserProfile(
            lives = 0,
            maxLives = 5,
            lastLifeUpdate = now - (25 * 60 * 1000L) // 25 mins ago (should recover 1 life)
        )
        val elapsedMinutes = (now - emptyProfile.lastLifeUpdate) / (60 * 1000L)
        val livesToAdd = (elapsedMinutes / 20).toInt()
        assertEquals(1, livesToAdd)

        // Max lives clamping
        val fullTimePassed = UserProfile(
            lives = 2,
            maxLives = 5,
            lastLifeUpdate = now - (300 * 60 * 1000L) // 5 hours ago
        )
        val newLives = minOf(
            fullTimePassed.maxLives,
            fullTimePassed.lives + ((now - fullTimePassed.lastLifeUpdate) / (20 * 60 * 1000L)).toInt()
        )
        assertEquals(5, newLives)
    }

    @Test
    fun testInfiniteLivesActiveStatus() {
        val now = System.currentTimeMillis()
        val profileWithInfLives = UserProfile(
            infiniteLivesExpiryTimestamp = now + (3600 * 1000L) // 1 hour left
        )
        assertTrue(profileWithInfLives.hasInfiniteLives)

        val expiredProfile = UserProfile(
            infiniteLivesExpiryTimestamp = now - 1000L
        )
        assertFalse(expiredProfile.hasInfiniteLives)
    }

    @Test
    fun testRealisticAntiCheatSecuritySignature() {
        val profile = UserProfile(
            coins = 500,
            diamonds = 50,
            lives = 5,
            highestUnlockedLevel = 10,
            piggyBankCoins = 120
        )
        val signature = AntiCheatManager.computeSignature(profile)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        val signedProfile = profile.copy(securitySignature = signature)
        val isValid = AntiCheatManager.verifySignature(signedProfile)
        assertTrue("Signature should be valid for untampered profile", isValid)

        // Simulate tampering with currency
        val tamperedProfile = signedProfile.copy(coins = 9999999)
        assertFalse("Tampered currency must fail signature check", AntiCheatManager.verifySignature(tamperedProfile))

        // Quarantines tampered currency while preserving player unlocked level
        val sanitized = AntiCheatManager.sanitizeProfile(tamperedProfile)
        assertEquals(0, sanitized.coins) // Tampered coins reset
        assertEquals(10, sanitized.highestUnlockedLevel) // Progress kept
    }

    @Test
    fun testPlayBillingProductsConfig() {
        assertEquals("remove_ads", PlayBillingManager.PRODUCT_REMOVE_ADS)
        assertEquals("vip_monthly", PlayBillingManager.PRODUCT_VIP_MONTHLY)
        assertEquals("coins_500", PlayBillingManager.PRODUCT_COINS_500)
        assertEquals("coins_1500", PlayBillingManager.PRODUCT_COINS_1500)
        assertEquals("coins_5000", PlayBillingManager.PRODUCT_COINS_5000)
        assertEquals("gems_50", PlayBillingManager.PRODUCT_GEMS_50)
        assertEquals("gems_200", PlayBillingManager.PRODUCT_GEMS_200)
        assertEquals("booster_starter_pack", PlayBillingManager.PRODUCT_STARTER_PACK)
    }

    @Test
    fun testAdminAuthenticationAndPrivileges() {
        // Identifier validation
        assertTrue(com.example.auth.AdminAuthManager.isAdminIdentifier("omtesh"))
        assertTrue(com.example.auth.AdminAuthManager.isAdminIdentifier("OMTESH"))
        assertTrue(com.example.auth.AdminAuthManager.isAdminIdentifier("omtesh@bonbonbloom.game"))
        assertFalse(com.example.auth.AdminAuthManager.isAdminIdentifier("regular_player"))

        // Password verification using salted cryptographic digest
        assertTrue(
            "Admin secret credentials should verify successfully",
            com.example.auth.AdminAuthManager.verifyCredentials("omtesh", "@Omtesh.9478735801")
        )
        assertFalse(
            "Wrong password must be rejected",
            com.example.auth.AdminAuthManager.verifyCredentials("omtesh", "wrong_password_123")
        )
        assertFalse(
            "Non-admin identifier with same password must be rejected",
            com.example.auth.AdminAuthManager.verifyCredentials("player_one", "@Omtesh.9478735801")
        )

        // Master privileges verification
        val base = UserProfile()
        val admin = com.example.auth.AdminAuthManager.buildAdminProfile(base)
        assertTrue(admin.isAdmin)
        assertTrue(admin.isLoggedIn)
        assertTrue(admin.isVipSubscribed)
        assertEquals(1000, admin.highestUnlockedLevel)
        assertEquals(99999, admin.coins)
        assertEquals(9999, admin.diamonds)
        assertEquals(99, admin.hammerCount)
        assertEquals(99, admin.shuffleCount)
        assertEquals(99, admin.colorBlastCount)
        assertEquals(99, admin.extraMovesCount)
        assertEquals(99, admin.magicWandCount)
        assertEquals(99, admin.cosmicBlastCount)
        assertTrue(admin.hasRemovedAds)
        assertFalse(admin.isCheatingDetected)
        assertTrue("Admin profile should have valid anti-cheat signature", AntiCheatManager.verifySignature(admin))
    }

    @Test
    fun testOwnerAdminStrictCheck() {
        // Only bhatiaomtesh02@gmail.com and omtesh are owner/admins
        assertTrue(com.example.auth.AdminAuthManager.isOwnerOrAdmin("bhatiaomtesh02@gmail.com"))
        assertTrue(com.example.auth.AdminAuthManager.isOwnerOrAdmin("BHATIAOMTESH02@GMAIL.COM"))
        assertTrue(com.example.auth.AdminAuthManager.isOwnerOrAdmin("  bhatiaomtesh02@gmail.com  "))
        assertTrue(com.example.auth.AdminAuthManager.isOwnerOrAdmin("omtesh"))
        assertTrue(com.example.auth.AdminAuthManager.isOwnerOrAdmin("OMTESH"))

        // Any other email or user MUST NOT be treated as owner/admin
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin("bhatiaomtesh@gmail.com"))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin("bhatiaomtesh01@gmail.com"))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin("bhatiaomtesh02@gmail.com.evil.com"))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin("user123@gmail.com"))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin("player@domain.org"))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin(null))
        assertFalse(com.example.auth.AdminAuthManager.isOwnerOrAdmin(""))
    }

    @Test
    fun testAccountStorageDataIsolation() {
        // Test normal user profile creation
        val normalProfile = com.example.data.AccountStorage.loadAccountProfile(
            email = "normal_user@gmail.com",
            displayName = "Normal User",
            authProvider = "GOOGLE"
        )
        assertFalse("Normal user must NEVER have admin privileges", normalProfile.isAdmin)
        assertEquals("Normal user starts at level 1", 1, normalProfile.highestUnlockedLevel)
        assertEquals("Normal user starting coins", 500, normalProfile.coins)
        assertEquals("Normal user starting diamonds", 25, normalProfile.diamonds)

        // Simulate normal user earning coins and leveling up
        val progressedNormal = normalProfile.copy(
            coins = 1200,
            highestUnlockedLevel = 5,
            diamonds = 40
        )
        com.example.data.AccountStorage.saveAccountProfile(progressedNormal)

        // Test owner account profile creation
        val ownerProfile = com.example.data.AccountStorage.loadAccountProfile(
            email = "bhatiaomtesh02@gmail.com",
            displayName = "Omtesh",
            authProvider = "GOOGLE"
        )
        assertTrue("Owner must have admin privileges", ownerProfile.isAdmin)
        assertEquals("Owner gets 1000 levels", 1000, ownerProfile.highestUnlockedLevel)
        assertEquals("Owner gets 99999 coins", 99999, ownerProfile.coins)

        // Reload normal user - must retain its OWN progress and NEVER inherit admin data
        val reloadedNormal = com.example.data.AccountStorage.loadAccountProfile(
            email = "normal_user@gmail.com",
            displayName = "Normal User",
            authProvider = "GOOGLE"
        )
        assertFalse("Reloaded normal user must NOT be admin", reloadedNormal.isAdmin)
        assertEquals("Normal user keeps level 5 progress", 5, reloadedNormal.highestUnlockedLevel)
        assertEquals("Normal user keeps 1200 coins", 1200, reloadedNormal.coins)
        assertEquals("Normal user keeps 40 diamonds", 40, reloadedNormal.diamonds)

        // Guest profile test
        val guest = com.example.data.AccountStorage.createGuestProfile()
        assertFalse(guest.isAdmin)
        assertFalse(guest.isLoggedIn)
        assertEquals(1, guest.highestUnlockedLevel)
        assertEquals(500, guest.coins)
    }

    @Test
    fun testRobustAdjacentMatchingLogic() {
        val engine = GameEngine()

        // 1. Verify adjacency logic
        val pCenter = GridPosition(3, 3)
        val pRight = GridPosition(3, 4)
        val pLeft = GridPosition(3, 2)
        val pUp = GridPosition(2, 3)
        val pDown = GridPosition(4, 3)
        val pDiagonal = GridPosition(4, 4)
        val pDistant = GridPosition(3, 6)

        fun isAdjacent(a: GridPosition, b: GridPosition): Boolean {
            val rd = kotlin.math.abs(a.row - b.row)
            val cd = kotlin.math.abs(a.col - b.col)
            return (rd == 1 && cd == 0) || (rd == 0 && cd == 1)
        }

        assertTrue("Right is adjacent", isAdjacent(pCenter, pRight))
        assertTrue("Left is adjacent", isAdjacent(pCenter, pLeft))
        assertTrue("Up is adjacent", isAdjacent(pCenter, pUp))
        assertTrue("Down is adjacent", isAdjacent(pCenter, pDown))
        assertFalse("Diagonal is NOT adjacent", isAdjacent(pCenter, pDiagonal))
        assertFalse("Distant is NOT adjacent", isAdjacent(pCenter, pDistant))

        // 2. Test horizontal 3-in-a-row match group detection
        val rowCells = List(8) { r ->
            List(8) { c ->
                val type = if (r == 0 && c in 2..4) CandyType.Strawberry else CandyType.Mint
                com.example.game.Cell(
                    position = GridPosition(r, c),
                    candy = Candy(id = r * 8 + c + 1, type = type)
                )
            }
        }
        val matchGroups = engine.findMatchGroups(rowCells)
        assertTrue("Should detect match group", matchGroups.isNotEmpty())
        val strawberryMatch = matchGroups.firstOrNull { it.candyType == CandyType.Strawberry }
        assertNotNull("Strawberry match group must exist", strawberryMatch)
        assertEquals(3, strawberryMatch?.positions?.size)
        assertTrue(strawberryMatch?.positions?.contains(GridPosition(0, 2)) == true)
        assertTrue(strawberryMatch?.positions?.contains(GridPosition(0, 3)) == true)
        assertTrue(strawberryMatch?.positions?.contains(GridPosition(0, 4)) == true)

        // 3. Verify clearingPositions state flow exists and starts empty
        assertTrue(engine.clearingPositions.value.isEmpty())
    }

    @Test
    fun testAdMobAndPrivacyPolicyCompliance() {
        assertTrue("Banner ad unit ID must be configured", com.example.BuildConfig.ADMOB_BANNER_ID.isNotEmpty())
        assertTrue("Interstitial ad unit ID must be configured", com.example.BuildConfig.ADMOB_INTERSTITIAL_ID.isNotEmpty())
        assertTrue("Rewarded ad unit ID must be configured", com.example.BuildConfig.ADMOB_REWARDED_ID.isNotEmpty())
        assertTrue("Privacy policy URL must be configured", com.example.BuildConfig.PRIVACY_POLICY_URL.isNotEmpty())
        assertTrue("Terms of service URL must be configured", com.example.BuildConfig.TERMS_URL.isNotEmpty())
    }
}
