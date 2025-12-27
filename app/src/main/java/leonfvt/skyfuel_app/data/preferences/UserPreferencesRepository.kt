package leonfvt.skyfuel_app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Préférences utilisateur pour les notifications
 */
data class NotificationPreferences(
    val alertsEnabled: Boolean = true,
    val chargeRemindersEnabled: Boolean = true,
    val maintenanceRemindersEnabled: Boolean = true,
    val lowBatteryWarningsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

/**
 * Préférences utilisateur pour le thème
 */
enum class ThemeMode {
    SYSTEM,  // Suit le thème du système
    LIGHT,   // Toujours clair
    DARK     // Toujours sombre
}

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorsEnabled: Boolean = true  // Material You
)

/**
 * Toutes les préférences utilisateur
 */
data class UserPreferences(
    val notifications: NotificationPreferences = NotificationPreferences(),
    val theme: ThemePreferences = ThemePreferences(),
    val hasCompletedOnboarding: Boolean = false
)

/**
 * Repository pour gérer les préférences utilisateur avec DataStore
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    companion object {
        // Clés pour les notifications
        private val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        private val CHARGE_REMINDERS_ENABLED = booleanPreferencesKey("charge_reminders_enabled")
        private val MAINTENANCE_REMINDERS_ENABLED = booleanPreferencesKey("maintenance_reminders_enabled")
        private val LOW_BATTERY_WARNINGS_ENABLED = booleanPreferencesKey("low_battery_warnings_enabled")
        private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        
        // Clés pour le thème
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS_ENABLED = booleanPreferencesKey("dynamic_colors_enabled")
        
        // Autres préférences
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
    
    /**
     * Flow des préférences utilisateur
     */
    val userPreferences: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            notifications = NotificationPreferences(
                alertsEnabled = preferences[ALERTS_ENABLED] ?: true,
                chargeRemindersEnabled = preferences[CHARGE_REMINDERS_ENABLED] ?: true,
                maintenanceRemindersEnabled = preferences[MAINTENANCE_REMINDERS_ENABLED] ?: true,
                lowBatteryWarningsEnabled = preferences[LOW_BATTERY_WARNINGS_ENABLED] ?: true,
                vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
                soundEnabled = preferences[SOUND_ENABLED] ?: true
            ),
            theme = ThemePreferences(
                themeMode = try {
                    ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                },
                dynamicColorsEnabled = preferences[DYNAMIC_COLORS_ENABLED] ?: true
            ),
            hasCompletedOnboarding = preferences[HAS_COMPLETED_ONBOARDING] ?: false
        )
    }
    
    /**
     * Flow des préférences de notification uniquement
     */
    val notificationPreferences: Flow<NotificationPreferences> = dataStore.data.map { preferences ->
        NotificationPreferences(
            alertsEnabled = preferences[ALERTS_ENABLED] ?: true,
            chargeRemindersEnabled = preferences[CHARGE_REMINDERS_ENABLED] ?: true,
            maintenanceRemindersEnabled = preferences[MAINTENANCE_REMINDERS_ENABLED] ?: true,
            lowBatteryWarningsEnabled = preferences[LOW_BATTERY_WARNINGS_ENABLED] ?: true,
            vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
            soundEnabled = preferences[SOUND_ENABLED] ?: true
        )
    }
    
    /**
     * Flow des préférences de thème uniquement
     */
    val themePreferences: Flow<ThemePreferences> = dataStore.data.map { preferences ->
        ThemePreferences(
            themeMode = try {
                ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            },
            dynamicColorsEnabled = preferences[DYNAMIC_COLORS_ENABLED] ?: true
        )
    }
    
    // ============ Méthodes pour les notifications ============
    
    suspend fun setAlertsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[ALERTS_ENABLED] = enabled
        }
    }
    
    suspend fun setChargeRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CHARGE_REMINDERS_ENABLED] = enabled
        }
    }
    
    suspend fun setMaintenanceRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[MAINTENANCE_REMINDERS_ENABLED] = enabled
        }
    }
    
    suspend fun setLowBatteryWarningsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[LOW_BATTERY_WARNINGS_ENABLED] = enabled
        }
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED] = enabled
        }
    }
    
    // ============ Méthodes pour le thème ============
    
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }
    
    suspend fun setDynamicColorsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS_ENABLED] = enabled
        }
    }
    
    // ============ Autres préférences ============
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }
}
