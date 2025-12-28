package leonfvt.skyfuel_app.domain.usecase.category

import leonfvt.skyfuel_app.data.repository.CategoryRepository
import javax.inject.Inject

/**
 * Use case pour mettre à jour les catégories d'une batterie
 */
class UpdateBatteryCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(batteryId: Long, categoryIds: List<Long>) {
        repository.updateBatteryCategories(batteryId, categoryIds)
    }
}
