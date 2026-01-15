package com.example.mtgutilityapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mtgutilityapp.data.repository.CardRepository
import com.example.mtgutilityapp.ui.camera.CameraScreen
import com.example.mtgutilityapp.ui.camera.CameraViewModel
import com.example.mtgutilityapp.ui.favorites.FavoritesScreen
import com.example.mtgutilityapp.ui.favorites.FavoritesViewModel
import com.example.mtgutilityapp.ui.history.HistoryScreen
import com.example.mtgutilityapp.ui.history.HistoryViewModel

sealed class Screen(val route: String) {
    object Camera : Screen("camera")
    object History : Screen("history")
    object Favorites : Screen("favorites")
}

@Composable
fun NavGraph(repository: CardRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Camera.route
    ) {
        composable(Screen.Camera.route) {
            val viewModel = CameraViewModel(repository)
            CameraScreen(
                viewModel = viewModel,
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Camera.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route) {
                        popUpTo(Screen.Camera.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.History.route) {
            val viewModel = HistoryViewModel(repository)
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Camera.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route) {
                        popUpTo(Screen.Camera.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Screen.Favorites.route) {
            val viewModel = FavoritesViewModel(repository)
            FavoritesScreen(
                viewModel = viewModel,
                onNavigateToScan = {
                    navController.navigate(Screen.Camera.route) {
                        popUpTo(Screen.Camera.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(Screen.Camera.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
