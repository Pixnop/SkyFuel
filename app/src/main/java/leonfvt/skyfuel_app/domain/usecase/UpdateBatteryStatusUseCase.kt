package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour mettre à jour le statut d'une batterie
 */
class UpdateBatteryStatusUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour mettre à jour le statut d'une batterie
     * @param batteryId l'ID de la batterie
     * @param newStatus le nouveau statut
     * @param notes notes supplémentaires sur le changement de statut
     */
    suspend operator fun invoke(
        batteryId: Long,
        newStatus: BatteryStatus,
        notes: String = ""
    ) {
        repository.updateBatteryStatus(batteryId, newStatus, notes)
    }
}