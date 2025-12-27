package leonfvt.skyfuel_app.presentation.viewmodel.state

import androidx.compose.runtime.Stable
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus

/**
 * État des détails d'une batterie
 * @Stable indique à Compose que cette classe est immuable pour optimiser les recompositions
 */
@Stable
data class BatteryDetailState(
    val battery: Battery? = null,
    val batteryHistory: List<BatteryHistory> = emptyList(),
    val isLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val error: String? = null
) {
    /**
     * Indique si une opération est en cours (chargement initial ou action)
     */
    val isAnyOperationInProgress: Boolean
        get() = isLoading || isHistoryLoading || isActionInProgress
}

/**
 * Événements des détails d'une batterie
 */
sealed class BatteryDetailEvent {
    data class UpdateStatus(val newStatus: BatteryStatus, val notes: String = "") : BatteryDetailEvent()
    data class RecordVoltage(val voltage: Float, val notes: String = "") : BatteryDetailEvent()
    data class AddNote(val note: String) : BatteryDetailEvent()
    data class RecordMaintenance(val description: String) : BatteryDetailEvent()
    data object DeleteBattery : BatteryDetailEvent()
    data object NavigateBack : BatteryDetailEvent()
    data object ClearError : BatteryDetailEvent()
}