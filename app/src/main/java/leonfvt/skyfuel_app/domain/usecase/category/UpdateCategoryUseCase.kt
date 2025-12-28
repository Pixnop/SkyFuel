package leonfvt.skyfuel_app.domain.usecase.category

import leonfvt.skyfuel_app.data.repository.CategoryRepository
import leonfvt.skyfuel_app.domain.model.Category
import javax.inject.Inject

/**
 * Use case pour mettre à jour une catégorie existante
 */
class UpdateCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category) {
        repository.updateCategory(category)
    }
}
