package com.example

import android.app.Application
import android.util.Log
import androidx.room.Room
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class GameApplication : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        // Global crash shield for all Android devices:
        // Catches unexpected runtime crashes, logs telemetry, and prevents abrupt process termination
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("GameApplication", "Global CrashShield intercepted exception on thread ${thread.name}: ${throwable.message}", throwable)
            // Attempt graceful fallback or delegate to default handler
            try {
                defaultHandler?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {}
        }

        // Initialize Google Mobile Ads SDK safely on main thread
        try {
            MobileAds.initialize(this) {
                try {
                    com.example.ads.AdManager.preloadAds(this)
                } catch (_: Throwable) {}
            }
        } catch (e: Throwable) {
            Log.w("GameApplication", "Ads SDK initialization skipped: ${e.message}")
        }

        // Initialize Google Play Billing 7.x
        try {
            com.example.payment.PlayBillingManager.initialize(this)
        } catch (e: Throwable) {
            Log.w("GameApplication", "PlayBillingManager initialization skipped: ${e.message}")
        }

        // Initialize Room Database with storage protection and non-destructive migrations
        try {
            database = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java,
                "crush_candy_db"
            )
            .addMigrations(AppDatabase.MIGRATION_5_6)
            .build()
        } catch (e: Throwable) {
            Log.e("GameApplication", "Disk database failed, falling back to memory database: ${e.message}")
            database = Room.inMemoryDatabaseBuilder(
                applicationContext,
                AppDatabase::class.java
            )
            .build()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val profile = database.userDao().getUserProfile().firstOrNull()
                if (profile != null) {
                    SoundManager.soundEnabled = profile.soundEnabled
                    SoundManager.musicEnabled = profile.musicEnabled
                    com.example.audio.HapticManager.hapticsEnabled = profile.hapticsEnabled
                }
            } catch (_: Throwable) {}
        }
    }
}
