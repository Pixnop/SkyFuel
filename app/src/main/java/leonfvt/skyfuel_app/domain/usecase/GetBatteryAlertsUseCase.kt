package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.BatteryAlert
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.domain.service.AlertService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * UseCase pour récupérer les alertes de toutes les batteries
 */
class GetBatteryAlertsUseCase @Inject constructor(
    private val repository: BatteryRepository,
    private val alertService: AlertService
) {
    /**
     * Récupère les alertes en flux continu (se met à jour quand les batteries changent)
     */
    operator fun invoke(): Flow<List<BatteryAlert>> {
        return repository.getAllBatteries().map { batteries ->
            alertService.checkAllBatteriesAlerts(batteries)
        }
    }
    
    /**
     * Récupère les alertes pour une batterie spécifique
     */
    suspend fun forBattery(batteryId: Long): List<BatteryAlert> {
        val battery = repository.getBatteryById(batteryId) ?: return emptyList()
        return alertService.checkBatteryAlerts(battery)
    }
}
