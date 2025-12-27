package leonfvt.skyfuel_app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import leonfvt.skyfuel_app.domain.service.AlertService
import leonfvt.skyfuel_app.util.NotificationHelper
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker pour vérifier les alertes de batteries en arrière-plan
 * S'exécute quotidiennement
 */
@HiltWorker
class BatteryAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: BatteryRepository,
    private val alertService: AlertService
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        const val WORK_NAME = "battery_alert_check"
        private const val REPEAT_INTERVAL_HOURS = 24L
        
        /**
         * Planifie la vérification périodique des alertes
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<BatteryAlertWorker>(
                REPEAT_INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setInitialDelay(1, TimeUnit.HOURS) // Première exécution après 1h
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Ne pas remplacer si déjà planifié
                workRequest
            )
            
            Timber.d("BatteryAlertWorker scheduled for every $REPEAT_INTERVAL_HOURS hours")
        }
        
        /**
         * Exécute une vérification immédiate (one-shot)
         */
        fun runNow(context: Context) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<BatteryAlertWorker>()
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Timber.d("BatteryAlertWorker triggered for immediate execution")
        }
        
        /**
         * Annule la vérification périodique
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Timber.d("BatteryAlertWorker cancelled")
        }
    }
    
    override suspend fun doWork(): Result {
        Timber.d("BatteryAlertWorker starting...")
        
        return try {
            // Récupérer toutes les batteries
            val batteries = repository.getAllBatteries().first()
            
            if (batteries.isEmpty()) {
                Timber.d("No batteries found, skipping alert check")
                return Result.success()
            }
            
            // Vérifier les alertes
            val alerts = alertService.checkAllBatteriesAlerts(batteries)
            
            Timber.d("Found ${alerts.size} alerts for ${batteries.size} batteries")
            
            if (alerts.isNotEmpty()) {
                // Afficher les notifications
                alerts.forEachIndexed { index, alert ->
                    NotificationHelper.showAlertNotification(
                        context = context,
                        alert = alert,
                        notificationId = index + 1 // ID > 0 pour éviter le summary ID
                    )
                }
                
                // Afficher le résumé groupé
                NotificationHelper.showAlertsSummaryNotification(context, alerts)
            }
            
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Error checking battery alerts")
            Result.retry()
        }
    }
}
