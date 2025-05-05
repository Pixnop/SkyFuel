package leonfvt.skyfuel_app.domain.usecase

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour rechercher des batteries
 */
class SearchBatteriesUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour rechercher des batteries
     * @param query la requête de recherche
     * @return un Flow de liste de batteries correspondant à la recherche
     */
    operator fun invoke(query: String): Flow<List<Battery>> {
        return repository.searchBatteries(query)
    }
}