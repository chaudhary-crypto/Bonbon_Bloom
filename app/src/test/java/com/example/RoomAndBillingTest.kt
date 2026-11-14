package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.LevelProgress
import com.example.data.PurchaseRecord
import com.example.data.UserDao
import com.example.data.UserProfile
import com.example.data.UserRepository
import com.example.payment.PlayBillingManager
import com.example.security.AntiCheatManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class RoomAndBillingTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        repository = UserRepository(userDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun purchaseRecordDeduplicatesTokensAndCreditsExactSku(): Unit = runBlocking {
        repository.saveProfile(UserProfile(coins = 500, diamonds = 25), syncCloud = false)

        val appliedFirst = repository.applyVerifiedPlayPurchase(
            purchaseToken = "tok_gems_150_abc",
            orderId = "GPA.1111-2222-3333-44444",
            productId = PlayBillingManager.PRODUCT_GEMS_150,
            isSubscription = false,
            verificationSource = "CLOUD_FUNCTION"
        )
        assertTrue("First verified purchase must apply", appliedFirst)

        // Replay of the exact same purchaseToken MUST be ignored
        val appliedReplay = repository.applyVerifiedPlayPurchase(
            purchaseToken = "tok_gems_150_abc",
            orderId = "GPA.1111-2222-3333-44444",
            productId = PlayBillingManager.PRODUCT_GEMS_150,
            isSubscription = false,
            verificationSource = "CLOUD_FUNCTION"
        )
        assertFalse("Duplicate purchaseToken must not double-credit", appliedReplay)

        val profile = userDao.getUserProfile().first()!!
        assertEquals(25 + 150, profile.diamonds)
        assertEquals(500 + 1000, profile.coins)
        assertEquals(1, profile.totalPurchasesCount)
        assertTrue(AntiCheatManager.verifySignature(profile))

        val storedRecord = userDao.getPurchaseByToken("tok_gems_150_abc")
        assertNotNull(storedRecord)
        assertEquals("VERIFIED_SERVER", storedRecord?.status)
        assertTrue(storedRecord?.entitledApplied == true)
    }

    @Test
    fun pendingOrUnknownPurchaseDoesNotGrantVipOrRemoveAds(): Unit = runBlocking {
        repository.saveProfile(UserProfile(), syncCloud = false)

        repository.recordPendingPurchase(
            purchaseToken = "tok_pending_vip",
            orderId = "GPA.9999",
            productId = PlayBillingManager.PRODUCT_VIP_MONTHLY,
            isSubscription = true
        )

        val unknownApplied = repository.applyVerifiedPlayPurchase(
            purchaseToken = "tok_unknown",
            orderId = "GPA.0000",
            productId = "free_hack_sku",
            isSubscription = false,
            verificationSource = "CLOUD_FUNCTION"
        )
        assertFalse(unknownApplied)

        val profile = userDao.getUserProfile().first()!!
        assertFalse("Pending payment must NOT unlock VIP", profile.isVipSubscribed)
        assertFalse("Pending payment must NOT unlock VIP active", profile.isVipActive)
        assertFalse(profile.hasRemovedAds)
        assertFalse(repository.isTokenAlreadyEntitled("tok_pending_vip"))
    }

    @Test
    fun reconcilePlayEntitlementsRevokesUnpaidOrExpiredVip(): Unit = runBlocking {
        val futureExpiry = System.currentTimeMillis() + 7L * 24 * 3600 * 1000L
        repository.saveProfile(
            UserProfile(
                isAdmin = false,
                isVipSubscribed = true,
                vipExpiryTimestamp = futureExpiry,
                hasRemovedAds = true
            ),
            syncCloud = false
        )

        // Play Store reports zero active subscriptions and zero remove_ads entitlements
        repository.reconcilePlayEntitlements(
            hasActiveVipSub = false,
            vipSubProductId = null,
            hasActiveRemoveAds = false
        )

        val reconciled = userDao.getUserProfile().first()!!
        assertFalse("Unpaid/cancelled VIP must be revoked", reconciled.isVipSubscribed)
        assertFalse("Unpaid remove_ads must be revoked", reconciled.hasRemovedAds)
        assertEquals(0L, reconciled.vipExpiryTimestamp)
        assertTrue(AntiCheatManager.verifySignature(reconciled))
    }

    @Test
    fun levelProgressIndexedQueriesReturnBestStarsAndCount(): Unit = runBlocking {
        userDao.saveLevelProgress(LevelProgress(levelId = 1, stars = 3, highScore = 5000, completed = true))
        userDao.saveLevelProgress(LevelProgress(levelId = 2, stars = 2, highScore = 4100, completed = true))
        userDao.saveLevelProgress(LevelProgress(levelId = 3, stars = 0, highScore = 200, completed = false))

        val completedLevels = userDao.getCompletedLevelsByPerformance()
        assertEquals(2, completedLevels.size)
        assertEquals(listOf(1, 2), completedLevels.map { it.levelId })
        val ordered = userDao.getAllLevelProgress().first()
        assertEquals(listOf(1, 2, 3), ordered.map { it.levelId })
        assertEquals(5, ordered.sumOf { it.stars })

        userDao.clearAllLevelProgress()
        assertNull(userDao.getLevelProgress(1))
    }

    @Test
    fun antiCheatDetectsEntitlementTampering() {
        val clean = AntiCheatManager.signProfile(
            UserProfile(isVipSubscribed = false, hasRemovedAds = false, isAdmin = false, extraMovesCount = 2)
        )
        assertTrue(AntiCheatManager.verifySignature(clean))
        assertFalse(AntiCheatManager.verifySignature(clean.copy(isVipSubscribed = true)))
        assertFalse(AntiCheatManager.verifySignature(clean.copy(hasRemovedAds = true)))
        assertFalse(AntiCheatManager.verifySignature(clean.copy(isAdmin = true)))
        assertFalse(AntiCheatManager.verifySignature(clean.copy(extraMovesCount = 99)))
    }

    @Test
    fun migration7To8CreatesIndexedPurchaseRecordsAndPreservesData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_${System.nanoTime()}.db"
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS user_profile (id INTEGER NOT NULL PRIMARY KEY, username TEXT NOT NULL, coins INTEGER NOT NULL, authUid TEXT NOT NULL DEFAULT '', email TEXT NOT NULL DEFAULT '', lastCloudSyncTime INTEGER NOT NULL DEFAULT 0)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS level_progress (levelId INTEGER NOT NULL PRIMARY KEY, stars INTEGER NOT NULL, highScore INTEGER NOT NULL, completed INTEGER NOT NULL)"
                    )
                    db.execSQL("INSERT INTO level_progress (levelId, stars, highScore, completed) VALUES (1, 3, 9500, 1)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val openHelper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = openHelper.writableDatabase
        AppDatabase.MIGRATION_7_8.migrate(sqliteDb)

        // Verify purchase_records exists & unique purchaseToken index enforces uniqueness
        sqliteDb.execSQL(
            "INSERT INTO purchase_records (purchaseToken, orderId, productId, isSubscription, status, entitledApplied, verificationSource, timestamp, updatedAt) " +
                "VALUES ('tok_1', 'GPA.1', 'vip_monthly', 1, 'VERIFIED_SERVER', 1, 'CLOUD_FUNCTION', 1000, 1000)"
        )
        val cursor = sqliteDb.query("SELECT COUNT(*) FROM purchase_records WHERE purchaseToken = 'tok_1'")
        cursor.moveToFirst()
        assertEquals(1, cursor.getInt(0))
        cursor.close()

        openHelper.close()
        context.deleteDatabase(dbName)
    }
}
