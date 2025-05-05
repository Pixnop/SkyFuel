package leonfvt.skyfuel_app.data.repository

import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.model.Battery
import leonfvt.skyfuel_app.data.model.BatteryHistory
import leonfvt.skyfuel_app.data.model.BatteryStatus
import java.time.LocalDate

/**
 * Interface defining the data layer repository operations
 */
interface BatteryRepository {
    fun getAllBatteries(): Flow<List<Battery>>
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>>
    fun searchBatteries(query: String): Flow<List<Battery>>
    suspend fun getBatteryById(id: Long): Battery?
    fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>>
    
    suspend fun addBattery(battery: Battery): Long
    suspend fun updateBattery(battery: Battery)
    suspend fun deleteBattery(battery: Battery)
    suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus, notes: String = "")
    suspend fun recordVoltageReading(batteryId: Long, voltage: Float, notes: String = "")
    suspend fun addBatteryNote(batteryId: Long, note: String)
    suspend fun recordMaintenance(batteryId: Long, description: String)
    
    suspend fun getTotalBatteryCount(): Int
    suspend fun getBatteryCountByStatus(status: BatteryStatus): Int
    suspend fun getAverageCycleCount(): Float
}