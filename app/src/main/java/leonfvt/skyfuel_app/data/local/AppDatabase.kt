package leonfvt.skyfuel_app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import leonfvt.skyfuel_app.data.local.converter.DateTimeConverters
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
import leonfvt.skyfuel_app.data.local.dao.CategoryDao
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.data.local.entity.BatteryEntity
import leonfvt.skyfuel_app.data.local.entity.BatteryHistoryEntity
import leonfvt.skyfuel_app.data.local.entity.BatteryCategoryCrossRef
import leonfvt.skyfuel_app.data.local.entity.CategoryEntity
import leonfvt.skyfuel_app.data.local.entity.ChargeReminderEntity
import leonfvt.skyfuel_app.data.local.migration.DatabaseMigrations

/**
 * Base de données principale de l'application SkyFuel.
 *
 * IMPORTANT sur les migrations :
 * - exportSchema = true permet de garder un historique des schémas dans /schemas
 * - Les migrations sont gérées explicitement via DatabaseMigrations
 * - Ne JAMAIS utiliser fallbackToDestructiveMigration() en production
 *
 * Pour ajouter une migration :
 * 1. Incrémenter la version ci-dessous
 * 2. Ajouter la migration dans DatabaseMigrations
 * 3. Tester la migration avec Room Testing
 */
@Database(
    entities = [
        BatteryEntity::class, 
        BatteryHistoryEntity::class, 
        ChargeReminderEntity::class,
        CategoryEntity::class,
        BatteryCategoryCrossRef::class
    ],
    version = 4,
    exportSchema = true
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

    /**
     * Récupère le DAO pour les rappels de charge
     */
    abstract fun chargeReminderDao(): ChargeReminderDao

    /**
     * Récupère le DAO pour les catégories
     */
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "skyfuel_database"

        // Singleton pour éviter plusieurs instances de la base de données
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Récupère l'instance de la base de données ou la crée si elle n'existe pas.
         *
         * @param context Le contexte de l'application
         * @param allowDestructiveMigration Si true, permet la migration destructive (DEV ONLY)
         */
        fun getDatabase(
            context: Context,
            allowDestructiveMigration: Boolean = false
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(*DatabaseMigrations.ALL_MIGRATIONS)

                // En développement uniquement, on peut permettre la migration destructive
                // En production, cela crashera si une migration manque (ce qui est le comportement souhaité)
                if (allowDestructiveMigration) {
                    builder.fallbackToDestructiveMigration()
                }

                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Réinitialise l'instance (utile pour les tests)
         */
        fun resetInstance() {
            INSTANCE = null
        }
    }
}