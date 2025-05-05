package leonfvt.skyfuel_app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import leonfvt.skyfuel_app.data.local.AppDatabase
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.BatteryHistoryDao
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
     * Fournit l'instance de la base de données
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
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
     * Fournit le repository pour les batteries (domain layer)
     */
    @Provides
    @Singleton
    fun provideBatteryRepository(
        batteryDao: BatteryDao,
        batteryHistoryDao: BatteryHistoryDao
    ): leonfvt.skyfuel_app.domain.repository.BatteryRepository {
        return BatteryRepositoryImpl(batteryDao, batteryHistoryDao)
    }
    
    /**
     * Fournit le repository pour les batteries (data layer alias)
     */
    @Provides
    @Singleton
    fun provideDataBatteryRepository(
        repository: leonfvt.skyfuel_app.domain.repository.BatteryRepository
    ): leonfvt.skyfuel_app.data.repository.BatteryRepository {
        return leonfvt.skyfuel_app.data.repository.LegacyBatteryRepositoryWrapper(repository)
    }
}