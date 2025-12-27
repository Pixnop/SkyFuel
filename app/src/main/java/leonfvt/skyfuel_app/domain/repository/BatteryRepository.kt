package leonfvt.skyfuel_app.domain.repository

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryHistory
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import java.time.LocalDate

/**
 * Interface du repository pour la gestion des batteries
 * Définit les méthodes d'accès aux données sans spécifier leur implémentation
 */
interface BatteryRepository {
    // Récupération de données
    fun getAllBatteries(): Flow<List<Battery>>
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>>
    fun searchBatteries(query: String): Flow<List<Battery>>
    suspend fun getBatteryById(id: Long): Battery?
    fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>>
    
    // Opérations de modification
    suspend fun addBattery(battery: Battery): Long
    suspend fun updateBattery(battery: Battery)
    suspend fun deleteBattery(battery: Battery)
    suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String = "")
    suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String = "")
    suspend fun addBatteryNote(batteryId: Long, note: String)
    suspend fun recordMaintenance(batteryId: Long, description: String)
    
    // Statistiques
    suspend fun getTotalBatteryCount(): Int
    suspend fun getBatteryCountByStatus(status: BatteryStatus): Int
    suspend fun getAverageCycleCount(): Float
    
    // Export/Import
    suspend fun getAllHistory(): List<BatteryHistory>
    suspend fun deleteAllBatteries()
    suspend fun getBatteryBySerialNumber(serialNumber: String): Battery?
    suspend fun insertBattery(battery: Battery): Long
}