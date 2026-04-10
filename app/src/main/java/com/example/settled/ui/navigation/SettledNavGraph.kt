package com.example.settled.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.settled.ui.screens.home.HomeScreen
import com.example.settled.ui.screens.splash.SplashScreen

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Home : Route("home")
    object AddCard : Route("add_card")
}

@Composable
fun SettledNavGraph() {
    val navController = rememberNavController()
    
    NavHost(navController = navController, startDestination = Route.Splash.route) {
        composable(Route.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Route.Home.route) {
            HomeScreen(
                onNavigateToAdd = { navController.navigate(Route.AddCard.route) },
                onNavigateToSettings = { /* Navigate to S9 */ }
            )
        }
        composable(Route.AddCard.route) {
            com.example.settled.ui.screens.addcard.AddCardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateHome = { 
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
