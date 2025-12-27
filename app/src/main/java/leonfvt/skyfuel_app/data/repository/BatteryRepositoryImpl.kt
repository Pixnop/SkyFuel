package leonfvt.skyfuel_app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
import leonfvt.skyfuel_app.data.local.entity.BatteryEntity
import leonfvt.skyfuel_app.data.local.entity.BatteryHistoryEntity
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Implémentation du repository de batteries utilisant Room comme source de données
 * Implémente directement l'interface du domaine pour simplifier l'architecture
 */
class BatteryRepositoryImpl @Inject constructor(
    private val batteryDao: BatteryDao,
    private val batteryHistoryDao: BatteryHistoryDao
) : BatteryRepository {
    
    override fun getAllBatteries(): Flow<List<Battery>> {
        return batteryDao.getAllBatteries().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>> {
        return batteryDao.getBatteriesByStatus(status).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun searchBatteries(query: String): Flow<List<Battery>> {
        return batteryDao.searchBatteries(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getBatteryById(id: Long): Battery? {
        return batteryDao.getBatteryById(id)?.toDomainModel()
    }
    
    override fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>> {
        return batteryHistoryDao.getBatteryHistory(batteryId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun addBattery(battery: Battery): Long {
        val entity = BatteryEntity.fromDomainModel(battery)
        val batteryId = batteryDao.insertBattery(entity)
        
        // Ajouter un événement d'historique pour la création
        val historyEntry = BatteryHistoryEntity(
            batteryId = batteryId,
            timestamp = LocalDateTime.now(),
            eventType = BatteryEventType.STATUS_CHANGE,
            previousStatus = null,
            newStatus = battery.status,
            voltage = null,
            notes = "Batterie ajoutée au système",
            cycleNumber = null
        )
        batteryHistoryDao.insertHistoryEntry(historyEntry)
        
        return batteryId
    }
    
    override suspend fun updateBattery(battery: Battery) {
        val oldBattery = batteryDao.getBatteryById(battery.id)
        val entity = BatteryEntity.fromDomainModel(battery)
        batteryDao.updateBattery(entity)
        
        // Si le statut a changé, enregistrer dans l'historique
        if (oldBattery != null && oldBattery.status != entity.status) {
            val historyEntry = BatteryHistoryEntity(
                batteryId = battery.id,
                timestamp = LocalDateTime.now(),
                eventType = BatteryEventType.STATUS_CHANGE,
                previousStatus = oldBattery.status,
                newStatus = entity.status,
                voltage = null,
                notes = "",
                cycleNumber = null
            )
            batteryHistoryDao.insertHistoryEntry(historyEntry)
        }
    }
    
    override suspend fun deleteBattery(battery: Battery) {
        val entity = BatteryEntity.fromDomainModel(battery)
        batteryDao.deleteBattery(entity)
    }
    
    override suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String) {
        val battery = batteryDao.getBatteryById(batteryId)
        
        if (battery != null) {
            val oldStatus = battery.status
            
            // Mise à jour du statut
            batteryDao.updateBatteryStatus(batteryId, newStatus)
            
            // Enregistrement dans l'historique
            val historyEntry = BatteryHistoryEntity(
                batteryId = batteryId,
                timestamp = LocalDateTime.now(),
                eventType = BatteryEventType.STATUS_CHANGE,
                previousStatus = oldStatus,
                newStatus = newStatus,
                voltage = null,
                notes = notes,
                cycleNumber = null
            )
            batteryHistoryDao.insertHistoryEntry(historyEntry)
            
            // Si la batterie passe de déchargée à chargée, incrémenter le compteur de cycles
            if (oldStatus == BatteryStatus.DISCHARGED && newStatus == BatteryStatus.CHARGED) {
                batteryDao.incrementCycleCount(batteryId)
                
                batteryHistoryDao.insertHistoryEntry(
                    BatteryHistoryEntity(
                        batteryId = batteryId,
                        timestamp = LocalDateTime.now(),
                        eventType = BatteryEventType.CYCLE_COMPLETED,
                        previousStatus = null,
                        newStatus = null,
                        voltage = null,
                        notes = "",
                        cycleNumber = (battery.cycleCount + 1)
                    )
                )
            }
        }
    }
    
    override suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String) {
        batteryHistoryDao.insertHistoryEntry(
            BatteryHistoryEntity(
                batteryId = batteryId,
                timestamp = LocalDateTime.now(),
                eventType = BatteryEventType.VOLTAGE_READING,
                previousStatus = null,
                newStatus = null,
                voltage = voltage,
                notes = notes,
                cycleNumber = null
            )
        )
    }
    
    override suspend fun addBatteryNote(batteryId: Long, note: String) {
        batteryHistoryDao.insertHistoryEntry(
            BatteryHistoryEntity(
                batteryId = batteryId,
                timestamp = LocalDateTime.now(),
                eventType = BatteryEventType.NOTE_ADDED,
                previousStatus = null,
                newStatus = null,
                voltage = null,
                notes = note,
                cycleNumber = null
            )
        )
    }
    
    override suspend fun recordMaintenance(batteryId: Long, description: String) {
        batteryHistoryDao.insertHistoryEntry(
            BatteryHistoryEntity(
                batteryId = batteryId,
                timestamp = LocalDateTime.now(),
                eventType = BatteryEventType.MAINTENANCE,
                previousStatus = null,
                newStatus = null,
                voltage = null,
                notes = description,
                cycleNumber = null
            )
        )
    }
    
    override suspend fun getTotalBatteryCount(): Int {
        return batteryDao.getTotalBatteryCount()
    }
    
    override suspend fun getBatteryCountByStatus(status: BatteryStatus): Int {
        return batteryDao.getBatteryCountByStatus(status)
    }
    
    override suspend fun getAverageCycleCount(): Float {
        return batteryDao.getAverageCycleCount()
    }
    
    override suspend fun getAllHistory(): List<BatteryHistory> {
        return batteryHistoryDao.getAllHistory().map { it.toDomainModel() }
    }
    
    override suspend fun deleteAllBatteries() {
        batteryHistoryDao.deleteAllHistory()
        batteryDao.deleteAllBatteries()
    }
    
    override suspend fun getBatteryBySerialNumber(serialNumber: String): Battery? {
        return batteryDao.getBatteryBySerialNumber(serialNumber)?.toDomainModel()
    }
    
    override suspend fun insertBattery(battery: Battery): Long {
        val entity = BatteryEntity.fromDomainModel(battery)
        return batteryDao.insertBattery(entity)
    }
}