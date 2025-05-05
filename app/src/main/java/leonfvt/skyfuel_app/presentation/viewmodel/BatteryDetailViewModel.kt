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
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.usecase.DeleteBatteryUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryDetailUseCase
import leonfvt.skyfuel_app.domain.usecase.GetBatteryHistoryUseCase
import leonfvt.skyfuel_app.domain.usecase.RecordVoltageReadingUseCase
import leonfvt.skyfuel_app.domain.usecase.UpdateBatteryStatusUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.BatteryDetailState
import javax.inject.Inject

/**
 * ViewModel pour l'écran de détails d'une batterie
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
        }
    }
    
    /**
     * Charge les détails de la batterie
     */
    private fun loadBatteryDetail() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            try {
                val battery = getBatteryDetailUseCase(batteryId)
                
                _state.update {
                    it.copy(
                        battery = battery,
                        isLoading = false,
                        error = if (battery == null) "Batterie non trouvée" else null
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors du chargement des détails"
                    )
                }
            }
        }
    }
    
    /**
     * Charge l'historique de la batterie
     */
    private fun loadBatteryHistory() {
        viewModelScope.launch {
            _state.update { it.copy(isHistoryLoading = true) }
            
            try {
                getBatteryHistoryUseCase(batteryId).collect { history ->
                    _state.update {
                        it.copy(
                            batteryHistory = history,
                            isHistoryLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isHistoryLoading = false,
                        error = e.message ?: "Erreur lors du chargement de l'historique"
                    )
                }
            }
        }
    }
    
    /**
     * Met à jour le statut de la batterie
     */
    private fun updateBatteryStatus(newStatus: leonfvt.skyfuel_app.domain.model.BatteryStatus, notes: String) {
        viewModelScope.launch {
            try {
                updateBatteryStatusUseCase(batteryId, newStatus, notes)
                
                // Recharger les détails de la batterie pour refléter les changements
                loadBatteryDetail()
                loadBatteryHistory()
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Erreur lors de la mise à jour du statut")
                }
            }
        }
    }
    
    /**
     * Enregistre une mesure de tension
     */
    private fun recordVoltage(voltage: Float, notes: String) {
        viewModelScope.launch {
            try {
                recordVoltageReadingUseCase(batteryId, voltage, notes)
                
                // Recharger l'historique pour afficher la nouvelle mesure
                loadBatteryHistory()
                
                // Réinitialiser le champ de saisie de tension
                _state.update { it.copy(voltageInput = "") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Erreur lors de l'enregistrement de la tension")
                }
            }
        }
    }
    
    /**
     * Ajoute une note à la batterie
     */
    private fun addNote(note: String) {
        viewModelScope.launch {
            try {
                // Appel à une méthode du repository pour ajouter une note
                // (à implémenter dans une prochaine itération)
                
                // Recharger l'historique pour afficher la nouvelle note
                loadBatteryHistory()
                
                // Réinitialiser le champ de saisie de note
                _state.update { it.copy(noteInput = "") }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Erreur lors de l'ajout de la note")
                }
            }
        }
    }
    
    /**
     * Supprime la batterie
     */
    private fun deleteBattery() {
        viewModelScope.launch {
            val battery = state.value.battery ?: return@launch
            
            try {
                deleteBatteryUseCase(battery)
                
                // Navigation vers l'écran précédent
                _navigationEvent.value = "back"
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Erreur lors de la suppression de la batterie")
                }
            }
        }
    }
    
    /**
     * Réinitialise l'événement de navigation après l'avoir consommé
     */
    fun onNavigationEventConsumed() {
        _navigationEvent.value = null
    }
}