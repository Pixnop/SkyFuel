package leonfvt.skyfuel_app.presentation.viewmodel.state

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus

/**
 * État des détails d'une batterie
 */
data class BatteryDetailState(
    val battery: Battery? = null,
    val batteryHistory: List<BatteryHistory> = emptyList(),
    val isLoading: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val error: String? = null,
    val voltageInput: String = "",
    val noteInput: String = ""
)

/**
 * Événements des détails d'une batterie
 */
sealed class BatteryDetailEvent {
    data class UpdateStatus(val newStatus: BatteryStatus, val notes: String = "") : BatteryDetailEvent()
    data class RecordVoltage(val voltage: Float, val notes: String = "") : BatteryDetailEvent()
    data class AddNote(val note: String) : BatteryDetailEvent()
    data object DeleteBattery : BatteryDetailEvent()
    data object NavigateBack : BatteryDetailEvent()
}