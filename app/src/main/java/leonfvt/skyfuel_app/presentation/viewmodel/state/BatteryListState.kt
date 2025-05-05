package leonfvt.skyfuel_app.presentation.viewmodel.state

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus

/**
 * État de la liste des batteries
 */
data class BatteryListState(
    val batteries: List<Battery> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterStatus: BatteryStatus? = null
)

/**
 * Événements de la liste des batteries
 */
sealed class BatteryListEvent {
    data class Search(val query: String) : BatteryListEvent()
    data class Filter(val status: BatteryStatus?) : BatteryListEvent()
    data class SelectBattery(val battery: Battery) : BatteryListEvent()
    object ClearSearch : BatteryListEvent()
    object RefreshList : BatteryListEvent()
}