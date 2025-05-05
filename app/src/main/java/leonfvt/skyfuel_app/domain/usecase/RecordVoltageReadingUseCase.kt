package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour enregistrer une mesure de tension d'une batterie
 */
class RecordVoltageReadingUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour enregistrer une mesure de tension
     * @param batteryId l'ID de la batterie
     * @param voltage la tension mesurée en volts
     * @param notes notes supplémentaires sur la mesure
     * @throws IllegalArgumentException si les valeurs sont invalides
     */
    suspend operator fun invoke(
        batteryId: Long,
        voltage: Float,
        notes: String = ""
    ) {
        // Validation de la tension
        if (voltage <= 0) {
            throw IllegalArgumentException("La tension doit être positive")
        }
        
        // Enregistrement de la mesure
        repository.recordVoltageReading(batteryId, voltage, notes)
    }
}