package leonfvt.skyfuel_app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import leonfvt.skyfuel_app.BuildConfig
import leonfvt.skyfuel_app.data.local.AppDatabase
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.data.repository.BatteryRepositoryImpl
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import javax.inject.Singleton

/**
 * Module Hilt principal pour les dépendances de l'application
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fournit l'instance de la base de données.
     *
     * En mode DEBUG, permet la migration destructive pour faciliter le développement.
     * En RELEASE, les migrations doivent être explicitement définies dans DatabaseMigrations.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(
            context = context,
            allowDestructiveMigration = BuildConfig.DEBUG
        )
    }
    
    /**
     * Fournit le DAO pour les batteries
     */
    @Provides
    @Singleton
    fun provideBatteryDao(appDatabase: AppDatabase): BatteryDao {
        return appDatabase.batteryDao()
    }
    
    /**
     * Fournit le DAO pour l'historique des batteries
     */
    @Provides
    @Singleton
    fun provideBatteryHistoryDao(appDatabase: AppDatabase): BatteryHistoryDao {
        return appDatabase.batteryHistoryDao()
    }
    
    /**
     * Fournit le DAO pour les rappels de charge
     */
    @Provides
    @Singleton
    fun provideChargeReminderDao(appDatabase: AppDatabase): ChargeReminderDao {
        return appDatabase.chargeReminderDao()
    }
    
    /**
     * Fournit le repository pour les batteries
     * Nous utilisons seulement l'interface du domaine (BatteryRepository) comme dépendance
     * pour tous les composants qui en ont besoin, y compris les viewmodels et les usecases.
     */
    @Provides
    @Singleton
    fun provideBatteryRepository(
        batteryDao: BatteryDao,
        batteryHistoryDao: BatteryHistoryDao
    ): BatteryRepository {
        return BatteryRepositoryImpl(batteryDao, batteryHistoryDao)
    }
}