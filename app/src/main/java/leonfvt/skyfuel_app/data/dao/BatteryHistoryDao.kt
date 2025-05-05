package leonfvt.skyfuel_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.model.BatteryEventType
import leonfvt.skyfuel_app.data.model.BatteryHistory

/**
 * Interface d'accès aux données pour l'historique des batteries
 */
@Dao
interface BatteryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: BatteryHistory): Long
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId ORDER BY timestamp DESC")
    fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistory>>
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId AND eventType = :eventType ORDER BY timestamp DESC")
    fun getBatteryHistoryByEventType(batteryId: Long, eventType: BatteryEventType): Flow<List<BatteryHistory>>
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHistoryEntry(batteryId: Long): BatteryHistory?
    
    @Query("SELECT COUNT(*) FROM battery_history WHERE batteryId = :batteryId AND eventType = :eventType")
    suspend fun countEventsByType(batteryId: Long, eventType: BatteryEventType): Int
}