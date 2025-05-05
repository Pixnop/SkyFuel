package leonfvt.skyfuel_app.domain.usecase

import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Inject

/**
 * Use case pour supprimer une batterie
 */
class DeleteBatteryUseCase @Inject constructor(
    private val repository: BatteryRepository
) {
    /**
     * Exécute le use case pour supprimer une batterie
     * @param battery la batterie à supprimer
     */
    suspend operator fun invoke(battery: Battery) {
        repository.deleteBattery(battery)
    }
}