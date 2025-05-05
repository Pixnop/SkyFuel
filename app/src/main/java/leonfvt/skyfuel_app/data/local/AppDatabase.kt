package leonfvt.skyfuel_app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import leonfvt.skyfuel_app.data.local.converter.DateTimeConverters
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
import leonfvt.skyfuel_app.data.local.entity.BatteryEntity
import leonfvt.skyfuel_app.data.local.entity.BatteryHistoryEntity

/**
 * Base de données principale de l'application SkyFuel
 */
@Database(
    entities = [BatteryEntity::class, BatteryHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Récupère le DAO pour les batteries
     */
    abstract fun batteryDao(): BatteryDao
    
    /**
     * Récupère le DAO pour l'historique des batteries
     */
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    
    companion object {
        // Singleton pour éviter plusieurs instances de la base de données
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Récupère l'instance de la base de données ou la crée si elle n'existe pas
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "skyfuel_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}