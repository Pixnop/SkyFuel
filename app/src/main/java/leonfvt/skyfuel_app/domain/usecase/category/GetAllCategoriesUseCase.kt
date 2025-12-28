package leonfvt.skyfuel_app.domain.usecase.category

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.repository.CategoryRepository
import leonfvt.skyfuel_app.domain.model.Category
import javax.inject.Inject

/**
 * Use case pour récupérer toutes les catégories
 */
class GetAllCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> {
        return repository.getAllCategories()
    }
}
