package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour récupérer l'historique d'une batterie
 */
class GetBatteryHistoryUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour récupérer l'historique d'une batterie
     * @param batteryId l'ID de la batterie
     * @return un Flow de liste d'entrées d'historique pour cette batterie
     */
    operator fun invoke(batteryId: Long): Flow<List<BatteryHistory>> {
        return repository.getBatteryHistory(batteryId)
    }
}