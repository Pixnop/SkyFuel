package leonfvt.skyfuel_app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * États possibles du chargement des batteries
 */
sealed class BatteriesLoadState {
    object Loading : BatteriesLoadState()
    data class Success(val batteries: List<Battery>) : BatteriesLoadState()
    data class Error(val message: String) : BatteriesLoadState()
}

/**
 * ViewModel pour la gestion des batteries
 */
@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val batteryRepository: BatteryRepository
) : ViewModel() {
    
    // État de filtrage
    private val _filterStatus = MutableStateFlow<BatteryStatus?>(null)
    val filterStatus = _filterStatus.asStateFlow()
    
    // État de recherche
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    // État de chargement des batteries
    val batteriesState: StateFlow<BatteriesLoadState> = combine(
        batteryRepository.getAllBatteries(),
        _filterStatus,
        _searchQuery
    ) { batteries, filterStatus, searchQuery ->
        // Appliquer d'abord le filtre par statut si nécessaire
        val filteredByStatus = if (filterStatus != null) {
            batteries.filter { it.status == filterStatus }
        } else {
            batteries
        }
        
        // Ensuite, appliquer la recherche si nécessaire
        val filteredBySearch = if (searchQuery.isNotBlank()) {
            filteredByStatus.filter {
                it.brand.contains(searchQuery, ignoreCase = true) ||
                it.model.contains(searchQuery, ignoreCase = true) ||
                it.serialNumber.contains(searchQuery, ignoreCase = true) ||
                it.notes.contains(searchQuery, ignoreCase = true)
            }
        } else {
            filteredByStatus
        }
        
        BatteriesLoadState.Success(filteredBySearch)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteriesLoadState.Loading
    )
    
    // Batterie sélectionnée et son historique
    private val _selectedBattery = MutableStateFlow<Battery?>(null)
    val selectedBattery = _selectedBattery.asStateFlow()
    
    private val _batteryHistory = MutableStateFlow<List<BatteryHistory>>(emptyList())
    val batteryHistory = _batteryHistory.asStateFlow()
    
    /**
     * Définit le filtre par statut
     */
    fun setStatusFilter(status: BatteryStatus?) {
        _filterStatus.value = status
    }
    
    /**
     * Définit la recherche
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Sélectionne une batterie et charge son historique
     */
    fun selectBattery(battery: Battery) {
        _selectedBattery.value = battery
        
        // Charger l'historique de la batterie sélectionnée
        viewModelScope.launch {
            batteryRepository.getBatteryHistory(battery.id).collect { history ->
                _batteryHistory.value = history
            }
        }
    }
    
    /**
     * Désélectionne la batterie actuelle
     */
    fun clearSelectedBattery() {
        _selectedBattery.value = null
        _batteryHistory.value = emptyList()
    }
    
    /**
     * Ajoute une nouvelle batterie
     */
    fun addBattery(
        brand: String,
        model: String,
        serialNumber: String,
        type: BatteryType,
        cells: Int,
        capacity: Int,
        purchaseDate: LocalDate,
        notes: String
    ) {
        val newBattery = Battery(
            brand = brand,
            model = model,
            serialNumber = serialNumber,
            type = type,
            cells = cells,
            capacity = capacity,
            purchaseDate = purchaseDate,
            status = BatteryStatus.CHARGED,
            notes = notes
        )
        
        viewModelScope.launch {
            batteryRepository.addBattery(newBattery)
        }
    }
    
    /**
     * Met à jour le statut d'une batterie
     */
    fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String = "") {
        viewModelScope.launch {
            batteryRepository.updateBatteryStatus(batteryId, newStatus, notes)
            
            // Mettre à jour la batterie sélectionnée si c'est celle qui a été modifiée
            _selectedBattery.value?.let { battery ->
                if (battery.id == batteryId) {
                    val updatedBattery = batteryRepository.getBatteryById(batteryId)
                    _selectedBattery.value = updatedBattery
                }
            }
        }
    }
    
    /**
     * Enregistre une mesure de tension pour une batterie
     */
    fun recordVoltage(batteryId: Long, voltage: Float, notes: String = "") {
        viewModelScope.launch {
            batteryRepository.recordVoltageReading(batteryId, voltage, notes)
        }
    }
    
    /**
     * Supprime une batterie
     */
    fun deleteBattery(battery: Battery) {
        viewModelScope.launch {
            batteryRepository.deleteBattery(battery)
            
            // Si la batterie supprimée était sélectionnée, la désélectionner
            if (_selectedBattery.value?.id == battery.id) {
                clearSelectedBattery()
            }
        }
    }
}