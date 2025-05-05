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
import leonfvt.skyfuel_app.presentation.screen.AddBatteryScreen
import leonfvt.skyfuel_app.presentation.screen.BatteryDetailScreen
import leonfvt.skyfuel_app.presentation.screen.HomeScreen
import leonfvt.skyfuel_app.presentation.viewmodel.AddBatteryViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.BatteryDetailViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.HomeViewModel

/**
 * Routes de navigation principales de l'application
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddBattery : Screen("add_battery")
    data object BatteryDetail : Screen("battery_detail/{batteryId}") {
        fun createRoute(batteryId: Long): String = "battery_detail/$batteryId"
    }
}

/**
 * Configuration du graphe de navigation principal
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
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
                onEvent = viewModel::onEvent
            )
        }
    }
}