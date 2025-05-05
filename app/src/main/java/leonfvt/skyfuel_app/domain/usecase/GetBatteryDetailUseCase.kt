package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour récupérer les détails d'une batterie
 */
class GetBatteryDetailUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour récupérer les détails d'une batterie par son ID
     * @param batteryId l'ID de la batterie
     * @return la batterie correspondant à l'ID ou null si elle n'existe pas
     */
    suspend operator fun invoke(batteryId: Long): Battery? {
        return repository.getBatteryById(batteryId)
    }
}