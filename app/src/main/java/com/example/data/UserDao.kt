package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    @Query("UPDATE user_profile SET coins = coins + :amount WHERE id = 1")
    suspend fun addCoins(amount: Int)

    @Query("UPDATE user_profile SET diamonds = diamonds + :amount WHERE id = 1")
    suspend fun addDiamonds(amount: Int)

    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllLevelProgress(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId")
    suspend fun getLevelProgress(levelId: Int): LevelProgress?

    @Query("SELECT * FROM level_progress WHERE completed = 1 ORDER BY stars DESC, highScore DESC")
    suspend fun getCompletedLevelsByPerformance(): List<LevelProgress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLevelProgress(progress: LevelProgress)

    @Query("DELETE FROM level_progress")
    suspend fun clearAllLevelProgress()

    // --- Indexed Google Play Purchase Audit & Deduplication Ledger ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPurchaseRecord(record: PurchaseRecord)

    @Query("SELECT * FROM purchase_records WHERE purchaseToken = :token LIMIT 1")
    suspend fun getPurchaseByToken(token: String): PurchaseRecord?

    @Query("SELECT * FROM purchase_records WHERE status = :status ORDER BY timestamp DESC")
    suspend fun getPurchasesByStatus(status: String): List<PurchaseRecord>

    @Query("SELECT * FROM purchase_records ORDER BY timestamp DESC")
    fun observePurchaseRecords(): Flow<List<PurchaseRecord>>

    @Query("DELETE FROM purchase_records")
    suspend fun clearAllPurchaseRecords()
}
