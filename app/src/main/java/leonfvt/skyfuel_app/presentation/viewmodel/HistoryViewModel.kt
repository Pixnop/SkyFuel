package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeFlowUseCase
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

data class HistoryState(
    val isLoading: Boolean = true,
    val battery: Battery? = null,
    val history: List<BatteryHistory> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val batteryRepository: BatteryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()
    
    fun loadHistory(batteryId: Long) {
        // Charger la batterie d'abord
        executeUseCase(
            useCase = { batteryRepository.getBatteryById(batteryId) },
            onStart = { _state.update { it.copy(isLoading = true, errorMessage = null) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement de la batterie $batteryId")
                _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
            },
            onSuccess = { battery ->
                _state.update { it.copy(battery = battery) }
                
                // Puis charger l'historique
                executeFlowUseCase(
                    useCase = { batteryRepository.getBatteryHistory(batteryId) },
                    onStart = { /* Déjà en chargement */ },
                    onError = { error ->
                        val errorMessage = ErrorHandler.getUserMessage(error)
                        ErrorHandler.logError(error, "Erreur lors du chargement de l'historique")
                        _state.update { it.copy(isLoading = false, errorMessage = errorMessage) }
                    },
                    onEach = { historyList ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                history = historyList.sortedByDescending { h -> h.timestamp }
                            ) 
                        }
                    }
                )
            }
        )
    }
}
