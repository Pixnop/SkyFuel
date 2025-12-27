package leonfvt.skyfuel_app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import leonfvt.skyfuel_app.MainActivity
import leonfvt.skyfuel_app.R
import leonfvt.skyfuel_app.domain.model.AlertPriority
import leonfvt.skyfuel_app.domain.model.BatteryAlert

/**
 * Helper pour gérer les notifications de l'application
 */
object NotificationHelper {
    
    const val CHANNEL_ID_ALERTS = "battery_alerts"
    const val CHANNEL_NAME_ALERTS = "Alertes batteries"
    const val CHANNEL_DESC_ALERTS = "Notifications pour les alertes de batteries"
    
    const val CHANNEL_ID_REMINDERS = "charge_reminders"
    const val CHANNEL_NAME_REMINDERS = "Rappels de charge"
    const val CHANNEL_DESC_REMINDERS = "Rappels programmés pour charger les batteries"
    
    const val NOTIFICATION_GROUP_ALERTS = "leonfvt.skyfuel_app.ALERTS"
    const val NOTIFICATION_GROUP_REMINDERS = "leonfvt.skyfuel_app.REMINDERS"
    const val NOTIFICATION_SUMMARY_ID = 0
    const val REMINDER_NOTIFICATION_BASE_ID = 10000
    
    /**
     * Crée les canaux de notification (nécessaire pour Android 8+)
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERTS,
                CHANNEL_NAME_ALERTS,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC_ALERTS
                enableVibration(true)
                enableLights(true)
            }
            
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_REMINDERS
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
    
    /**
     * Affiche une notification pour une alerte
     */
    fun showAlertNotification(
        context: Context,
        alert: BatteryAlert,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("battery_id", alert.batteryId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val priority = when (alert.priority) {
            AlertPriority.CRITICAL -> NotificationCompat.PRIORITY_HIGH
            AlertPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            AlertPriority.MEDIUM -> NotificationCompat.PRIORITY_DEFAULT
            AlertPriority.LOW -> NotificationCompat.PRIORITY_LOW
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(alert.title)
            .setContentText(alert.batteryName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_ALERTS)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission de notification non accordée
        }
    }
    
    /**
     * Affiche un résumé groupé pour plusieurs alertes
     */
    fun showAlertsSummaryNotification(
        context: Context,
        alerts: List<BatteryAlert>
    ) {
        if (alerts.isEmpty()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_SUMMARY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val criticalCount = alerts.count { 
            it.priority == AlertPriority.CRITICAL || it.priority == AlertPriority.HIGH 
        }
        
        val title = if (criticalCount > 0) {
            "$criticalCount alerte(s) importante(s)"
        } else {
            "${alerts.size} alerte(s) batterie"
        }
        
        val summary = NotificationCompat.Builder(context, CHANNEL_ID_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Appuyez pour voir les détails")
            .setStyle(NotificationCompat.InboxStyle()
                .also { style ->
                    alerts.take(5).forEach { alert ->
                        style.addLine("${alert.batteryName}: ${alert.title}")
                    }
                    if (alerts.size > 5) {
                        style.setSummaryText("+${alerts.size - 5} autres")
                    }
                }
            )
            .setGroup(NOTIFICATION_GROUP_ALERTS)
            .setGroupSummary(true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_SUMMARY_ID, summary)
        } catch (e: SecurityException) {
            // Permission de notification non accordée
        }
    }
    
    /**
     * Affiche une notification pour un rappel programmé
     */
    fun showReminderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REMINDER_NOTIFICATION_BASE_ID + notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_REMINDERS)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(
                REMINDER_NOTIFICATION_BASE_ID + notificationId,
                notification
            )
        } catch (e: SecurityException) {
            // Permission de notification non accordée
        }
    }
    
    /**
     * Annule toutes les notifications d'alertes
     */
    fun cancelAllAlertNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
