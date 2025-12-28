package leonfvt.skyfuel_app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import timber.log.Timber

/**
 * Worker pour la synchronisation en arrière-plan
 * Exécuté par WorkManager avec contraintes réseau
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val firebaseSyncService: FirebaseSyncService,
    private val batteryRepository: BatteryRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: Starting sync work")

        return try {
            // Vérifier si l'utilisateur est connecté et sync activée
            val syncState = firebaseSyncService.syncState.first()

            if (!syncState.isAuthenticated) {
                Timber.d("SyncWorker: Not authenticated, skipping")
                return Result.success()
            }

            if (!syncState.isEnabled) {
                Timber.d("SyncWorker: Sync disabled, skipping")
                return Result.success()
            }

            // Récupérer les batteries locales
            val batteries = batteryRepository.getAllBatteries().first()

            // Synchroniser avec Firebase
            when (val result = firebaseSyncService.syncBatteries(batteries)) {
                is SyncResult.Success -> {
                    Timber.d("SyncWorker: Sync success - ${result.uploadedCount} uploaded")
                    Result.success()
                }
                is SyncResult.Error -> {
                    Timber.e("SyncWorker: Sync error - ${result.message}")
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
                is SyncResult.NotAuthenticated -> {
                    Timber.w("SyncWorker: Not authenticated")
                    Result.failure()
                }
                is SyncResult.Disabled -> {
                    Timber.d("SyncWorker: Sync disabled")
                    Result.success()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Exception during sync")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "SyncWorker"
    }
}
