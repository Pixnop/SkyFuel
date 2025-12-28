package leonfvt.skyfuel_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import leonfvt.skyfuel_app.data.preferences.ThemeMode
import leonfvt.skyfuel_app.presentation.navigation.NavGraph
import leonfvt.skyfuel_app.presentation.navigation.Screen
import leonfvt.skyfuel_app.presentation.theme.SkyFuelTheme
import leonfvt.skyfuel_app.presentation.viewmodel.OnboardingViewModel
import leonfvt.skyfuel_app.presentation.viewmodel.ThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            
            val themeState by themeViewModel.themeState.collectAsState()
            val hasCompletedOnboarding by onboardingViewModel.hasCompletedOnboarding.collectAsState()
            
            // Déterminer si le thème sombre doit être utilisé
            val darkTheme = when (themeState.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            
            SkyFuelTheme(
                darkTheme = darkTheme,
                dynamicColor = themeState.dynamicColorsEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Attendre que l'état de l'onboarding soit chargé
                    when (hasCompletedOnboarding) {
                        null -> {
                            // Loading state - afficher un écran vide
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        false -> {
                            NavGraph(
                                navController = navController,
                                startDestination = Screen.Onboarding.route,
                                onOnboardingComplete = { onboardingViewModel.completeOnboarding() }
                            )
                        }
                        true -> {
                            NavGraph(
                                navController = navController,
                                startDestination = Screen.Home.route,
                                onOnboardingComplete = { }
                            )
                        }
                    }
                }
            }
        }
    }
}