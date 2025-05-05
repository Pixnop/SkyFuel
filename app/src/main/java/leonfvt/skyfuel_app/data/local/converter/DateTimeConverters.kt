package leonfvt.skyfuel_app.data.local.converter

import androidx.room.TypeConverter
import leonfvt.skyfuel_app.domain.model.BatteryEventType
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.model.BatteryType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Classe de convertisseurs pour Room permettant de stocker des types complexes
 * dans la base de données SQLite.
 */
class DateTimeConverters {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    // LocalDate conversions
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }
    
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            LocalDate.parse(dateString, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }
    
    // LocalDateTime conversions
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }
    
    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        if (dateTimeString.isNullOrEmpty()) return null
        return try {
            LocalDateTime.parse(dateTimeString, dateTimeFormatter)
        } catch (e: Exception) {
            null
        }
    }
    
    // BatteryType conversions
    @TypeConverter
    fun fromBatteryType(type: BatteryType?): String? {
        return type?.name
    }
    
    @TypeConverter
    fun toBatteryType(name: String?): BatteryType? {
        if (name.isNullOrEmpty()) return null
        return try {
            BatteryType.valueOf(name)
        } catch (e: Exception) {
            BatteryType.OTHER
        }
    }
    
    // BatteryStatus conversions
    @TypeConverter
    fun fromBatteryStatus(status: BatteryStatus?): String? {
        return status?.name
    }
    
    @TypeConverter
    fun toBatteryStatus(name: String?): BatteryStatus? {
        if (name.isNullOrEmpty()) return null
        return try {
            BatteryStatus.valueOf(name)
        } catch (e: Exception) {
            null
        }
    }
    
    // BatteryEventType conversions
    @TypeConverter
    fun fromBatteryEventType(eventType: BatteryEventType?): String? {
        return eventType?.name
    }
    
    @TypeConverter
    fun toBatteryEventType(name: String?): BatteryEventType? {
        if (name.isNullOrEmpty()) return null
        return try {
            BatteryEventType.valueOf(name)
        } catch (e: Exception) {
            null
        }
    }
}