package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.game.GameViewModel
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HomeViewModel
import com.example.ui.screens.WorldMapScreen
import com.example.ui.theme.CrushCandyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrushCandyTheme {
                CrushCandyApp()
            }
        }
    }
}

@Composable
fun CrushCandyApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)

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
}
