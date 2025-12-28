package leonfvt.skyfuel_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import leonfvt.skyfuel_app.domain.model.Category

/**
 * Entité Room pour une catégorie de batterie
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Long,
    val icon: String,
    val description: String
) {
    fun toDomainModel(batteryCount: Int = 0): Category = Category(
        id = id,
        name = name,
        color = color,
        icon = icon,
        description = description,
        batteryCount = batteryCount
    )
    
    companion object {
        fun fromDomainModel(category: Category): CategoryEntity = CategoryEntity(
            id = if (category.id > 0) category.id else 0,
            name = category.name,
            color = category.color,
            icon = category.icon,
            description = category.description
        )
    }
}

/**
 * Table de jointure entre batteries et catégories (many-to-many)
 */
@Entity(
    tableName = "battery_category_cross_ref",
    primaryKeys = ["batteryId", "categoryId"]
)
data class BatteryCategoryCrossRef(
    val batteryId: Long,
    val categoryId: Long
)
