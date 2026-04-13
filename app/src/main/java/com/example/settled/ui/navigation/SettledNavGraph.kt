package com.example.settled.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.settled.ui.navigation.components.CustomBottomBar
import com.example.settled.ui.screens.home.HomeScreen
import com.example.settled.ui.screens.splash.SplashScreen
import com.example.settled.ui.screens.settings.SettingsScreen

sealed class Route(val route: String) {
    object Splash : Route("splash")
    object Home : Route("home")
    object Settings : Route("settings")
    object AddCardFlow : Route("add_card_flow")
    
    // Add Card Wizard Routes
    object AddCardBankSelect : Route("add_card_bank_select")
    object AddCardTypeSelect : Route("add_card_type_select")
    object AddCardDetailsEntry : Route("add_card_details_entry")
    object AddCardSuccess : Route("add_card_success")
    
    object Details : Route("details/{cardId}") {
        fun createRoute(cardId: String) = "details/$cardId"
    }
}

@Composable
fun SettledNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine current index for bottom bar
    val selectedTab = when {
        currentDestination?.hierarchy?.any { it.route == Route.Home.route } == true -> 0
        currentDestination?.hierarchy?.any { it.route == Route.AddCardFlow.route } == true -> 1
        currentDestination?.hierarchy?.any { it.route == Route.Settings.route } == true -> 2
        else -> 0
    }

    // Hide bottom bar only on Splash screen
    val showBottomBar = currentDestination?.route != Route.Splash.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CustomBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        val targetRoute = when (index) {
                            0 -> Route.Home.route
                            1 -> Route.AddCardFlow.route
                            2 -> Route.Settings.route
                            else -> Route.Home.route
                        }
                        
                        navController.navigate(targetRoute) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            // on the back stack as users select items
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = Route.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
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
                    onNavigateToAdd = { navController.navigate(Route.AddCardFlow.route) },
                    onNavigateToDetails = { cardId -> navController.navigate(Route.Details.createRoute(cardId)) }
                )
            }
            
            composable(Route.Settings.route) {
                SettingsScreen()
            }
            
            navigation(startDestination = Route.AddCardBankSelect.route, route = Route.AddCardFlow.route) {
                composable(Route.AddCardBankSelect.route) { backStackEntry ->
                    val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                        navController.getBackStackEntry(Route.AddCardFlow.route)
                    }
                    val viewModel: com.example.settled.ui.screens.addcard.AddCardViewModel = hiltViewModel(parentEntry)
                    
                    com.example.settled.ui.screens.addcard.BankSelectionScreen(
                        viewModel = viewModel,
                        onNavigateNext = { navController.navigate(Route.AddCardTypeSelect.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Route.AddCardTypeSelect.route) { backStackEntry ->
                    val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                        navController.getBackStackEntry(Route.AddCardFlow.route)
                    }
                    val viewModel: com.example.settled.ui.screens.addcard.AddCardViewModel = hiltViewModel(parentEntry)
                    
                    com.example.settled.ui.screens.addcard.CardSelectionScreen(
                        viewModel = viewModel,
                        onNavigateNext = { navController.navigate(Route.AddCardDetailsEntry.route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Route.AddCardDetailsEntry.route) { backStackEntry ->
                    val parentEntry = androidx.compose.runtime.remember(backStackEntry) {
                        navController.getBackStackEntry(Route.AddCardFlow.route)
                    }
                    val viewModel: com.example.settled.ui.screens.addcard.AddCardViewModel = hiltViewModel(parentEntry)
                    
                    com.example.settled.ui.screens.addcard.CardDetailsEntryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateSuccess = { navController.navigate(Route.AddCardSuccess.route) }
                    )
                }
                composable(Route.AddCardSuccess.route) {
                    com.example.settled.ui.screens.addcard.AddCardSuccessScreen(
                        onNavigateHome = { 
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.Home.route) { inclusive = true }
                            }
                        }
                    )
                }
            }
            
            composable(Route.Details.route) {
                com.example.settled.ui.screens.details.CardDetailsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
