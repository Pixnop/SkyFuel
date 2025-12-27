package leonfvt.skyfuel_app.domain.model

import androidx.compose.runtime.Stable
import java.time.LocalDateTime

/**
 * Modèle de domaine représentant une entrée d'historique de batterie
 * @Stable indique à Compose que cette classe est immuable pour optimiser les recompositions
 */
@Stable
data class BatteryHistory(
    val id: Long = 0,
    val batteryId: Long,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val eventType: BatteryEventType,
    
    // Données spécifiques à l'événement
    val previousStatus: BatteryStatus? = null,
    val newStatus: BatteryStatus? = null,
    val voltage: Float? = null,      // Tension mesurée en volts
    val notes: String = "",          // Notes ou commentaires additionnels
    val cycleNumber: Int? = null     // Numéro du cycle si applicable
) {
    /**
     * Crée une description de l'événement en fonction de son type
     */
    fun getDescription(): String {
        return when (eventType) {
            BatteryEventType.STATUS_CHANGE -> {
                if (previousStatus != null && newStatus != null) {
                    "Changement de statut: ${previousStatus.name} → ${newStatus.name}"
                } else {
                    "Batterie ajoutée"
                }
            }
            BatteryEventType.CYCLE_COMPLETED -> "Cycle #$cycleNumber complété"
            BatteryEventType.VOLTAGE_READING -> "Relevé de tension: ${voltage}V"
            BatteryEventType.NOTE_ADDED -> "Note ajoutée"
            BatteryEventType.MAINTENANCE -> "Maintenance effectuée"
        }
    }
    
    /**
     * Détermine si cet événement est positif, négatif ou neutre
     * pour la santé de la batterie
     */
    fun getEventImpact(): EventImpact {
        return when (eventType) {
            BatteryEventType.STATUS_CHANGE -> {
                when (newStatus) {
                    BatteryStatus.CHARGED -> EventImpact.NEUTRAL
                    BatteryStatus.DISCHARGED -> EventImpact.NEUTRAL
                    BatteryStatus.STORAGE -> EventImpact.POSITIVE
                    BatteryStatus.OUT_OF_SERVICE -> EventImpact.NEGATIVE
                    null -> EventImpact.NEUTRAL
                }
            }
            BatteryEventType.CYCLE_COMPLETED -> EventImpact.NEGATIVE
            BatteryEventType.VOLTAGE_READING -> EventImpact.NEUTRAL
            BatteryEventType.NOTE_ADDED -> EventImpact.NEUTRAL
            BatteryEventType.MAINTENANCE -> EventImpact.POSITIVE
        }
    }
}

/**
 * Impact d'un événement sur la batterie
 */
enum class EventImpact {
    POSITIVE, // Bon pour la batterie
    NEUTRAL,  // Sans impact
    NEGATIVE  // Réduit la durée de vie
}