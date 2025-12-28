package leonfvt.skyfuel_app.domain.model

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Couleurs prédéfinies pour les catégories
 */
object CategoryColors {
    val colors = listOf(
        0xFF4CAF50L, // Green
        0xFF2196F3L, // Blue
        0xFFFF9800L, // Orange
        0xFF9C27B0L, // Purple
        0xFFE91E63L, // Pink
        0xFF00BCD4L, // Cyan
        0xFFFF5722L, // Deep Orange
        0xFF607D8BL, // Blue Grey
        0xFF795548L, // Brown
        0xFF3F51B5L  // Indigo
    )
    
    fun getColor(index: Int): Long = colors[index % colors.size]
}

/**
 * Modèle de domaine représentant une catégorie de batterie
 */
@Stable
data class Category(
    val id: Long = 0,
    val name: String,
    val color: Long = CategoryColors.colors.first(),
    val icon: String = "folder", // Nom de l'icône Material
    val description: String = "",
    val batteryCount: Int = 0 // Nombre de batteries dans cette catégorie
) {
    companion object {
        val DEFAULT = Category(
            id = -1,
            name = "Non catégorisé",
            color = 0xFF9E9E9EL,
            icon = "folder"
        )
    }
}

/**
 * Extension pour obtenir la couleur Compose
 */
fun Category.getComposeColor(): Color = Color(this.color)
