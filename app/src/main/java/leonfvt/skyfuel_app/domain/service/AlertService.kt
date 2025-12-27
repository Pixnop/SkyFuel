package leonfvt.skyfuel_app.domain.service

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryAlert
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service responsable de la détection des alertes sur les batteries
 */
@Singleton
class AlertService @Inject constructor() {
    
    companion object {
        // Seuils de configuration
        const val DISCHARGE_DAYS_WARNING = 7L
        const val HEALTH_WARNING_THRESHOLD = 20
        const val MAINTENANCE_INTERVAL_DAYS = 90L
        const val MAINTENANCE_INTERVAL_CYCLES = 50
        
        // Cycles recommandés par type de batterie
        val RECOMMENDED_MAX_CYCLES = mapOf(
            BatteryType.LIPO to 300,
            BatteryType.LI_ION to 500,
            BatteryType.NIMH to 800,
            BatteryType.LIFE to 1500,
            BatteryType.OTHER to 400
        )
    }
    
    /**
     * Analyse une batterie et retourne toutes les alertes applicables
     */
    fun checkBatteryAlerts(battery: Battery): List<BatteryAlert> {
        val alerts = mutableListOf<BatteryAlert>()
        
        // 1. Vérifier si la batterie doit être chargée
        checkNeedsCharging(battery)?.let { alerts.add(it) }
        
        // 2. Vérifier la santé de la batterie
        checkLowHealth(battery)?.let { alerts.add(it) }
        
        // 3. Vérifier la maintenance périodique
        checkMaintenanceDue(battery)?.let { alerts.add(it) }
        
        // 4. Vérifier le nombre de cycles
        checkHighCycleCount(battery)?.let { alerts.add(it) }
        
        return alerts
    }
    
    /**
     * Analyse une liste de batteries et retourne toutes les alertes
     */
    fun checkAllBatteriesAlerts(batteries: List<Battery>): List<BatteryAlert> {
        return batteries.flatMap { checkBatteryAlerts(it) }
            .sortedByDescending { it.priority.ordinal }
    }
    
    /**
     * Vérifie si la batterie doit être chargée
     */
    private fun checkNeedsCharging(battery: Battery): BatteryAlert? {
        if (battery.status != BatteryStatus.DISCHARGED) return null
        
        val lastDischargeDate = battery.lastUseDate ?: return null
        val daysSinceDischarge = ChronoUnit.DAYS.between(lastDischargeDate, LocalDate.now())
        
        return if (daysSinceDischarge >= DISCHARGE_DAYS_WARNING) {
            BatteryAlert.needsCharging(battery, daysSinceDischarge)
        } else {
            null
        }
    }
    
    /**
     * Vérifie si la santé de la batterie est faible
     */
    private fun checkLowHealth(battery: Battery): BatteryAlert? {
        val health = battery.getHealthPercentage()
        
        return if (health <= HEALTH_WARNING_THRESHOLD) {
            BatteryAlert.lowHealth(battery, health)
        } else {
            null
        }
    }
    
    /**
     * Vérifie si la maintenance est due
     */
    private fun checkMaintenanceDue(battery: Battery): BatteryAlert? {
        val daysSincePurchase = ChronoUnit.DAYS.between(battery.purchaseDate, LocalDate.now())
        val maintenanceIntervals = daysSincePurchase / MAINTENANCE_INTERVAL_DAYS
        val cycleIntervals = battery.cycleCount / MAINTENANCE_INTERVAL_CYCLES
        
        // Vérifier si on a passé un nouveau palier de maintenance
        val lastMaintenanceCheck = battery.lastChargeDate ?: battery.purchaseDate
        val daysSinceLastCheck = ChronoUnit.DAYS.between(lastMaintenanceCheck, LocalDate.now())
        
        return when {
            daysSinceLastCheck >= MAINTENANCE_INTERVAL_DAYS -> {
                BatteryAlert.maintenanceDue(
                    battery, 
                    "Dernière vérification il y a ${daysSinceLastCheck} jours"
                )
            }
            battery.cycleCount > 0 && battery.cycleCount % MAINTENANCE_INTERVAL_CYCLES == 0 -> {
                BatteryAlert.maintenanceDue(
                    battery,
                    "Palier de ${battery.cycleCount} cycles atteint"
                )
            }
            else -> null
        }
    }
    
    /**
     * Vérifie si le nombre de cycles est élevé
     */
    private fun checkHighCycleCount(battery: Battery): BatteryAlert? {
        val maxRecommended = RECOMMENDED_MAX_CYCLES[battery.type] ?: 400
        val warningThreshold = (maxRecommended * 0.8).toInt()
        
        return if (battery.cycleCount >= warningThreshold) {
            BatteryAlert.highCycleCount(battery, battery.cycleCount, maxRecommended)
        } else {
            null
        }
    }
}
