package leonfvt.skyfuel_app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.local.entity.BatteryCategoryCrossRef
import leonfvt.skyfuel_app.data.local.entity.CategoryEntity

/**
 * DAO pour les opérations sur les catégories
 */
@Dao
interface CategoryDao {
    
    // ============ Opérations CRUD sur les catégories ============
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Update
    suspend fun updateCategory(category: CategoryEntity)
    
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>
    
    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?
    
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?
    
    // ============ Opérations sur les relations batterie-catégorie ============
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBatteryToCategory(crossRef: BatteryCategoryCrossRef)
    
    @Delete
    suspend fun removeBatteryFromCategory(crossRef: BatteryCategoryCrossRef)
    
    @Query("DELETE FROM battery_category_cross_ref WHERE batteryId = :batteryId")
    suspend fun removeAllCategoriesFromBattery(batteryId: Long)
    
    @Query("DELETE FROM battery_category_cross_ref WHERE categoryId = :categoryId")
    suspend fun removeAllBatteriesFromCategory(categoryId: Long)
    
    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN battery_category_cross_ref bcr ON c.id = bcr.categoryId
        WHERE bcr.batteryId = :batteryId
        ORDER BY c.name ASC
    """)
    fun getCategoriesForBattery(batteryId: Long): Flow<List<CategoryEntity>>
    
    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN battery_category_cross_ref bcr ON c.id = bcr.categoryId
        WHERE bcr.batteryId = :batteryId
        ORDER BY c.name ASC
    """)
    suspend fun getCategoriesForBatterySync(batteryId: Long): List<CategoryEntity>
    
    @Query("""
        SELECT COUNT(*) FROM battery_category_cross_ref
        WHERE categoryId = :categoryId
    """)
    fun getBatteryCountForCategory(categoryId: Long): Flow<Int>
    
    @Query("""
        SELECT bcr.batteryId FROM battery_category_cross_ref bcr
        WHERE bcr.categoryId = :categoryId
    """)
    fun getBatteryIdsForCategory(categoryId: Long): Flow<List<Long>>
    
    // ============ Transaction pour mise à jour des catégories d'une batterie ============
    
    @Transaction
    suspend fun updateBatteryCategories(batteryId: Long, categoryIds: List<Long>) {
        removeAllCategoriesFromBattery(batteryId)
        categoryIds.forEach { categoryId ->
            addBatteryToCategory(BatteryCategoryCrossRef(batteryId, categoryId))
        }
    }
}
