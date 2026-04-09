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
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
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
            
            db.execSQL("CREATE INDEX IF NOT EXISTS index_charge_reminders_batteryId ON charge_reminders(batteryId)")
        }
    }

    /**
     * Migration de la version 2 vers la version 3.
     * Ajoute les tables pour les catégories et tags de batteries.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Table des catégories
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    icon TEXT NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
            
            // Table de liaison many-to-many batteries <-> catégories
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS battery_category_cross_ref (
                    batteryId INTEGER NOT NULL,
                    categoryId INTEGER NOT NULL,
                    PRIMARY KEY (batteryId, categoryId),
                    FOREIGN KEY (batteryId) REFERENCES batteries(id) ON DELETE CASCADE,
                    FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Index pour performances
            db.execSQL("CREATE INDEX IF NOT EXISTS index_battery_category_cross_ref_batteryId ON battery_category_cross_ref(batteryId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_battery_category_cross_ref_categoryId ON battery_category_cross_ref(categoryId)")
        }
    }
    
    /**
     * Migration de la version 3 vers la version 4.
     * Ajoute le champ photoPath pour les photos de batteries.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Ajoute la colonne photoPath à la table batteries
            db.execSQL("ALTER TABLE batteries ADD COLUMN photoPath TEXT DEFAULT NULL")
        }
    }
    
    /**
     * Migration de la version 4 vers la version 5.
     * Ajoute l'index manquant sur batteryId dans battery_history et
     * supprime la colonne createdAt de categories (schema mismatch fix).
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Index sur batteryId dans battery_history pour éviter les full table scans
            db.execSQL("CREATE INDEX IF NOT EXISTS index_battery_history_batteryId ON battery_history(batteryId)")

            // Fix schema mismatch: recréer la table categories sans createdAt
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS categories_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    color INTEGER NOT NULL,
                    icon TEXT NOT NULL,
                    description TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("INSERT INTO categories_new (id, name, color, icon, description) SELECT id, name, color, icon, description FROM categories")
            db.execSQL("DROP TABLE categories")
            db.execSQL("ALTER TABLE categories_new RENAME TO categories")
        }
    }

    /**
     * Liste de toutes les migrations disponibles.
     * À utiliser dans Room.databaseBuilder().addMigrations(*ALL_MIGRATIONS)
     */
    val ALL_MIGRATIONS: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
    )
}
