package leonfvt.skyfuel_app.presentation.viewmodel.state

import androidx.compose.runtime.Stable
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.Category
import leonfvt.skyfuel_app.domain.model.ChargeReminder
import leonfvt.skyfuel_app.domain.model.ReminderType
import leonfvt.skyfuel_app.domain.service.BatteryPrediction
import leonfvt.skyfuel_app.domain.service.VoltageTrend
import java.time.DayOfWeek

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
    val error: String? = null,
    // Catégories
    val batteryCategories: List<Category> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val showCategorySelector: Boolean = false,
    // Rappels
    val reminders: List<ChargeReminder> = emptyList(),
    val showReminderDialog: Boolean = false,
    val editingReminder: ChargeReminder? = null,
    // Analytics
    val prediction: BatteryPrediction? = null,
    val voltageTrends: List<VoltageTrend> = emptyList()
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
    // Catégories
    data object ShowCategorySelector : BatteryDetailEvent()
    data object HideCategorySelector : BatteryDetailEvent()
    data class UpdateCategories(val categoryIds: List<Long>) : BatteryDetailEvent()
    // Rappels
    data object ShowAddReminder : BatteryDetailEvent()
    data class ShowEditReminder(val reminder: ChargeReminder) : BatteryDetailEvent()
    data object HideReminderDialog : BatteryDetailEvent()
    data class SaveReminder(
        val title: String,
        val hour: Int,
        val minute: Int,
        val daysOfWeek: Set<DayOfWeek>,
        val reminderType: ReminderType,
        val notes: String
    ) : BatteryDetailEvent()
    data class ToggleReminder(val reminder: ChargeReminder) : BatteryDetailEvent()
    data class DeleteReminder(val reminder: ChargeReminder) : BatteryDetailEvent()
    // Photo
    data class UpdatePhoto(val photoPath: String) : BatteryDetailEvent()
    data object RemovePhoto : BatteryDetailEvent()
}