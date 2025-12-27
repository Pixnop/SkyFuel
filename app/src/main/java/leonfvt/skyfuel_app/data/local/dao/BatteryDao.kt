package leonfvt.skyfuel_app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.local.entity.BatteryEntity
import leonfvt.skyfuel_app.domain.model.BatteryStatus

/**
 * DAO pour accéder à la table des batteries
 */
@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattery(battery: BatteryEntity): Long
    
    @Update
    suspend fun updateBattery(battery: BatteryEntity)
    
    @Delete
    suspend fun deleteBattery(battery: BatteryEntity)
    
    @Query("SELECT * FROM batteries WHERE id = :batteryId")
    suspend fun getBatteryById(batteryId: Long): BatteryEntity?
    
    @Query("SELECT * FROM batteries ORDER BY brand, model")
    fun getAllBatteries(): Flow<List<BatteryEntity>>
    
    @Query("SELECT * FROM batteries WHERE status = :status ORDER BY brand, model")
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<BatteryEntity>>
    
    @Query("SELECT * FROM batteries WHERE brand LIKE '%' || :searchQuery || '%' OR model LIKE '%' || :searchQuery || '%' OR serialNumber LIKE '%' || :searchQuery || '%' OR notes LIKE '%' || :searchQuery || '%'")
    fun searchBatteries(searchQuery: String): Flow<List<BatteryEntity>>
    
    @Query("UPDATE batteries SET cycleCount = cycleCount + 1 WHERE id = :batteryId")
    suspend fun incrementCycleCount(batteryId: Long)
    
    @Query("UPDATE batteries SET status = :newStatus WHERE id = :batteryId")
    suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus)
    
    @Query("SELECT COUNT(*) FROM batteries")
    suspend fun getTotalBatteryCount(): Int
    
    @Query("SELECT COUNT(*) FROM batteries WHERE status = :status")
    suspend fun getBatteryCountByStatus(status: BatteryStatus): Int
    
    @Query("SELECT AVG(cycleCount) FROM batteries")
    suspend fun getAverageCycleCount(): Float
    
    @Query("DELETE FROM batteries")
    suspend fun deleteAllBatteries()
    
    @Query("SELECT * FROM batteries WHERE serialNumber = :serialNumber LIMIT 1")
    suspend fun getBatteryBySerialNumber(serialNumber: String): BatteryEntity?
}