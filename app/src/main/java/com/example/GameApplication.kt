package com.example

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.auth.CloudSyncManager
import com.example.data.AccountStorage
import com.example.data.AppDatabase
import com.example.payment.PlayBillingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GameApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GameApplication", "Global CrashShield intercepted exception on thread ${thread.name}: ${throwable.message}", throwable)
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {}
        }

        // Resolve named Firestore DB ID from resources
        try {
            CloudSyncManager.initialize(this)
        } catch (e: Throwable) {
            Log.w("GameApplication", "CloudSyncManager initialization failed: ${e.message}")
        }

        // In debug builds, pre-initialize MobileAds; release builds wait for UMP consent in MainActivity
        if (BuildConfig.DEBUG) {
            try {
                com.example.ads.ConsentManager.initializeMobileAds(this)
            } catch (e: Throwable) {
                Log.w("GameApplication", "AdMob startup initialization deferred: ${e.message}")
            }
        }

        // Initialize Room Database with non-destructive migrations through v8 (indexed tables + purchase ledger)
        try {
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "crush_candy_db"
            )
                .addMigrations(
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_6_7,
                    AppDatabase.MIGRATION_7_8
                )
                .build()
        } catch (e: Throwable) {
            Log.e("GameApplication", "Disk database failed, falling back to memory database: ${e.message}")
            database = Room.inMemoryDatabaseBuilder(
                applicationContext,
                AppDatabase::class.java
            ).build()
        }

        AccountStorage.init(this)

        // Initialize Google Play Billing 8.0.0 wired to UserDao for verified entitlement fulfillment & reconciliation
        try {
            PlayBillingManager.initialize(this, database.userDao())
        } catch (e: Throwable) {
            Log.w("GameApplication", "PlayBillingManager initialization skipped: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = database.userDao().getUserProfile().firstOrNull()
                if (profile != null) {
                    SoundManager.soundEnabled = profile.soundEnabled
                    SoundManager.musicEnabled = profile.musicEnabled
                    HapticManager.hapticsEnabled = profile.hapticsEnabled
                }
            } catch (_: Throwable) {}
        }
    }
}
