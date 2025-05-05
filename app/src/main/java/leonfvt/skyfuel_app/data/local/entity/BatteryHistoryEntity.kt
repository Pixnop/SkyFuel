package leonfvt.skyfuel_app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import java.time.LocalDateTime

/**
 * Entité Room pour la table d'historique des batteries
 */
@Entity(
    tableName = "battery_history",
    foreignKeys = [
        ForeignKey(
            entity = BatteryEntity::class,
            parentColumns = ["id"],
            childColumns = ["batteryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val batteryId: Long,
    val timestamp: LocalDateTime,
    val eventType: BatteryEventType,
    
    // Données spécifiques à l'événement
    val previousStatus: BatteryStatus? = null,
    val newStatus: BatteryStatus? = null,
    val voltage: Float? = null,
    val notes: String = "",
    val cycleNumber: Int? = null
) {
    /**
     * Convertit l'entité en modèle de domaine
     */
    fun toDomainModel(): BatteryHistory {
        return BatteryHistory(
            id = id,
            batteryId = batteryId,
            timestamp = timestamp,
            eventType = eventType,
            previousStatus = previousStatus,
            newStatus = newStatus,
            voltage = voltage,
            notes = notes,
            cycleNumber = cycleNumber
        )
    }
    
    companion object {
        /**
         * Crée une entité à partir du modèle de domaine
         */
        fun fromDomainModel(history: BatteryHistory): BatteryHistoryEntity {
            return BatteryHistoryEntity(
                id = history.id,
                batteryId = history.batteryId,
                timestamp = history.timestamp,
                eventType = history.eventType,
                previousStatus = history.previousStatus,
                newStatus = history.newStatus,
                voltage = history.voltage,
                notes = history.notes,
                cycleNumber = history.cycleNumber
            )
        }
    }
}