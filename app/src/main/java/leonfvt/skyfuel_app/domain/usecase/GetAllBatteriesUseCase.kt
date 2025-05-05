package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour récupérer toutes les batteries
 */
class GetAllBatteriesUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour récupérer toutes les batteries
     * @return un Flow de liste de batteries
     */
    operator fun invoke(): Flow<List<Battery>> {
        return repository.getAllBatteries()
    }
}