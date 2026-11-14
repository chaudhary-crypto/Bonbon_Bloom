package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfile::class, LevelProgress::class, PurchaseRecord::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hasRemovedAds INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hapticsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN languageCode TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN hasAskedNotificationPermission INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isAdmin INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_authUid` ON `user_profile` (`authUid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_email` ON `user_profile` (`email`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_profile_lastCloudSyncTime` ON `user_profile` (`lastCloudSyncTime`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_level_progress_completed` ON `level_progress` (`completed`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_level_progress_stars` ON `level_progress` (`stars`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_level_progress_highScore` ON `level_progress` (`highScore`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `purchase_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `purchaseToken` TEXT NOT NULL,
                        `orderId` TEXT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `isSubscription` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `entitledApplied` INTEGER NOT NULL,
                        `verificationSource` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_records_purchaseToken` ON `purchase_records` (`purchaseToken`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_records_orderId` ON `purchase_records` (`orderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_records_productId` ON `purchase_records` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_records_status` ON `purchase_records` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchase_records_timestamp` ON `purchase_records` (`timestamp`)")
            }
        }
    }
}
