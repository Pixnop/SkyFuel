package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.BatteryStatistics
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour récupérer les statistiques des batteries
 */
class GetBatteryStatisticsUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour obtenir les statistiques
     * @return un objet contenant les statistiques des batteries
     */
    suspend operator fun invoke(): BatteryStatistics {
        val totalCount = repository.getTotalBatteryCount()
        val chargedCount = repository.getBatteryCountByStatus(BatteryStatus.CHARGED)
        val dischargedCount = repository.getBatteryCountByStatus(BatteryStatus.DISCHARGED)
        val storageCount = repository.getBatteryCountByStatus(BatteryStatus.STORAGE)
        val outOfServiceCount = repository.getBatteryCountByStatus(BatteryStatus.OUT_OF_SERVICE)
        val averageCycleCount = repository.getAverageCycleCount()
        
        return BatteryStatistics(
            totalCount = totalCount,
            chargedCount = chargedCount,
            dischargedCount = dischargedCount, 
            storageCount = storageCount,
            outOfServiceCount = outOfServiceCount,
            averageCycleCount = averageCycleCount
        )
    }
}