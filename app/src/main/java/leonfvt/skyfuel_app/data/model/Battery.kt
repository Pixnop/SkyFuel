package leonfvt.skyfuel_app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Représente les différents types de batteries disponibles
 */
enum class BatteryType {
    LIPO,
    LI_ION,
    NIMH,
    LIFE,
    OTHER
}

/**
 * Représente les différents états dans lesquels peut se trouver une batterie
 */
enum class BatteryStatus {
    CHARGED,      // Chargée, prête à l'emploi
    DISCHARGED,   // Déchargée, nécessite une charge
    STORAGE,      // En stockage, niveau optimal pour stockage long terme
    OUT_OF_SERVICE // Hors service, à recycler
}

/**
 * Entité représentant une batterie de drone dans l'application
 */
@Entity(tableName = "batteries")
data class Battery(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Identifiants de la batterie
    val brand: String,
    val model: String,
    val serialNumber: String,
    
    // Caractéristiques techniques
    val type: BatteryType,
    val cells: Int, // Nombre de cellules
    val capacity: Int, // en mAh
    
    // Informations de gestion
    val purchaseDate: LocalDate,
    val status: BatteryStatus = BatteryStatus.CHARGED,
    val cycleCount: Int = 0,
    
    // Informations supplémentaires
    val notes: String = "",
    val lastUseDate: LocalDate? = null,
    val lastChargeDate: LocalDate? = null
)