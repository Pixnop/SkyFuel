package leonfvt.skyfuel_app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.util.ErrorHandler
import leonfvt.skyfuel_app.util.UseCaseExtensions.executeUseCase
import javax.inject.Inject

data class StatisticsState(
    val isLoading: Boolean = true,
    val totalBatteries: Int = 0,
    val chargedCount: Int = 0,
    val dischargedCount: Int = 0,
    val storageCount: Int = 0,
    val outOfServiceCount: Int = 0,
    val averageCycleCount: Float = 0f,
    val maxCycles: Int = 200, // Max cycles recommandé par défaut
    val cyclesByBrand: Map<String, Float> = emptyMap(),
    val cyclesByType: Map<String, Float> = emptyMap(),
    val healthyCount: Int = 0,      // < 50% des cycles max
    val warningCount: Int = 0,      // 50-80% des cycles max
    val criticalCount: Int = 0,     // > 80% des cycles max
    val oldestBattery: Battery? = null,
    val mostUsedBattery: Battery? = null,
    val totalCycles: Int = 0,
    val monthlyActivity: List<Pair<String, Float>> = emptyList()
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: BatteryRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    fun refresh() {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        executeUseCase(
            useCase = { repository.getAllBatteries().first() },
            onStart = { _state.update { it.copy(isLoading = true) } },
            onError = { error ->
                ErrorHandler.logError(error, "Erreur lors du chargement des statistiques")
                _state.update { it.copy(isLoading = false) }
            },
            onSuccess = { batteries ->
                if (batteries.isEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    return@executeUseCase
                }
                
                calculateAndUpdateStatistics(batteries)
            }
        )
    }
    
    /**
     * Calcule et met à jour les statistiques à partir de la liste des batteries
     */
    private fun calculateAndUpdateStatistics(batteries: List<Battery>) {
        // Comptages par statut
        val chargedCount = batteries.count { it.status == BatteryStatus.CHARGED }
        val dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED }
        val storageCount = batteries.count { it.status == BatteryStatus.STORAGE }
        val outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE }
        
        // Cycles moyens
        val averageCycles = batteries.map { it.cycleCount }.average().toFloat()
        val totalCycles = batteries.sumOf { it.cycleCount }
        
        // Cycles par marque
        val cyclesByBrand = batteries
            .groupBy { it.brand }
            .mapValues { (_, batteryList) ->
                batteryList.map { it.cycleCount }.average().toFloat()
            }
        
        // Cycles par type
        val cyclesByType = batteries
            .groupBy { it.type.name }
            .mapValues { (_, batteryList) ->
                batteryList.map { it.cycleCount }.average().toFloat()
            }
        
        // Santé des batteries (basée sur 200 cycles max)
        val maxCycles = 200
        val healthyCount = batteries.count { it.cycleCount < maxCycles * 0.5 }
        val warningCount = batteries.count { it.cycleCount >= maxCycles * 0.5 && it.cycleCount < maxCycles * 0.8 }
        val criticalCount = batteries.count { it.cycleCount >= maxCycles * 0.8 }
        
        // Batterie la plus ancienne
        val oldestBattery = batteries.minByOrNull { it.purchaseDate }
        
        // Batterie la plus utilisée
        val mostUsedBattery = batteries.maxByOrNull { it.cycleCount }
        
        _state.update {
            it.copy(
                isLoading = false,
                totalBatteries = batteries.size,
                chargedCount = chargedCount,
                dischargedCount = dischargedCount,
                storageCount = storageCount,
                outOfServiceCount = outOfServiceCount,
                averageCycleCount = averageCycles,
                maxCycles = maxCycles,
                cyclesByBrand = cyclesByBrand,
                cyclesByType = cyclesByType,
                healthyCount = healthyCount,
                warningCount = warningCount,
                criticalCount = criticalCount,
                oldestBattery = oldestBattery,
                mostUsedBattery = mostUsedBattery,
                totalCycles = totalCycles
            )
        }
    }
}
