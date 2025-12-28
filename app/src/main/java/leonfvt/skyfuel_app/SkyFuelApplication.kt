package leonfvt.skyfuel_app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import leonfvt.skyfuel_app.data.sync.SyncManager
import leonfvt.skyfuel_app.util.NotificationHelper
import leonfvt.skyfuel_app.worker.BatteryAlertWorker
import leonfvt.skyfuel_app.worker.ReminderWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * Classe d'application principale pour SkyFuel.
 *
 * L'annotation HiltAndroidApp est nécessaire pour l'initialisation de Hilt
 * et la génération du code d'injection de dépendances.
 */
@HiltAndroidApp
class SkyFuelApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        initializeTimber()
        initializeThreeTenABP()
        initializeNotifications()
        scheduleBatteryAlertWorker()
        initializeSyncManager()
    }

    /**
     * Initialise le gestionnaire de synchronisation
     */
    private fun initializeSyncManager() {
        syncManager.initializeOnAppStart()
        Timber.d("SyncManager initialized")
    }

    /**
     * Initialise Timber pour le logging.
     * En mode DEBUG, utilise DebugTree qui affiche les logs dans Logcat.
     * En RELEASE, on pourrait ajouter un CrashReportingTree pour Crashlytics/Sentry.
     */
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Timber initialized in DEBUG mode")
        } else {
            // En production, on pourrait ajouter un tree pour le crash reporting
            // Timber.plant(CrashReportingTree())
        }
    }

    private fun initializeThreeTenABP() {
        try {
            AndroidThreeTen.init(this)
            Timber.d("ThreeTenABP initialized successfully")
        } catch (e: Exception) {
            // Log error but continue - the app may still work on newer Android versions
            Timber.e(e, "Failed to initialize ThreeTenABP")
        }
    }
    
    /**
     * Crée les canaux de notification
     */
    private fun initializeNotifications() {
        NotificationHelper.createNotificationChannels(this)
        Timber.d("Notification channels created")
    }
    
    /**
     * Planifie le worker pour les alertes de batteries
     */
    private fun scheduleBatteryAlertWorker() {
        BatteryAlertWorker.schedule(this)
        ReminderWorker.schedule(this)
    }
    
    /**
     * Configuration de WorkManager avec Hilt
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}