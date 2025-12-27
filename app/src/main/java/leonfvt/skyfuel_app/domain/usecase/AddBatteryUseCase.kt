package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.model.Result
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.util.ErrorHandler
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case pour ajouter une nouvelle batterie.
 *
 * Retourne un [Result] encapsulant soit l'ID de la batterie créée,
 * soit une erreur avec un message explicite.
 */
class AddBatteryUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour ajouter une nouvelle batterie.
     *
     * @param brand marque de la batterie
     * @param model modèle de la batterie
     * @param serialNumber numéro de série
     * @param type type de batterie
     * @param cells nombre de cellules
     * @param capacity capacité en mAh
     * @param purchaseDate date d'achat
     * @param notes notes supplémentaires
     * @return [Result.Success] avec l'ID de la batterie, ou [Result.Error] en cas d'échec
     */
    suspend operator fun invoke(
        brand: String,
        model: String,
        serialNumber: String,
        type: BatteryType,
        cells: Int,
        capacity: Int,
        purchaseDate: LocalDate = LocalDate.now(),
        notes: String = ""
    ): Result<Long> {
        Timber.d("AddBatteryUseCase: Adding battery $brand $model")

        // Validation des données
        val validationError = validateInputs(brand, model, serialNumber, cells, capacity)
        if (validationError != null) {
            Timber.w("AddBatteryUseCase: Validation failed - $validationError")
            return Result.error(ErrorHandler.AppError.ValidationError(validationError))
        }

        return Result.runCatchingSuspend {
            val battery = Battery(
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

            val id = repository.addBattery(battery)
            Timber.i("AddBatteryUseCase: Battery added successfully with ID $id")
            id
        }
    }

    /**
     * Valide les entrées pour la création d'une batterie.
     *
     * @return Message d'erreur si validation échoue, null si tout est valide
     */
    private fun validateInputs(
        brand: String,
        model: String,
        serialNumber: String,
        cells: Int,
        capacity: Int
    ): String? {
        return when {
            brand.isBlank() -> "La marque ne peut pas être vide"
            model.isBlank() -> "Le modèle ne peut pas être vide"
            serialNumber.isBlank() -> "Le numéro de série ne peut pas être vide"
            cells <= 0 -> "Le nombre de cellules doit être positif"
            capacity <= 0 -> "La capacité doit être positive"
            else -> null
        }
    }
}