package leonfvt.skyfuel_app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.model.Battery
import leonfvt.skyfuel_app.data.model.BatteryStatus

/**
 * Interface d'accès aux données pour les batteries
 */
@Dao
interface BatteryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBattery(battery: Battery): Long
    
    @Update
    suspend fun updateBattery(battery: Battery)
    
    @Delete
    suspend fun deleteBattery(battery: Battery)
    
    @Query("SELECT * FROM batteries WHERE id = :batteryId")
    suspend fun getBatteryById(batteryId: Long): Battery?
    
    @Query("SELECT * FROM batteries ORDER BY brand, model")
    fun getAllBatteries(): Flow<List<Battery>>
    
    @Query("SELECT * FROM batteries WHERE status = :status ORDER BY brand, model")
    fun getBatteriesByStatus(status: BatteryStatus): Flow<List<Battery>>
    
    @Query("SELECT * FROM batteries WHERE brand LIKE '%' || :searchQuery || '%' OR model LIKE '%' || :searchQuery || '%' OR serialNumber LIKE '%' || :searchQuery || '%'")
    fun searchBatteries(searchQuery: String): Flow<List<Battery>>
    
    @Query("UPDATE batteries SET cycleCount = cycleCount + 1 WHERE id = :batteryId")
    suspend fun incrementCycleCount(batteryId: Long)
    
    @Query("UPDATE batteries SET status = :newStatus WHERE id = :batteryId")
    suspend fun updateBatteryStatus(batteryId: Long, newStatus: BatteryStatus)
}