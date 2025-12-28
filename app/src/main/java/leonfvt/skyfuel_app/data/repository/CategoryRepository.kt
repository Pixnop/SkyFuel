package leonfvt.skyfuel_app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import leonfvt.skyfuel_app.data.local.dao.CategoryDao
import leonfvt.skyfuel_app.data.local.entity.BatteryCategoryCrossRef
import leonfvt.skyfuel_app.data.local.entity.CategoryEntity
import leonfvt.skyfuel_app.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    /**
     * Récupère toutes les catégories avec leur nombre de batteries
     */
    fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomainModel(0) }
        }
    }

    /**
     * Récupère une catégorie par son ID
     */
    suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomainModel(0)
    }

    /**
     * Récupère les catégories d'une batterie spécifique
     */
    fun getCategoriesForBattery(batteryId: Long): Flow<List<Category>> {
        return categoryDao.getCategoriesForBattery(batteryId).map { entities ->
            entities.map { it.toDomainModel(0) }
        }
    }

    /**
     * Crée une nouvelle catégorie
     */
    suspend fun createCategory(category: Category): Long {
        return categoryDao.insertCategory(CategoryEntity.fromDomainModel(category))
    }

    /**
     * Met à jour une catégorie existante
     */
    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(CategoryEntity.fromDomainModel(category))
    }

    /**
     * Supprime une catégorie
     */
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(CategoryEntity.fromDomainModel(category))
    }

    /**
     * Ajoute une batterie à une catégorie
     */
    suspend fun addBatteryToCategory(batteryId: Long, categoryId: Long) {
        categoryDao.addBatteryToCategory(BatteryCategoryCrossRef(batteryId, categoryId))
    }

    /**
     * Retire une batterie d'une catégorie
     */
    suspend fun removeBatteryFromCategory(batteryId: Long, categoryId: Long) {
        categoryDao.removeBatteryFromCategory(BatteryCategoryCrossRef(batteryId, categoryId))
    }

    /**
     * Met à jour les catégories d'une batterie
     */
    suspend fun updateBatteryCategories(batteryId: Long, categoryIds: List<Long>) {
        categoryDao.updateBatteryCategories(batteryId, categoryIds)
    }

    /**
     * Recherche des catégories par nom
     */
    fun searchCategories(query: String): Flow<List<Category>> {
        // Utilise getAllCategories et filtre côté client
        return categoryDao.getAllCategories().map { entities ->
            entities.filter { it.name.contains(query, ignoreCase = true) }
                .map { it.toDomainModel(0) }
        }
    }
}
