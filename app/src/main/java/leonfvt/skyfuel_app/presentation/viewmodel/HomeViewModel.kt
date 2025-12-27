package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.BatteryAlert
import leonfvt.skyfuel_app.domain.usecase.GetAllBatteriesUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryAlertsUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteriesByStatusUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryStatisticsUseCase
import leonfvt.skyfuel_app.domain.usecase.SearchBatteriesUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryListState
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeFlowUseCase
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

/**
 * ViewModel pour l'écran d'accueil (dashboard)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAllBatteriesUseCase: GetAllBatteriesUseCase,
    private val getBatteriesByStatusUseCase: GetBatteriesByStatusUseCase,
    private val searchBatteriesUseCase: SearchBatteriesUseCase,
    private val getBatteryStatisticsUseCase: GetBatteryStatisticsUseCase,
    private val getBatteryAlertsUseCase: GetBatteryAlertsUseCase
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
    
    // Alertes ignorées (temporairement en mémoire)
    private val dismissedAlertIds = mutableSetOf<Long>()
    
    init {
        loadBatteries()
        loadStatistics()
        loadAlerts()
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
            is BatteryListEvent.Sort -> {
                _state.update { it.copy(sortOption = event.option) }
            }
            is BatteryListEvent.ClearSearch -> {
                _state.update { it.copy(searchQuery = "", isLoading = true) }
                loadBatteries()
            }
            is BatteryListEvent.RefreshList -> {
                _state.update { it.copy(isLoading = true) }
                loadBatteries()
                loadStatistics()
                loadAlerts()
            }
            is BatteryListEvent.DismissAlert -> {
                dismissAlert(event.alert)
            }
            is BatteryListEvent.AlertClicked -> {
                // Navigation gérée dans l'UI
            }
            else -> {} // Les autres événements sont gérés ailleurs
        }
    }
    
    /**
     * Charge toutes les batteries
     */
    private fun loadBatteries() {
        executeFlowUseCase(
            useCase = { getAllBatteriesUseCase() },
            onStart = { /* déjà en état de chargement */ },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement des batteries")
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            },
            onEach = { batteries ->
                _state.update {
                    it.copy(
                        batteries = batteries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        )
    }
    
    /**
     * Charge les alertes
     */
    private fun loadAlerts() {
        executeFlowUseCase(
            useCase = { getBatteryAlertsUseCase() },
            onStart = { /* Les alertes ne bloquent pas l'UI */ },
            onError = { error ->
                // Les alertes sont optionnelles, on log simplement l'erreur
                ErrorHandler.logError(error, "Erreur lors du chargement des alertes")
            },
            onEach = { alerts ->
                // Filtrer les alertes ignorées
                val visibleAlerts = alerts.filterNot { it.batteryId in dismissedAlertIds }
                _state.update { it.copy(alerts = visibleAlerts) }
            }
        )
    }
    
    /**
     * Ignore une alerte
     */
    private fun dismissAlert(alert: BatteryAlert) {
        dismissedAlertIds.add(alert.batteryId)
        _state.update { currentState ->
            currentState.copy(
                alerts = currentState.alerts.filterNot { it.batteryId == alert.batteryId }
            )
        }
    }
    
    /**
     * Filtre les batteries par statut
     */
    private fun filterBatteries(status: leonfvt.skyfuel_app.domain.model.BatteryStatus?) {
        executeFlowUseCase(
            useCase = { getBatteriesByStatusUseCase(status) },
            onStart = { /* déjà en état de chargement */ },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du filtrage des batteries")
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            },
            onEach = { batteries ->
                _state.update {
                    it.copy(
                        batteries = batteries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        )
    }
    
    /**
     * Recherche des batteries
     */
    private fun searchBatteries(query: String) {
        executeFlowUseCase(
            useCase = { searchBatteriesUseCase(query) },
            onStart = { /* déjà en état de chargement */ },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de la recherche")
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            },
            onEach = { batteries ->
                _state.update {
                    it.copy(
                        batteries = batteries,
                        isLoading = false,
                        error = null
                    )
                }
            }
        )
    }
    
    /**
     * Charge les statistiques des batteries
     */
    private fun loadStatistics() {
        executeUseCase(
            useCase = { getBatteryStatisticsUseCase() },
            onStart = { /* Les statistiques ne bloquent pas l'UI */ },
            onError = { error ->
                ErrorHandler.logError(error, "Erreur lors du chargement des statistiques")
            },
            onSuccess = { statistics ->
                _statistics.value = statistics
            }
        )
    }
}