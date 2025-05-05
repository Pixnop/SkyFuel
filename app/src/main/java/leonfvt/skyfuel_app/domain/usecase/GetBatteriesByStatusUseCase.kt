package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour récupérer les batteries par statut
 */
class GetBatteriesByStatusUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour récupérer les batteries par statut
     * @param status le statut à filtrer, peut être null pour obtenir toutes les batteries
     * @return un Flow de liste de batteries filtrées par statut
     */
    operator fun invoke(status: BatteryStatus?): Flow<List<Battery>> {
        return if (status != null) {
            repository.getBatteriesByStatus(status)
        } else {
            repository.getAllBatteries()
        }
    }
}