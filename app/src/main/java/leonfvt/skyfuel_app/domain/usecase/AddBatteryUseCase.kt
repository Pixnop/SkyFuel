package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Use case pour ajouter une nouvelle batterie
 */
class AddBatteryUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour ajouter une nouvelle batterie
     * @param brand marque de la batterie
     * @param model modèle de la batterie
     * @param serialNumber numéro de série
     * @param type type de batterie
     * @param cells nombre de cellules
     * @param capacity capacité en mAh
     * @param purchaseDate date d'achat
     * @param notes notes supplémentaires
     * @return l'identifiant de la nouvelle batterie
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
    ): Long {
        // Validation des données
        validateInputs(brand, model, serialNumber, cells, capacity)
        
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
        
        return repository.addBattery(battery)
    }
    
    /**
     * Valide les entrées pour la création d'une batterie
     * @throws IllegalArgumentException si une entrée est invalide
     */
    private fun validateInputs(
        brand: String,
        model: String,
        serialNumber: String,
        cells: Int,
        capacity: Int
    ) {
        if (brand.isBlank()) {
            throw IllegalArgumentException("La marque ne peut pas être vide")
        }
        
        if (model.isBlank()) {
            throw IllegalArgumentException("Le modèle ne peut pas être vide")
        }
        
        if (serialNumber.isBlank()) {
            throw IllegalArgumentException("Le numéro de série ne peut pas être vide")
        }
        
        if (cells <= 0) {
            throw IllegalArgumentException("Le nombre de cellules doit être positif")
        }
        
        if (capacity <= 0) {
            throw IllegalArgumentException("La capacité doit être positive")
        }
    }
}