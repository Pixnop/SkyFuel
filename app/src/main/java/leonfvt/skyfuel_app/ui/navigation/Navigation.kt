package leonfvt.skyfuel_app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import leonfvt.skyfuel_app.ui.screens.AddBatteryScreen
import leonfvt.skyfuel_app.ui.screens.BatteryDetailsScreen
import leonfvt.skyfuel_app.ui.screens.HomeScreen

/**
 * Routes de navigation principales de l'application
 */
object NavRoutes {
    const val HOME = "home"
    const val BATTERY_DETAILS = "battery_details/{batteryId}"
    const val ADD_BATTERY = "add_battery"
    
    /**
     * Crée la route pour l'écran de détails d'une batterie avec l'ID
     */
    fun batteryDetails(batteryId: Long): String {
        return "battery_details/$batteryId"
    }
}

/**
 * Composant de navigation principal
 */
@Composable
fun SkyFuelNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        // Écran d'accueil / dashboard
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        
        // Écran de détails d'une batterie
        composable(
            route = NavRoutes.BATTERY_DETAILS,
            arguments = listOf(
                navArgument("batteryId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val batteryId = backStackEntry.arguments?.getLong("batteryId") ?: 0
            BatteryDetailsScreen(
                batteryId = batteryId,
                navController = navController
            )
        }
        
        // Écran d'ajout d'une batterie
        composable(NavRoutes.ADD_BATTERY) {
            AddBatteryScreen(navController = navController)
        }
    }
}