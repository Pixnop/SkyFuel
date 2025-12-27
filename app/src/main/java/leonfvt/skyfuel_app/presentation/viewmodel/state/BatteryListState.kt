package leonfvt.skyfuel_app.presentation.viewmodel.state

import androidx.compose.runtime.Stable
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryAlert
import leonfvt.skyfuel_app.domain.model.BatteryStatus

/**
 * Options de tri pour la liste des batteries
 */
enum class SortOption {
    NAME_ASC,       // Par nom (marque + modèle) A-Z
    NAME_DESC,      // Par nom (marque + modèle) Z-A
    DATE_NEWEST,    // Plus récent d'abord
    DATE_OLDEST,    // Plus ancien d'abord
    CAPACITY_HIGH,  // Capacité décroissante
    CAPACITY_LOW,   // Capacité croissante
    CYCLES_HIGH,    // Cycles décroissants
    CYCLES_LOW      // Cycles croissants
}

/**
 * État de la liste des batteries
 * @Stable indique à Compose que cette classe est immuable pour optimiser les recompositions
 */
@Stable
data class BatteryListState(
    val batteries: List<Battery> = emptyList(),
    val alerts: List<BatteryAlert> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val filterStatus: BatteryStatus? = null,
    val sortOption: SortOption = SortOption.NAME_ASC
) {
    /**
     * Retourne les batteries triées selon l'option sélectionnée
     */
    val sortedBatteries: List<Battery>
        get() = when (sortOption) {
            SortOption.NAME_ASC -> batteries.sortedBy { "${it.brand} ${it.model}".lowercase() }
            SortOption.NAME_DESC -> batteries.sortedByDescending { "${it.brand} ${it.model}".lowercase() }
            SortOption.DATE_NEWEST -> batteries.sortedByDescending { it.purchaseDate }
            SortOption.DATE_OLDEST -> batteries.sortedBy { it.purchaseDate }
            SortOption.CAPACITY_HIGH -> batteries.sortedByDescending { it.capacity }
            SortOption.CAPACITY_LOW -> batteries.sortedBy { it.capacity }
            SortOption.CYCLES_HIGH -> batteries.sortedByDescending { it.cycleCount }
            SortOption.CYCLES_LOW -> batteries.sortedBy { it.cycleCount }
        }
}

/**
 * Événements de la liste des batteries
 */
sealed class BatteryListEvent {
    data class Search(val query: String) : BatteryListEvent()
    data class Filter(val status: BatteryStatus?) : BatteryListEvent()
    data class Sort(val option: SortOption) : BatteryListEvent()
    data class SelectBattery(val battery: Battery) : BatteryListEvent()
    data class DismissAlert(val alert: BatteryAlert) : BatteryListEvent()
    data class AlertClicked(val alert: BatteryAlert) : BatteryListEvent()
    data object ClearSearch : BatteryListEvent()
    data object RefreshList : BatteryListEvent()
}