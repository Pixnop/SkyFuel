package leonfvt.skyfuel_app.data.repository

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.model.Battery as DataBattery
import leonfvt.skyfuel_app.data.model.BatteryHistory as DataBatteryHistory
import leonfvt.skyfuel_app.data.model.BatteryStatus as DataBatteryStatus
import leonfvt.skyfuel_app.data.model.BatteryType as DataBatteryType
import leonfvt.skyfuel_app.domain.model.Battery as DomainBattery
import leonfvt.skyfuel_app.domain.model.BatteryHistory as DomainBatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus as DomainBatteryStatus
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * This wrapper class allows us to adapt the domain repository interface
 * to the data repository interface expected by older components.
 * This helps during the migration to the new Clean Architecture.
 */
class LegacyBatteryRepositoryWrapper @Inject constructor(
    private val domainRepository: leonfvt.skyfuel_app.domain.repository.BatteryRepository
) : BatteryRepository {

    // Helper function to convert domain Battery to data Battery
    private fun DomainBattery.toDataModel(): DataBattery {
        return DataBattery(
            id = this.id,
            brand = this.brand,
            model = this.model,
            serialNumber = this.serialNumber,
            type = this.type.toDataType(),
            cells = this.cells,
            capacity = this.capacity,
            purchaseDate = this.purchaseDate,
            status = this.status.toDataStatus(),
            cycleCount = this.cycleCount,
            notes = this.notes,
            lastUseDate = this.lastUseDate,
            lastChargeDate = this.lastChargeDate
        )
    }

    // Helper function to convert data Battery to domain Battery
    private fun DataBattery.toDomainModel(): DomainBattery {
        return DomainBattery(
            id = this.id,
            brand = this.brand,
            model = this.model,
            serialNumber = this.serialNumber,
            type = this.type.toDomainType(),
            cells = this.cells,
            capacity = this.capacity,
            purchaseDate = this.purchaseDate,
            status = this.status.toDomainStatus(),
            cycleCount = this.cycleCount,
            notes = this.notes,
            lastUseDate = this.lastUseDate,
            lastChargeDate = this.lastChargeDate
        )
    }

    // Helper function to convert domain BatteryHistory to data BatteryHistory
    private fun DomainBatteryHistory.toDataModel(): DataBatteryHistory {
        return DataBatteryHistory(
            id = this.id,
            batteryId = this.batteryId,
            timestamp = this.timestamp,
            eventType = when (this.eventType) {
                leonfvt.skyfuel_app.domain.model.BatteryEventType.STATUS_CHANGE -> leonfvt.skyfuel_app.data.model.BatteryEventType.STATUS_CHANGE
                leonfvt.skyfuel_app.domain.model.BatteryEventType.CYCLE_COMPLETED -> leonfvt.skyfuel_app.data.model.BatteryEventType.CYCLE_COMPLETED
                leonfvt.skyfuel_app.domain.model.BatteryEventType.VOLTAGE_READING -> leonfvt.skyfuel_app.data.model.BatteryEventType.VOLTAGE_READING
                leonfvt.skyfuel_app.domain.model.BatteryEventType.NOTE_ADDED -> leonfvt.skyfuel_app.data.model.BatteryEventType.NOTE_ADDED
                leonfvt.skyfuel_app.domain.model.BatteryEventType.MAINTENANCE -> leonfvt.skyfuel_app.data.model.BatteryEventType.MAINTENANCE
            },
            previousStatus = this.previousStatus?.toDataStatus(),
            newStatus = this.newStatus?.toDataStatus(),
            voltage = this.voltage,
            notes = this.notes,
            cycleNumber = this.cycleNumber
        )
    }

    // Helper function to convert domain BatteryStatus to data BatteryStatus
    private fun DomainBatteryStatus.toDataStatus(): DataBatteryStatus {
        return when (this) {
            DomainBatteryStatus.CHARGED -> DataBatteryStatus.CHARGED
            DomainBatteryStatus.DISCHARGED -> DataBatteryStatus.DISCHARGED
            DomainBatteryStatus.STORAGE -> DataBatteryStatus.STORAGE
            DomainBatteryStatus.OUT_OF_SERVICE -> DataBatteryStatus.OUT_OF_SERVICE
        }
    }

    // Helper function to convert data BatteryStatus to domain BatteryStatus
    private fun DataBatteryStatus.toDomainStatus(): DomainBatteryStatus {
        return when (this) {
            DataBatteryStatus.CHARGED -> DomainBatteryStatus.CHARGED
            DataBatteryStatus.DISCHARGED -> DomainBatteryStatus.DISCHARGED
            DataBatteryStatus.STORAGE -> DomainBatteryStatus.STORAGE
            DataBatteryStatus.OUT_OF_SERVICE -> DomainBatteryStatus.OUT_OF_SERVICE
        }
    }

    // Helper function to convert domain BatteryType to data BatteryType
    private fun leonfvt.skyfuel_app.domain.model.BatteryType.toDataType(): DataBatteryType {
        return when (this) {
            leonfvt.skyfuel_app.domain.model.BatteryType.LIPO -> DataBatteryType.LIPO
            leonfvt.skyfuel_app.domain.model.BatteryType.LI_ION -> DataBatteryType.LI_ION
            leonfvt.skyfuel_app.domain.model.BatteryType.NIMH -> DataBatteryType.NIMH
            leonfvt.skyfuel_app.domain.model.BatteryType.LIFE -> DataBatteryType.LIFE
            leonfvt.skyfuel_app.domain.model.BatteryType.OTHER -> DataBatteryType.OTHER
        }
    }

    // Helper function to convert data BatteryType to domain BatteryType
    private fun DataBatteryType.toDomainType(): leonfvt.skyfuel_app.domain.model.BatteryType {
        return when (this) {
            DataBatteryType.LIPO -> leonfvt.skyfuel_app.domain.model.BatteryType.LIPO
            DataBatteryType.LI_ION -> leonfvt.skyfuel_app.domain.model.BatteryType.LI_ION
            DataBatteryType.NIMH -> leonfvt.skyfuel_app.domain.model.BatteryType.NIMH
            DataBatteryType.LIFE -> leonfvt.skyfuel_app.domain.model.BatteryType.LIFE
            DataBatteryType.OTHER -> leonfvt.skyfuel_app.domain.model.BatteryType.OTHER
        }
    }

    override fun getAllBatteries(): Flow<List<DataBattery>> {
        return domainRepository.getAllBatteries().map { domainBatteries ->
            domainBatteries.map { it.toDataModel() }
        }
    }

    override fun getBatteriesByStatus(status: DataBatteryStatus): Flow<List<DataBattery>> {
        return domainRepository.getBatteriesByStatus(status.toDomainStatus()).map { domainBatteries ->
            domainBatteries.map { it.toDataModel() }
        }
    }

    override fun searchBatteries(query: String): Flow<List<DataBattery>> {
        return domainRepository.searchBatteries(query).map { domainBatteries ->
            domainBatteries.map { it.toDataModel() }
        }
    }

    override suspend fun getBatteryById(id: Long): DataBattery? {
        return domainRepository.getBatteryById(id)?.toDataModel()
    }

    override fun getBatteryHistory(batteryId: Long): Flow<List<DataBatteryHistory>> {
        return domainRepository.getBatteryHistory(batteryId).map { historyEntries ->
            historyEntries.map { it.toDataModel() }
        }
    }

    override suspend fun addBattery(battery: DataBattery): Long {
        return domainRepository.addBattery(battery.toDomainModel())
    }

    override suspend fun updateBattery(battery: DataBattery) {
        domainRepository.updateBattery(battery.toDomainModel())
    }

    override suspend fun deleteBattery(battery: DataBattery) {
        domainRepository.deleteBattery(battery.toDomainModel())
    }

    override suspend fun updateBatteryStatus(batteryId: Long, newStatus: DataBatteryStatus, notes: String) {
        domainRepository.updateBatteryStatus(batteryId, newStatus.toDomainStatus(), notes)
    }

    override suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String) {
        domainRepository.recordVoltageReading(batteryId, voltage, notes)
    }

    override suspend fun addBatteryNote(batteryId: Long, note: String) {
        domainRepository.addBatteryNote(batteryId, note)
    }

    override suspend fun recordMaintenance(batteryId: Long, description: String) {
        domainRepository.recordMaintenance(batteryId, description)
    }

    override suspend fun getTotalBatteryCount(): Int {
        return domainRepository.getTotalBatteryCount()
    }

    override suspend fun getBatteryCountByStatus(status: DataBatteryStatus): Int {
        return domainRepository.getBatteryCountByStatus(status.toDomainStatus())
    }

    override suspend fun getAverageCycleCount(): Float {
        return domainRepository.getAverageCycleCount()
    }
}