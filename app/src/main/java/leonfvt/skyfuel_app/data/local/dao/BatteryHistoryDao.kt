package leonfvt.skyfuel_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.local.entity.BatteryHistoryEntity
import leonfvt.skyfuel_app.domain.model.BatteryEventType

/**
 * DAO pour accéder à la table d'historique des batteries
 */
@Dao
interface BatteryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: BatteryHistoryEntity): Long
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId ORDER BY timestamp DESC")
    fun getBatteryHistory(batteryId: Long): Flow<List<BatteryHistoryEntity>>
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId AND eventType = :eventType ORDER BY timestamp DESC")
    fun getBatteryHistoryByEventType(batteryId: Long, eventType: BatteryEventType): Flow<List<BatteryHistoryEntity>>
    
    @Query("SELECT * FROM battery_history WHERE batteryId = :batteryId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHistoryEntry(batteryId: Long): BatteryHistoryEntity?
    
    @Query("SELECT COUNT(*) FROM battery_history WHERE batteryId = :batteryId AND eventType = :eventType")
    suspend fun countEventsByType(batteryId: Long, eventType: BatteryEventType): Int
    
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<BatteryHistoryEntity>>
    
    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<BatteryHistoryEntity>
    
    @Query("DELETE FROM battery_history")
    suspend fun deleteAllHistory()
}