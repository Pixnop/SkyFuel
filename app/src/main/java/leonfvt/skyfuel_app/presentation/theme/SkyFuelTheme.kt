package leonfvt.skyfuel_app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Palette de couleurs pour le thème clair
 */
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = LightOnPrimary,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = LightOnPrimary,
    
    secondary = Secondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = SecondaryLight,
    onSecondaryContainer = LightOnSecondary,
    
    tertiary = Tertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = TertiaryLight,
    onTertiaryContainer = LightOnTertiary,
    
    background = LightBackground,
    onBackground = LightOnBackground,
    
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurface,
    
    error = Error,
    onError = LightOnPrimary
)

/**
 * Palette de couleurs pour le thème sombre
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = DarkOnPrimary,
    primaryContainer = Primary,
    onPrimaryContainer = LightOnPrimary,
    
    secondary = SecondaryLight,
    onSecondary = DarkOnSecondary,
    secondaryContainer = Secondary,
    onSecondaryContainer = LightOnSecondary,
    
    tertiary = TertiaryLight,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = Tertiary,
    onTertiaryContainer = LightOnTertiary,
    
    background = DarkBackground,
    onBackground = DarkOnBackground,
    
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurface,
    
    error = Error,
    onError = LightOnPrimary
)

/**
 * Thème principal de l'application SkyFuel
 */
@Composable
fun SkyFuelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Supporte les thèmes dynamiques pour Android 12+
    dynamicColor: Boolean = false, // Disabled by default to maintain brand consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            // Set status bar color with slight transparency for a more modern look
            window.statusBarColor = colorScheme.primary.toArgb()
            
            // Set system bars appearance based on theme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            
            // Edge-to-edge design (full screen immersive mode)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}