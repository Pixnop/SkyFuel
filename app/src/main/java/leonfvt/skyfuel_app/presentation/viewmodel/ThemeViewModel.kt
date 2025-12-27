package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.data.preferences.ThemeMode
import leonfvt.skyfuel_app.data.preferences.ThemePreferences
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import javax.inject.Inject

/**
 * État du thème de l'application
 */
data class ThemeState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColorsEnabled: Boolean = true
)

/**
 * ViewModel pour gérer le thème de l'application
 */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    val themeState: StateFlow<ThemeState> = userPreferencesRepository.themePreferences
        .map { prefs ->
            ThemeState(
                themeMode = prefs.themeMode,
                dynamicColorsEnabled = prefs.dynamicColorsEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeState()
        )
    
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }
    
    fun setDynamicColorsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDynamicColorsEnabled(enabled)
        }
    }
}
