package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import javax.inject.Inject

/**
 * ViewModel pour gérer l'état de l'onboarding
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    /**
     * Indique si l'onboarding a été complété
     */
    val hasCompletedOnboarding: StateFlow<Boolean?> = userPreferencesRepository.userPreferences
        .map { it.hasCompletedOnboarding }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null // null = loading
        )
    
    /**
     * Marque l'onboarding comme complété
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
        }
    }
}
