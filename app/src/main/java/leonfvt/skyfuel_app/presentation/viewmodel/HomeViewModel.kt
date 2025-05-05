package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.usecase.GetAllBatteriesUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteriesByStatusUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryStatisticsUseCase
import leonfvt.skyfuel_app.domain.usecase.SearchBatteriesUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListState
import javax.inject.Inject

/**
 * ViewModel pour l'écran d'accueil (dashboard)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllBatteriesUseCase: GetAllBatteriesUseCase,
    private val getBatteriesByStatusUseCase: GetBatteriesByStatusUseCase,
    private val searchBatteriesUseCase: SearchBatteriesUseCase,
    private val getBatteryStatisticsUseCase: GetBatteryStatisticsUseCase
) : ViewModel() {
    
    // État interne mutable
    private val _state = MutableStateFlow(BatteryListState(isLoading = true))
    
    // État exposé pour l'UI
    val state: StateFlow<BatteryListState> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteryListState(isLoading = true)
    )
    
    // Statistiques
    private val _statistics = MutableStateFlow<Any?>(null)
    
    init {
        loadBatteries()
        loadStatistics()
    }
    
    /**
     * Gère les événements de l'UI
     */
    fun onEvent(event: BatteryListEvent) {
        when (event) {
            is BatteryListEvent.Search -> {
                _state.update { it.copy(searchQuery = event.query, isLoading = true) }
                searchBatteries(event.query)
            }
            is BatteryListEvent.Filter -> {
                _state.update { it.copy(filterStatus = event.status, isLoading = true) }
                filterBatteries(event.status)
            }
            is BatteryListEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "", isLoading = true) }
                loadBatteries()
            }
            is BatteryListEvent.RefreshList -> {
                _state.update { it.copy(isLoading = true) }
                loadBatteries()
                loadStatistics()
            }
            else -> {} // Les autres événements sont gérés ailleurs
        }
    }
    
    /**
     * Charge toutes les batteries
     */
    private fun loadBatteries() {
        viewModelScope.launch {
            try {
                // Combine l'état actuel avec le flux de données
                getAllBatteriesUseCase().collect { batteries ->
                    _state.update {
                        it.copy(
                            batteries = batteries,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors du chargement des batteries"
                    )
                }
            }
        }
    }
    
    /**
     * Filtre les batteries par statut
     */
    private fun filterBatteries(status: leonfvt.skyfuel_app.domain.model.BatteryStatus?) {
        viewModelScope.launch {
            try {
                getBatteriesByStatusUseCase(status).collect { batteries ->
                    _state.update {
                        it.copy(
                            batteries = batteries,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors du filtrage des batteries"
                    )
                }
            }
        }
    }
    
    /**
     * Recherche des batteries
     */
    private fun searchBatteries(query: String) {
        viewModelScope.launch {
            try {
                searchBatteriesUseCase(query).collect { batteries ->
                    _state.update {
                        it.copy(
                            batteries = batteries,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de la recherche"
                    )
                }
            }
        }
    }
    
    /**
     * Charge les statistiques des batteries
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val statistics = getBatteryStatisticsUseCase()
                _statistics.value = statistics
            } catch (e: Exception) {
                // Gérer l'erreur si nécessaire
            }
        }
    }
}