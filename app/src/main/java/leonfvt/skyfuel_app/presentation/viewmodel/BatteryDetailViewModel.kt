package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.usecase.DeleteBatteryUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryDetailUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryHistoryUseCase
import leonfvt.skyfuel_app.domain.usecase.RecordVoltageReadingUseCase
import leonfvt.skyfuel_app.domain.usecase.UpdateBatteryStatusUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailState
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeFlowUseCase
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

/**
 * ViewModel pour l'écran de détails d'une batterie
 * Utilise les utilitaires de gestion d'erreurs pour plus de robustesse
 */
@HiltViewModel
class BatteryDetailViewModel @Inject constructor(
    private val getBatteryDetailUseCase: GetBatteryDetailUseCase,
    private val getBatteryHistoryUseCase: GetBatteryHistoryUseCase,
    private val updateBatteryStatusUseCase: UpdateBatteryStatusUseCase,
    private val recordVoltageReadingUseCase: RecordVoltageReadingUseCase,
    private val deleteBatteryUseCase: DeleteBatteryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Récupération de l'ID de la batterie depuis les arguments de navigation
    private val batteryId: Long = savedStateHandle.get<Long>("batteryId") ?: 0
    
    // État interne mutable
    private val _state = MutableStateFlow(BatteryDetailState(isLoading = true))
    
    // État exposé pour l'UI
    val state: StateFlow<BatteryDetailState> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteryDetailState(isLoading = true)
    )
    
    // Pour les événements de navigation
    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent
    
    init {
        loadBatteryDetail()
        loadBatteryHistory()
    }
    
    /**
     * Gère les événements de l'UI
     */
    fun onEvent(event: BatteryDetailEvent) {
        when (event) {
            is BatteryDetailEvent.UpdateStatus -> {
                updateBatteryStatus(event.newStatus, event.notes)
            }
            is BatteryDetailEvent.RecordVoltage -> {
                recordVoltage(event.voltage, event.notes)
            }
            is BatteryDetailEvent.AddNote -> {
                addNote(event.note)
            }
            is BatteryDetailEvent.DeleteBattery -> {
                deleteBattery()
            }
            is BatteryDetailEvent.NavigateBack -> {
                _navigationEvent.value = "back"
            }
            is BatteryDetailEvent.ClearError -> {
                clearError()
            }
        }
    }
    
    /**
     * Charge les détails de la batterie
     */
    private fun loadBatteryDetail() {
        executeUseCase(
            useCase = { getBatteryDetailUseCase(batteryId) },
            onStart = { _state.update { it.copy(isLoading = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement des détails de la batterie $batteryId")
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            },
            onSuccess = { battery ->
                _state.update {
                    it.copy(
                        battery = battery,
                        isLoading = false,
                        error = if (battery == null) "Batterie non trouvée" else null
                    )
                }
            }
        )
    }
    
    /**
     * Charge l'historique de la batterie
     */
    private fun loadBatteryHistory() {
        executeFlowUseCase(
            useCase = { getBatteryHistoryUseCase(batteryId) },
            onStart = { _state.update { it.copy(isHistoryLoading = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors du chargement de l'historique de la batterie $batteryId")
                _state.update { it.copy(isHistoryLoading = false, error = errorMessage) }
            },
            onEach = { history ->
                _state.update {
                    it.copy(
                        batteryHistory = history,
                        isHistoryLoading = false
                    )
                }
            }
        )
    }
    
    /**
     * Met à jour le statut de la batterie
     */
    private fun updateBatteryStatus(newStatus: leonfvt.skyfuel_app.domain.model.BatteryStatus, notes: String) {
        executeUseCase(
            useCase = { updateBatteryStatusUseCase(batteryId, newStatus, notes) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de la mise à jour du statut de la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = { 
                _state.update { it.copy(isActionInProgress = false) }
                // Recharger les détails de la batterie pour refléter les changements
                loadBatteryDetail()
                loadBatteryHistory()
            }
        )
    }
    
    /**
     * Enregistre une mesure de tension
     */
    private fun recordVoltage(voltage: Float, notes: String) {
        executeUseCase(
            useCase = { recordVoltageReadingUseCase(batteryId, voltage, notes) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de l'enregistrement de tension pour la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false, voltageInput = "") }
                // Recharger l'historique pour afficher la nouvelle mesure
                loadBatteryHistory()
            }
        )
    }
    
    /**
     * Ajoute une note à la batterie
     */
    private fun addNote(note: String) {
        // Implémentation à venir dans une prochaine itération
        // Pour l'instant, on simule le comportement
        _state.update { it.copy(noteInput = "") }
    }
    
    /**
     * Supprime la batterie
     */
    private fun deleteBattery() {
        val battery = state.value.battery ?: return
        
        executeUseCase(
            useCase = { deleteBatteryUseCase(battery) },
            onStart = { _state.update { it.copy(isActionInProgress = true) } },
            onError = { error ->
                val errorMessage = ErrorHandler.getUserMessage(error)
                ErrorHandler.logError(error, "Erreur lors de la suppression de la batterie $batteryId")
                _state.update { it.copy(isActionInProgress = false, error = errorMessage) }
            },
            onSuccess = {
                _state.update { it.copy(isActionInProgress = false) }
                // Navigation vers l'écran précédent
                _navigationEvent.value = "back"
            }
        )
    }
    
    /**
     * Réinitialise l'événement de navigation après l'avoir consommé
     */
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
    
    /**
     * Réinitialise l'erreur après l'avoir affichée
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}