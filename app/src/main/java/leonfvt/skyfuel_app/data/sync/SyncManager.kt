package leonfvt.skyfuel_app.data.sync

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.data.preferences.UserPreferencesRepository
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestionnaire de synchronisation automatique
 * - Sync à l'ouverture de l'app
 * - Sync après modifications locales
 * - File d'attente offline avec sync dès connexion
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseSyncService: FirebaseSyncService,
    private val batteryRepository: BatteryRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _pendingChanges = MutableStateFlow(0)
    val pendingChanges: StateFlow<Int> = _pendingChanges.asStateFlow()

    private val _lastSyncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val lastSyncStatus: StateFlow<SyncStatus> = _lastSyncStatus.asStateFlow()

    init {
        // Observer les changements de batteries pour déclencher une sync
        observeBatteryChanges()
    }

    /**
     * Initialise la synchronisation au démarrage de l'app
     */
    fun initializeOnAppStart() {
        scope.launch {
            val syncState = firebaseSyncService.syncState.first()
            if (syncState.isAuthenticated && syncState.isEnabled) {
                Timber.d("SyncManager: Auto-sync on app start")
                performSync()
            }
        }

        // Programmer les syncs périodiques
        schedulePeriodicSync()
    }

    /**
     * Observer les changements de batteries et programmer une sync
     */
    private fun observeBatteryChanges() {
        scope.launch {
            batteryRepository.getAllBatteries().collect { batteries ->
                // Vérifier si on doit synchroniser
                val syncState = firebaseSyncService.syncState.first()
                if (syncState.isAuthenticated && syncState.isEnabled) {
                    // Marquer comme changement en attente
                    _pendingChanges.value = batteries.size

                    // Programmer une sync différée (debounce)
                    scheduleDeferredSync()
                }
            }
        }
    }

    /**
     * Programme une synchronisation différée (pour éviter trop de syncs)
     */
    private fun scheduleDeferredSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(5, TimeUnit.SECONDS) // Attendre 5 sec pour grouper les changements
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .addTag(SYNC_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SYNC_WORK_NAME_DEFERRED,
                ExistingWorkPolicy.REPLACE, // Remplacer si déjà programmé
                syncRequest
            )

        Timber.d("SyncManager: Deferred sync scheduled")
    }

    /**
     * Programme des syncs périodiques (toutes les 15 min si connecté)
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSync = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .addTag(SYNC_WORK_TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SYNC_WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSync
            )

        Timber.d("SyncManager: Periodic sync scheduled")
    }

    /**
     * Force une synchronisation immédiate
     */
    suspend fun performSync(): SyncResult {
        _lastSyncStatus.value = SyncStatus.Syncing

        return try {
            val batteries = batteryRepository.getAllBatteries().first()
            val result = firebaseSyncService.syncBatteries(batteries)

            when (result) {
                is leonfvt.skyfuel_app.data.sync.SyncResult.Success -> {
                    _lastSyncStatus.value = SyncStatus.Success(
                        uploadedCount = result.uploadedCount,
                        downloadedCount = result.downloadedCount
                    )
                    _pendingChanges.value = 0
                    Timber.d("SyncManager: Sync success - ${result.uploadedCount} uploaded")
                }
                is leonfvt.skyfuel_app.data.sync.SyncResult.Error -> {
                    _lastSyncStatus.value = SyncStatus.Error(result.message)
                    Timber.e("SyncManager: Sync error - ${result.message}")
                }
                is leonfvt.skyfuel_app.data.sync.SyncResult.NotAuthenticated -> {
                    _lastSyncStatus.value = SyncStatus.Error("Non authentifié")
                }
                is leonfvt.skyfuel_app.data.sync.SyncResult.Disabled -> {
                    _lastSyncStatus.value = SyncStatus.Idle
                }
            }

            result
        } catch (e: Exception) {
            _lastSyncStatus.value = SyncStatus.Error(e.message ?: "Erreur inconnue")
            Timber.e(e, "SyncManager: Sync exception")
            leonfvt.skyfuel_app.data.sync.SyncResult.Error(e.message ?: "Erreur")
        }
    }

    /**
     * Télécharge les batteries depuis le cloud et les fusionne localement
     */
    suspend fun downloadAndMerge(): Int {
        val remoteBatteries = firebaseSyncService.downloadBatteries() ?: return 0

        var importedCount = 0
        remoteBatteries.forEach { battery ->
            val existing = batteryRepository.getBatteryBySerialNumber(battery.serialNumber)
            if (existing == null) {
                batteryRepository.insertBattery(battery)
                importedCount++
            }
        }

        Timber.d("SyncManager: Downloaded and merged $importedCount batteries")
        return importedCount
    }

    /**
     * Notifie qu'un changement local a été effectué
     */
    fun notifyLocalChange() {
        scope.launch {
            val syncState = firebaseSyncService.syncState.first()
            if (syncState.isAuthenticated && syncState.isEnabled) {
                _pendingChanges.value++
                scheduleDeferredSync()
            }
        }
    }

    companion object {
        const val SYNC_WORK_TAG = "sync_work"
        const val SYNC_WORK_NAME_DEFERRED = "sync_deferred"
        const val SYNC_WORK_NAME_PERIODIC = "sync_periodic"
    }
}

/**
 * État de la dernière synchronisation
 */
sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data class Success(val uploadedCount: Int, val downloadedCount: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
