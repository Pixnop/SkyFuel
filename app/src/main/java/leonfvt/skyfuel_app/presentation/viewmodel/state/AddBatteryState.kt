package leonfvt.skyfuel_app.presentation.viewmodel.state

import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate

/**
 * État de l'ajout d'une batterie
 */
data class AddBatteryState(
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val batteryType: BatteryType = BatteryType.LIPO,
    val cells: String = "",
    val capacity: String = "",
    val purchaseDate: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val brandError: Boolean = false,
    val modelError: Boolean = false,
    val serialNumberError: Boolean = false,
    val cellsError: Boolean = false,
    val capacityError: Boolean = false
) {
    /**
     * Vérifie si le formulaire est valide
     */
    val isFormValid: Boolean
        get() = brand.isNotBlank() && model.isNotBlank() && serialNumber.isNotBlank() &&
                cells.isNotBlank() && capacity.isNotBlank() &&
                !brandError && !modelError && !serialNumberError && !cellsError && !capacityError
    
    /**
     * Vérifie si le formulaire a été modifié par rapport à l'état initial
     */
    val isFormModified: Boolean
        get() = brand.isNotBlank() || model.isNotBlank() || serialNumber.isNotBlank() ||
                cells.isNotBlank() || capacity.isNotBlank() || notes.isNotBlank() ||
                batteryType != BatteryType.LIPO || purchaseDate != LocalDate.now()
}

/**
 * Événements de l'ajout d'une batterie
 */
sealed class AddBatteryEvent {
    data class UpdateBrand(val brand: String) : AddBatteryEvent()
    data class UpdateModel(val model: String) : AddBatteryEvent()
    data class UpdateSerialNumber(val serialNumber: String) : AddBatteryEvent()
    data class UpdateBatteryType(val type: BatteryType) : AddBatteryEvent()
    data class UpdateCells(val cells: String) : AddBatteryEvent()
    data class UpdateCapacity(val capacity: String) : AddBatteryEvent()
    data class UpdatePurchaseDate(val date: LocalDate) : AddBatteryEvent()
    data class UpdateNotes(val notes: String) : AddBatteryEvent()
    data object SubmitBattery : AddBatteryEvent()
    data object NavigateBack : AddBatteryEvent()
}