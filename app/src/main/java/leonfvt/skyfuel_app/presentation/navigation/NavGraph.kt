package leonfvt.skyfuel_app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import leonfvt.skyfuel_app.presentation.screen.AddBatteryScreen
import leonfvt.skyfuel_app.presentation.screen.BatteryDetailScreen
import leonfvt.skyfuel_app.presentation.screen.CategoryScreen
import leonfvt.skyfuel_app.presentation.screen.HomeScreen
import leonfvt.skyfuel_app.presentation.screen.OnboardingScreen
import leonfvt.skyfuel_app.presentation.screen.SettingsScreen
import leonfvt.skyfuel_app.presentation.screen.HistoryScreen
import leonfvt.skyfuel_app.presentation.screen.StatisticsScreen
import leonfvt.skyfuel_app.presentation.viewmodel.AddBatteryViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.BatteryDetailViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.CategoryViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.HomeViewModel

/**
 * Routes de navigation principales de l'application
 */
sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object AddBattery : Screen("add_battery")
    data object Settings : Screen("settings")
    data object Statistics : Screen("statistics")
    data object Categories : Screen("categories")
    data object BatteryDetail : Screen("battery_detail/{batteryId}") {
        fun createRoute(batteryId: Long): String = "battery_detail/$batteryId"
    }
    data object History : Screen("history/{batteryId}") {
        fun createRoute(batteryId: Long): String = "history/$batteryId"
    }
}

/**
 * Configuration du graphe de navigation principal
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Home.route,
    onOnboardingComplete: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Écran d'onboarding
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        // Écran d'accueil
        composable(route = Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            HomeScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToAddBattery = {
                    navController.navigate(Screen.AddBattery.route)
                },
                onNavigateToBatteryDetail = { batteryId ->
                    navController.navigate(Screen.BatteryDetail.createRoute(batteryId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStatistics = {
                    navController.navigate(Screen.Statistics.route)
                }
            )
        }
        
        // Écran des paramètres
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCategories = {
                    navController.navigate(Screen.Categories.route)
                }
            )
        }
        
        // Écran des statistiques
        composable(route = Screen.Statistics.route) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Écran des catégories
        composable(route = Screen.Categories.route) {
            val viewModel: CategoryViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            
            CategoryScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Écran d'ajout de batterie
        composable(route = Screen.AddBattery.route) {
            val viewModel: AddBatteryViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val navigationEvent by viewModel.navigationEvent.collectAsState()
            
            // Gestion des événements de navigation
            LaunchedEffect(navigationEvent) {
                navigationEvent?.let {
                    if (it == "back") {
                        navController.popBackStack()
                        viewModel.onNavigationEventConsumed()
                    }
                }
            }
            
            AddBatteryScreen(
                state = state,
                onEvent = viewModel::onEvent
            )
        }
        
        // Écran de détails d'une batterie
        composable(
            route = Screen.BatteryDetail.route,
            arguments = listOf(
                navArgument("batteryId") {
                    type = NavType.LongType
                }
            )
        ) {
            val viewModel: BatteryDetailViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val navigationEvent by viewModel.navigationEvent.collectAsState()
            
            // Gestion des événements de navigation
            LaunchedEffect(navigationEvent) {
                navigationEvent?.let {
                    if (it == "back") {
                        navController.popBackStack()
                        viewModel.onNavigationEventConsumed()
                    }
                }
            }
            
            BatteryDetailScreen(
                state = state,
                onEvent = viewModel::onEvent,
                onNavigateToHistory = { batteryId ->
                    navController.navigate(Screen.History.createRoute(batteryId))
                }
            )
        }
        
        // Écran d'historique d'une batterie
        composable(
            route = Screen.History.route,
            arguments = listOf(
                navArgument("batteryId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val batteryId = backStackEntry.arguments?.getLong("batteryId") ?: 0L
            
            HistoryScreen(
                batteryId = batteryId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}