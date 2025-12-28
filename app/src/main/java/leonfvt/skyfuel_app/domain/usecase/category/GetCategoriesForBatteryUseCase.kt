package leonfvt.skyfuel_app.domain.usecase.category

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.repository.CategoryRepository
import leonfvt.skyfuel_app.domain.model.Category
import javax.inject.Inject

/**
 * Use case pour récupérer les catégories d'une batterie
 */
class GetCategoriesForBatteryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(batteryId: Long): Flow<List<Category>> {
        return repository.getCategoriesForBattery(batteryId)
    }
}
