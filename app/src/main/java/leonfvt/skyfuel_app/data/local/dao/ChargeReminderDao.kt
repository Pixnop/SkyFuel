package leonfvt.skyfuel_app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import leonfvt.skyfuel_app.data.local.entity.ChargeReminderEntity

/**
 * DAO pour la gestion des rappels de charge
 */
@Dao
interface ChargeReminderDao {
    
    @Query("SELECT * FROM charge_reminders ORDER BY hour, minute")
    fun getAllReminders(): Flow<List<ChargeReminderEntity>>
    
    @Query("SELECT * FROM charge_reminders WHERE batteryId = :batteryId ORDER BY hour, minute")
    fun getRemindersByBattery(batteryId: Long): Flow<List<ChargeReminderEntity>>
    
    @Query("SELECT * FROM charge_reminders WHERE isEnabled = 1 ORDER BY hour, minute")
    fun getEnabledReminders(): Flow<List<ChargeReminderEntity>>
    
    @Query("SELECT * FROM charge_reminders WHERE isEnabled = 1")
    suspend fun getEnabledRemindersSync(): List<ChargeReminderEntity>
    
    @Query("SELECT * FROM charge_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ChargeReminderEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ChargeReminderEntity): Long
    
    @Update
    suspend fun updateReminder(reminder: ChargeReminderEntity)
    
    @Delete
    suspend fun deleteReminder(reminder: ChargeReminderEntity)
    
    @Query("DELETE FROM charge_reminders WHERE batteryId = :batteryId")
    suspend fun deleteRemindersByBattery(batteryId: Long)
    
    @Query("UPDATE charge_reminders SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setReminderEnabled(id: Long, isEnabled: Boolean)
}
