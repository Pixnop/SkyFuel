package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour enregistrer une maintenance sur une batterie
 */
class RecordMaintenanceUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour enregistrer une maintenance
     * @param batteryId l'ID de la batterie
     * @param description description de la maintenance effectuée
     * @throws IllegalArgumentException si la description est vide
     */
    suspend operator fun invoke(
        batteryId: Long,
        description: String
    ) {
        // Validation de la description
        if (description.isBlank()) {
            throw IllegalArgumentException("La description de maintenance ne peut pas être vide")
        }

        // Enregistrement de la maintenance
        repository.recordMaintenance(batteryId, description)
    }
}
