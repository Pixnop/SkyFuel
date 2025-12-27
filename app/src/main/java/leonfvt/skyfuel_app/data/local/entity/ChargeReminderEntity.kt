package leonfvt.skyfuel_app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import leonfvt.skyfuel_app.domain.model.ChargeReminder
import leonfvt.skyfuel_app.domain.model.ReminderType
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Entité Room pour stocker les rappels de charge
 */
@Entity(
    tableName = "charge_reminders",
    foreignKeys = [
        ForeignKey(
            entity = BatteryEntity::class,
            parentColumns = ["id"],
            childColumns = ["batteryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("batteryId")]
)
data class ChargeReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batteryId: Long,
    val title: String,
    val hour: Int,              // Heure (0-23)
    val minute: Int,            // Minute (0-59)
    val daysOfWeekBits: Int,    // Bitmask pour les jours (1=Lun, 2=Mar, 4=Mer, 8=Jeu, 16=Ven, 32=Sam, 64=Dim)
    val isEnabled: Boolean,
    val reminderType: ReminderType,
    val notes: String
) {
    fun toDomainModel(): ChargeReminder {
        return ChargeReminder(
            id = id,
            batteryId = batteryId,
            title = title,
            time = LocalTime.of(hour, minute),
            daysOfWeek = bitsToDay(daysOfWeekBits),
            isEnabled = isEnabled,
            reminderType = reminderType,
            notes = notes
        )
    }
    
    companion object {
        fun fromDomainModel(reminder: ChargeReminder): ChargeReminderEntity {
            return ChargeReminderEntity(
                id = reminder.id,
                batteryId = reminder.batteryId,
                title = reminder.title,
                hour = reminder.time.hour,
                minute = reminder.time.minute,
                daysOfWeekBits = daysToBits(reminder.daysOfWeek),
                isEnabled = reminder.isEnabled,
                reminderType = reminder.reminderType,
                notes = reminder.notes
            )
        }
        
        private fun daysToBits(days: Set<DayOfWeek>): Int {
            var bits = 0
            days.forEach { day ->
                bits = bits or (1 shl (day.value - 1))
            }
            return bits
        }
        
        private fun bitsToDay(bits: Int): Set<DayOfWeek> {
            val days = mutableSetOf<DayOfWeek>()
            DayOfWeek.values().forEach { day ->
                if ((bits and (1 shl (day.value - 1))) != 0) {
                    days.add(day)
                }
            }
            return days
        }
    }
}
