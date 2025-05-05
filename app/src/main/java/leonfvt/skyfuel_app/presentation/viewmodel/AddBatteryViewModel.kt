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
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.usecase.AddBatteryUseCase
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryEvent
import leonfvt.skyfuel_app.presentation.viewmodel.state.AddBatteryState
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel pour l'écran d'ajout d'une batterie
 */
@HiltViewModel
class AddBatteryViewModel @Inject constructor(
    private val addBatteryUseCase: AddBatteryUseCase
) : ViewModel() {
    
    // État interne mutable
    private val _state = MutableStateFlow(AddBatteryState())
    
    // État exposé pour l'UI
    val state: StateFlow<AddBatteryState> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddBatteryState()
    )
    
    // Pour les événements de navigation
    private val _navigationEvent = MutableStateFlow<String?>(null)
    val navigationEvent: StateFlow<String?> = _navigationEvent
    
    /**
     * Gère les événements de l'UI
     */
    fun onEvent(event: AddBatteryEvent) {
        when (event) {
            is AddBatteryEvent.UpdateBrand -> {
                _state.update {
                    it.copy(
                        brand = event.brand,
                        brandError = event.brand.isBlank()
                    )
                }
            }
            is AddBatteryEvent.UpdateModel -> {
                _state.update {
                    it.copy(
                        model = event.model,
                        modelError = event.model.isBlank()
                    )
                }
            }
            is AddBatteryEvent.UpdateSerialNumber -> {
                _state.update {
                    it.copy(
                        serialNumber = event.serialNumber,
                        serialNumberError = event.serialNumber.isBlank()
                    )
                }
            }
            is AddBatteryEvent.UpdateBatteryType -> {
                _state.update { it.copy(batteryType = event.type) }
            }
            is AddBatteryEvent.UpdateCells -> {
                val cellsValue = event.cells.filter { it.isDigit() }
                val isError = cellsValue.isBlank() || cellsValue.toIntOrNull()?.let { it <= 0 } ?: true
                
                _state.update {
                    it.copy(
                        cells = cellsValue,
                        cellsError = isError
                    )
                }
            }
            is AddBatteryEvent.UpdateCapacity -> {
                val capacityValue = event.capacity.filter { it.isDigit() }
                val isError = capacityValue.isBlank() || capacityValue.toIntOrNull()?.let { it <= 0 } ?: true
                
                _state.update {
                    it.copy(
                        capacity = capacityValue,
                        capacityError = isError
                    )
                }
            }
            is AddBatteryEvent.UpdatePurchaseDate -> {
                _state.update { it.copy(purchaseDate = event.date) }
            }
            is AddBatteryEvent.UpdateNotes -> {
                _state.update { it.copy(notes = event.notes) }
            }
            is AddBatteryEvent.SubmitBattery -> {
                submitBattery()
            }
            is AddBatteryEvent.NavigateBack -> {
                _navigationEvent.value = "back"
            }
        }
    }
    
    /**
     * Soumet le formulaire pour ajouter une nouvelle batterie
     */
    private fun submitBattery() {
        val currentState = _state.value
        
        // Vérification de validation
        if (!currentState.isFormValid) {
            _state.update {
                it.copy(
                    brandError = it.brand.isBlank(),
                    modelError = it.model.isBlank(),
                    serialNumberError = it.serialNumber.isBlank(),
                    cellsError = it.cells.isBlank() || it.cells.toIntOrNull()?.let { cells -> cells <= 0 } ?: true,
                    capacityError = it.capacity.isBlank() || it.capacity.toIntOrNull()?.let { capacity -> capacity <= 0 } ?: true,
                    errorMessage = "Veuillez corriger les erreurs dans le formulaire"
                )
            }
            return
        }
        
        // Soumission du formulaire
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                addBatteryUseCase(
                    brand = currentState.brand,
                    model = currentState.model,
                    serialNumber = currentState.serialNumber,
                    type = currentState.batteryType,
                    cells = currentState.cells.toInt(),
                    capacity = currentState.capacity.toInt(),
                    purchaseDate = currentState.purchaseDate,
                    notes = currentState.notes
                )
                
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true
                    )
                }
                
                // Navigation de retour vers l'écran précédent
                _navigationEvent.value = "back"
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.message ?: "Erreur lors de l'ajout de la batterie"
                    )
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