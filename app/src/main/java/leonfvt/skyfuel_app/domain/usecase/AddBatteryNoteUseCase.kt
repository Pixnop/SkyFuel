package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour ajouter une note à une batterie
 */
class AddBatteryNoteUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour ajouter une note à une batterie
     * @param batteryId l'ID de la batterie
     * @param note la note à ajouter
     * @throws IllegalArgumentException si la note est vide
     */
    suspend operator fun invoke(
        batteryId: Long,
        note: String
    ) {
        // Validation de la note
        if (note.isBlank()) {
            throw IllegalArgumentException("La note ne peut pas être vide")
        }

        // Ajout de la note
        repository.addBatteryNote(batteryId, note)
    }
}
