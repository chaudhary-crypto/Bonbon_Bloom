package com.example

import com.example.data.UserProfile
import com.example.game.Candy
import com.example.game.CandyType
import com.example.game.GameEngine
import com.example.game.GridPosition
import com.example.game.LevelCatalog
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
    fun testLevelCatalogHas1000LevelsAnd20Chapters() {
        assertEquals(1000, LevelCatalog.MAX_LEVELS)
        assertEquals(20, LevelCatalog.CHAPTERS.size)

        // Verify chapter continuity
        for (i in LevelCatalog.CHAPTERS.indices) {
            val chap = LevelCatalog.CHAPTERS[i]
            assertEquals(i + 1, chap.chapterNumber)
            assertEquals((i * 50) + 1, chap.startLevel)
            assertEquals((i + 1) * 50, chap.endLevel)
            assertNotNull(chap.uniqueBlocker)
        }

        // Test difficulty calibration
        val lvl1 = LevelCatalog.getLevel(1)
        assertEquals(LevelCatalog.LevelDifficulty.Tutorial, LevelCatalog.getDifficultyForLevel(1))
        assertEquals(28, lvl1.maxMoves)

        val lvl10 = LevelCatalog.getLevel(10)
        assertEquals(LevelCatalog.LevelDifficulty.Hard, LevelCatalog.getDifficultyForLevel(10))
        assertEquals(16, lvl10.maxMoves)

        val lvl11 = LevelCatalog.getLevel(11)
        assertEquals(LevelCatalog.LevelDifficulty.Relief, LevelCatalog.getDifficultyForLevel(11))
        assertEquals(25, lvl11.maxMoves)

        val lvl50 = LevelCatalog.getLevel(50)
        assertEquals(LevelCatalog.LevelDifficulty.Boss, LevelCatalog.getDifficultyForLevel(50))
        assertEquals(15, lvl50.maxMoves)
    }

    @Test
    fun testBoardGenerationNoInitialMatches() {
        val engine = GameEngine()
        engine.loadLevel(1)

        val board = engine.board.value
        assertEquals(10, board.size)
        assertEquals(8, board[0].size)

        // Verify no 3-in-a-row matches exist horizontally or vertically
        for (r in 0 until 10) {
            for (c in 0 until 8) {
                val current = board[r][c].candy?.type ?: continue
                if (current == CandyType.Empty) continue

                // Check horizontal 3-in-a-row
                if (c + 2 < 8) {
                    val c1 = board[r][c + 1].candy?.type
                    val c2 = board[r][c + 2].candy?.type
                    assertFalse("No horizontal 3-matches on initial board at ($r, $c)", current == c1 && current == c2)
                }

                // Check vertical 3-in-a-row
                if (r + 2 < 10) {
                    val r1 = board[r + 1][c].candy?.type
                    val r2 = board[r + 2][c].candy?.type
                    assertFalse("No vertical 3-matches on initial board at ($r, $c)", current == r1 && current == r2)
                }
            }
        }
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
        val newLives = minOf(fullTimePassed.maxLives, fullTimePassed.lives + ((now - fullTimePassed.lastLifeUpdate) / (20 * 60 * 1000L)).toInt())
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

        // Simulate tampering
        val tamperedProfile = signedProfile.copy(coins = 9999999)
        assertFalse("Tampered currency must fail signature check", AntiCheatManager.verifySignature(tamperedProfile))
    }

    @Test
    fun testPlayBillingProductsConfig() {
        assertEquals("remove_ads", PlayBillingManager.PRODUCT_REMOVE_ADS)
        assertEquals("vip_monthly", PlayBillingManager.PRODUCT_VIP_MONTHLY)
        assertEquals("coins_500", PlayBillingManager.PRODUCT_COINS_500)
        assertEquals("gems_50", PlayBillingManager.PRODUCT_GEMS_50)
    }
}
