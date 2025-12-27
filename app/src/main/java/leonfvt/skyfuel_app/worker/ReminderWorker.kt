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
import leonfvt.skyfuel_app.data.local.dao.BatteryDao
import leonfvt.skyfuel_app.data.local.dao.ChargeReminderDao
import leonfvt.skyfuel_app.domain.model.ReminderType
import leonfvt.skyfuel_app.util.NotificationHelper
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Worker qui vérifie et déclenche les rappels de charge
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderDao: ChargeReminderDao,
    private val batteryDao: BatteryDao
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            checkAndTriggerReminders()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private suspend fun checkAndTriggerReminders() {
        val now = LocalDateTime.now()
        val currentTime = now.toLocalTime()
        val currentDay = now.dayOfWeek
        
        val enabledReminders = reminderDao.getEnabledRemindersSync()
        
        enabledReminders.forEach { reminderEntity ->
            val reminder = reminderEntity.toDomainModel()
            
            // Vérifier si c'est le bon jour
            val isCorrectDay = reminder.daysOfWeek.isEmpty() || reminder.daysOfWeek.contains(currentDay)
            
            // Vérifier si c'est la bonne heure (avec une fenêtre de 5 minutes)
            val reminderTime = reminder.time
            val isCorrectTime = isWithinTimeWindow(currentTime, reminderTime, windowMinutes = 5)
            
            if (isCorrectDay && isCorrectTime) {
                // Récupérer les infos de la batterie
                val battery = batteryDao.getBatteryById(reminder.batteryId)
                
                if (battery != null) {
                    val title = when (reminder.reminderType) {
                        ReminderType.CHARGE -> "Rappel de charge"
                        ReminderType.STORAGE -> "Rappel de stockage"
                        ReminderType.MAINTENANCE -> "Rappel de maintenance"
                        ReminderType.VOLTAGE_CHECK -> "Vérification de tension"
                    }
                    
                    val message = when (reminder.reminderType) {
                        ReminderType.CHARGE -> "Il est temps de charger ${battery.brand} ${battery.model}"
                        ReminderType.STORAGE -> "Mettez ${battery.brand} ${battery.model} en mode stockage"
                        ReminderType.MAINTENANCE -> "Maintenance prévue pour ${battery.brand} ${battery.model}"
                        ReminderType.VOLTAGE_CHECK -> "Vérifiez la tension de ${battery.brand} ${battery.model}"
                    }
                    
                    NotificationHelper.showReminderNotification(
                        context = applicationContext,
                        title = title,
                        message = message,
                        notificationId = reminder.id.toInt()
                    )
                }
            }
        }
    }
    
    private fun isWithinTimeWindow(current: LocalTime, target: LocalTime, windowMinutes: Int): Boolean {
        val diffMinutes = kotlin.math.abs(
            current.toSecondOfDay() - target.toSecondOfDay()
        ) / 60
        return diffMinutes <= windowMinutes
    }
    
    companion object {
        const val WORK_NAME = "reminder_check_work"
        
        /**
         * Planifie le worker pour vérifier les rappels toutes les 15 minutes
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
                15, java.util.concurrent.TimeUnit.MINUTES
            ).build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
