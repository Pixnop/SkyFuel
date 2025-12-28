package leonfvt.skyfuel_app.domain.usecase.category

import leonfvt.skyfuel_app.data.repository.CategoryRepository
import leonfvt.skyfuel_app.domain.model.Category
import javax.inject.Inject

/**
 * Use case pour créer une nouvelle catégorie
 */
class CreateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Long {
        return repository.createCategory(category)
    }
}
