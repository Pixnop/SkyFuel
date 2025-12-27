package leonfvt.skyfuel_app.domain.model

import java.time.LocalDateTime

/**
 * Types d'alertes pour les batteries
 */
enum class AlertType {
    /** Batterie déchargée depuis trop longtemps (>7 jours) - risque de dégradation */
    NEEDS_CHARGING,
    
    /** Santé de la batterie faible (<20%) - remplacement recommandé */
    LOW_HEALTH,
    
    /** Maintenance périodique nécessaire (tous les 3 mois ou 50 cycles) */
    MAINTENANCE_DUE,
    
    /** Nombre de cycles élevé - surveiller la batterie */
    HIGH_CYCLE_COUNT
}

/**
 * Priorité d'une alerte
 */
enum class AlertPriority {
    LOW,      // Information
    MEDIUM,   // Attention requise
    HIGH,     // Action nécessaire
    CRITICAL  // Action urgente
}

/**
 * Représente une alerte concernant une batterie
 */
data class BatteryAlert(
    val id: Long = 0,
    val batteryId: Long,
    val batteryName: String,  // "brand model" pour affichage
    val type: AlertType,
    val priority: AlertPriority,
    val title: String,
    val message: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val isRead: Boolean = false,
    val isDismissed: Boolean = false
) {
    companion object {
        /**
         * Crée une alerte "batterie à charger"
         */
        fun needsCharging(battery: Battery, daysSinceDischarge: Long): BatteryAlert {
            return BatteryAlert(
                batteryId = battery.id,
                batteryName = "${battery.brand} ${battery.model}",
                type = AlertType.NEEDS_CHARGING,
                priority = if (daysSinceDischarge > 14) AlertPriority.HIGH else AlertPriority.MEDIUM,
                title = "Batterie à charger",
                message = "La batterie ${battery.brand} ${battery.model} est déchargée depuis $daysSinceDischarge jours. " +
                        "Rechargez-la pour éviter la dégradation."
            )
        }
        
        /**
         * Crée une alerte "santé faible"
         */
        fun lowHealth(battery: Battery, healthPercentage: Int): BatteryAlert {
            val priority = when {
                healthPercentage < 10 -> AlertPriority.CRITICAL
                healthPercentage < 15 -> AlertPriority.HIGH
                else -> AlertPriority.MEDIUM
            }
            
            return BatteryAlert(
                batteryId = battery.id,
                batteryName = "${battery.brand} ${battery.model}",
                type = AlertType.LOW_HEALTH,
                priority = priority,
                title = "Santé batterie faible",
                message = "La batterie ${battery.brand} ${battery.model} n'a plus que $healthPercentage% de santé. " +
                        "Envisagez son remplacement."
            )
        }
        
        /**
         * Crée une alerte "maintenance requise"
         */
        fun maintenanceDue(battery: Battery, reason: String): BatteryAlert {
            return BatteryAlert(
                batteryId = battery.id,
                batteryName = "${battery.brand} ${battery.model}",
                type = AlertType.MAINTENANCE_DUE,
                priority = AlertPriority.LOW,
                title = "Maintenance recommandée",
                message = "La batterie ${battery.brand} ${battery.model} nécessite une maintenance : $reason"
            )
        }
        
        /**
         * Crée une alerte "cycles élevés"
         */
        fun highCycleCount(battery: Battery, cycleCount: Int, maxRecommended: Int): BatteryAlert {
            val percentageOver = ((cycleCount.toFloat() / maxRecommended) * 100).toInt()
            val priority = when {
                percentageOver > 120 -> AlertPriority.HIGH
                percentageOver > 100 -> AlertPriority.MEDIUM
                else -> AlertPriority.LOW
            }
            
            return BatteryAlert(
                batteryId = battery.id,
                batteryName = "${battery.brand} ${battery.model}",
                type = AlertType.HIGH_CYCLE_COUNT,
                priority = priority,
                title = "Cycles élevés",
                message = "La batterie ${battery.brand} ${battery.model} a $cycleCount cycles " +
                        "(recommandé max: $maxRecommended). Surveillez ses performances."
            )
        }
    }
}
