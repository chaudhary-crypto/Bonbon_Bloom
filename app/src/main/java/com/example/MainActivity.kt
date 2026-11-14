package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.audio.SoundManager
import com.example.game.GameViewModel
import com.example.notifications.NotificationScheduler
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.WorldMapScreen
import com.example.ui.theme.BonbonBloomTheme
import com.example.ads.ConsentManager
import com.example.ads.AdManager

class MainActivity : ComponentActivity() {

    companion object {
        var currentActivity: ComponentActivity? = null
    }

    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationScheduler.scheduleDailyNotifications(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentActivity = this

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        com.example.auth.configureAppCheck(this, intent)

        enableEdgeToEdge()

        // Google Mobile Ads & UMP Consent flow: gather EEA/UK/state consent before initializing/loading ads in release builds
        if (BuildConfig.DEBUG) {
            ConsentManager.initializeMobileAds(this)
            AdManager.preloadAds(this)
        }
        ConsentManager.requestConsent(this) { canRequest ->
            if (canRequest || BuildConfig.DEBUG) {
                AdManager.preloadAds(this)
            }
        }

        // Pre-Android 13 scheduling (Android 13+ deferred until level 1 complete)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationScheduler.scheduleDailyNotifications(this)
        }
        
        // Hide the system bars (status bar and navigation bar) for a full immersive game experience
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            BonbonBloomTheme {
                BonbonBloomApp()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }


    override fun onStart() {
        super.onStart()
        try {
            SoundManager.setAppInForeground(true)
        } catch (_: Throwable) {}
    }

    override fun onResume() {
        super.onResume()
        try {
            SoundManager.setAppInForeground(true)
            SoundManager.resume()
            com.example.payment.PlayBillingManager.queryAndRestorePurchases()
        } catch (_: Throwable) {}
    }

    override fun onPause() {
        super.onPause()
        try {
            SoundManager.setAppInForeground(false)
            SoundManager.stopAudioEngine()
        } catch (_: Throwable) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            SoundManager.setAppInForeground(false)
            SoundManager.stopAudioEngine()
        } catch (_: Throwable) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            SoundManager.setAppInForeground(false)
            SoundManager.stopAudioEngine()
        } catch (_: Throwable) {}
    }
}

@Composable
fun BonbonBloomApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val isAdLoading by com.example.ads.AdManager.isAdLoading.collectAsState()

    androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onAdventureClicked = {
                    navController.navigate("map")
                },
                onQuickPlayClicked = { levelId ->
                    navController.navigate("game/$levelId")
                }
            )
        }

        composable("map") {
            WorldMapScreen(
                viewModel = homeViewModel,
                onLevelSelected = { levelId ->
                    navController.navigate("game/$levelId")
                },
                onBackToHome = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "game/{levelId}",
            arguments = listOf(navArgument("levelId") { type = NavType.IntType; defaultValue = 1 })
        ) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getInt("levelId") ?: 1
            val gameViewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
            GameScreen(
                viewModel = gameViewModel,
                levelId = levelId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }

    if (isAdLoading) {
        com.example.ui.components.AdLoadingOverlay()
    }
}
}
