package leonfvt.skyfuel_app

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe d'application principale pour SkyFuel
 * L'annotation HiltAndroidApp est nécessaire pour l'initialisation de Hilt
 * et la génération du code d'injection de dépendances
 */
@HiltAndroidApp
class SkyFuelApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize ThreeTenABP for java.time API support on older Android versions
        initializeThreeTenABP()
    }
    
    private fun initializeThreeTenABP() {
        try {
            AndroidThreeTen.init(this)
        } catch (e: Exception) {
            // Log error but continue - the app may still work on newer Android versions
            android.util.Log.e("SkyFuelApp", "Failed to initialize ThreeTenABP: ${e.message}")
        }
    }
}