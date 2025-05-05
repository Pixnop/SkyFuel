package leonfvt.skyfuel_app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Type d'évènement enregistré dans l'historique des batteries
 */
enum class BatteryEventType {
    STATUS_CHANGE,     // Changement de statut (chargé, déchargé, stockage, etc.)
    CYCLE_COMPLETED,   // Cycle de charge/décharge complet
    VOLTAGE_READING,   // Relevé de tension
    NOTE_ADDED,        // Note ajoutée
    MAINTENANCE        // Opération de maintenance
}

/**
 * Représente une entrée dans l'historique d'une batterie
 */
@Entity(
    tableName = "battery_history",
    foreignKeys = [
        ForeignKey(
            entity = Battery::class,
            parentColumns = ["id"],
            childColumns = ["batteryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BatteryHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val batteryId: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val eventType: BatteryEventType,
    
    // Données spécifiques à l'événement
    val previousStatus: BatteryStatus? = null,
    val newStatus: BatteryStatus? = null,
    val voltage: Float? = null,      // Tension mesurée en volts
    val notes: String = "",          // Notes ou commentaires additionnels
    
    // Pour les cycles de charge
    val cycleNumber: Int? = null     // Numéro du cycle si applicable
)