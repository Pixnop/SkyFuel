package leonfvt.skyfuel_app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Objet contenant toutes les migrations de la base de données.
 *
 * Chaque migration doit être définie comme une val avec le pattern MIGRATION_X_Y
 * où X est la version source et Y la version cible.
 *
 * IMPORTANT: Ne jamais utiliser fallbackToDestructiveMigration() en production
 * car cela supprime toutes les données utilisateur.
 */
object DatabaseMigrations {

    // =========================================================================
    // MIGRATIONS
    // =========================================================================

    /**
     * Migration de la version 1 vers la version 2.
     * Ajoute la table charge_reminders pour les rappels de charge programmables.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS charge_reminders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    batteryId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    hour INTEGER NOT NULL,
                    minute INTEGER NOT NULL,
                    daysOfWeekBits INTEGER NOT NULL,
                    isEnabled INTEGER NOT NULL,
                    reminderType TEXT NOT NULL,
                    notes TEXT NOT NULL,
                    FOREIGN KEY (batteryId) REFERENCES batteries(id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            database.execSQL("CREATE INDEX IF NOT EXISTS index_charge_reminders_batteryId ON charge_reminders(batteryId)")
        }
    }
    
    /**
     * Liste de toutes les migrations disponibles.
     * À utiliser dans Room.databaseBuilder().addMigrations(*ALL_MIGRATIONS)
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2,
    )
}
