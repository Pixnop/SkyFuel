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
import leonfvt.skyfuel_app.domain.service.BatteryPrediction
import leonfvt.skyfuel_app.domain.service.BatteryPredictionService
import leonfvt.skyfuel_app.domain.service.FleetHealthScore
import leonfvt.skyfuel_app.domain.service.VoltageTrend
import leonfvt.skyfuel_app.domain.service.WeeklyActivity
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
    val maxCycles: Int = 200,
    val cyclesByBrand: Map<String, Float> = emptyMap(),
    val cyclesByType: Map<String, Float> = emptyMap(),
    val healthyCount: Int = 0,
    val warningCount: Int = 0,
    val criticalCount: Int = 0,
    val oldestBattery: Battery? = null,
    val mostUsedBattery: Battery? = null,
    val totalCycles: Int = 0,
    // Nouvelles données analytiques
    val predictions: List<BatteryPrediction> = emptyList(),
    val fleetHealth: FleetHealthScore? = null,
    val voltageTrends: List<VoltageTrend> = emptyList(),
    val activityHeatmap: List<WeeklyActivity> = emptyList(),
    val selectedBatteryPrediction: BatteryPrediction? = null
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: BatteryRepository,
    private val predictionService: BatteryPredictionService
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        loadStatistics()
    }

    fun refresh() {
        loadStatistics()
    }

    fun selectBatteryPrediction(prediction: BatteryPrediction?) {
        _state.update { it.copy(selectedBatteryPrediction = prediction) }
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
                loadPredictions(batteries)
                loadHistoryData(batteries)
            }
        )
    }

    private fun calculateAndUpdateStatistics(batteries: List<Battery>) {
        val chargedCount = batteries.count { it.status == BatteryStatus.CHARGED }
        val dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED }
        val storageCount = batteries.count { it.status == BatteryStatus.STORAGE }
        val outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE }
        val averageCycles = batteries.map { it.cycleCount }.average().toFloat()
        val totalCycles = batteries.sumOf { it.cycleCount }

        val cyclesByBrand = batteries
            .groupBy { it.brand }
            .mapValues { (_, list) -> list.map { it.cycleCount }.average().toFloat() }

        val cyclesByType = batteries
            .groupBy { it.type.name }
            .mapValues { (_, list) -> list.map { it.cycleCount }.average().toFloat() }

        val maxCycles = 200
        val healthyCount = batteries.count { it.cycleCount < maxCycles * 0.5 }
        val warningCount = batteries.count { it.cycleCount >= maxCycles * 0.5 && it.cycleCount < maxCycles * 0.8 }
        val criticalCount = batteries.count { it.cycleCount >= maxCycles * 0.8 }
        val oldestBattery = batteries.minByOrNull { it.purchaseDate }
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

    private fun loadPredictions(batteries: List<Battery>) {
        executeUseCase(
            useCase = {
                batteries.map { battery ->
                    val history = repository.getBatteryHistory(battery.id).first()
                    predictionService.predictBatteryLifespan(battery, history)
                }
            },
            onStart = { },
            onError = { error ->
                ErrorHandler.logError(error, "Erreur lors du calcul des prédictions")
            },
            onSuccess = { predictions ->
                val fleetHealth = predictionService.calculateFleetHealth(
                    batteries, predictions
                )
                _state.update {
                    it.copy(
                        predictions = predictions,
                        fleetHealth = fleetHealth,
                        selectedBatteryPrediction = predictions.firstOrNull()
                    )
                }
            }
        )
    }

    private fun loadHistoryData(batteries: List<Battery>) {
        executeUseCase(
            useCase = {
                val allHistory = batteries.flatMap { battery ->
                    repository.getBatteryHistory(battery.id).first()
                }
                allHistory
            },
            onStart = { },
            onError = { error ->
                ErrorHandler.logError(error, "Erreur lors du chargement de l'historique")
            },
            onSuccess = { allHistory ->
                val voltageTrends = predictionService.getVoltageTrends(allHistory)
                val heatmap = predictionService.generateActivityHeatmap(allHistory)

                _state.update {
                    it.copy(
                        voltageTrends = voltageTrends,
                        activityHeatmap = heatmap
                    )
                }
            }
        )
    }
}
