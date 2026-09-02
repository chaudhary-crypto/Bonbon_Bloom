package com.example

import android.app.Application
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
        
        // Initialize Google Mobile Ads SDK on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(this@GameApplication) {
                    com.example.ads.AdManager.preloadAds(this@GameApplication)
                }
            } catch (e: Exception) {
                // Graceful fallback if ads SDK initialization encounters an issue
            }
        }

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "crush_candy_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        CoroutineScope(Dispatchers.IO).launch {
            val profile = database.userDao().getUserProfile().firstOrNull()
            if (profile != null) {
                SoundManager.soundEnabled = profile.soundEnabled
                SoundManager.musicEnabled = profile.musicEnabled
            }
        }
    }
}
