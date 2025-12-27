package leonfvt.skyfuel_app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import leonfvt.skyfuel_app.MainActivity
import leonfvt.skyfuel_app.R
import leonfvt.skyfuel_app.domain.model.Battery
import leonfvt.skyfuel_app.domain.model.BatteryStatus
import leonfvt.skyfuel_app.domain.repository.BatteryRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Widget Android pour afficher un résumé des batteries
 */
@AndroidEntryPoint
class BatteryWidgetProvider : AppWidgetProvider() {
    
    @Inject
    lateinit var repository: BatteryRepository
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Timber.d("BatteryWidgetProvider onUpdate called for ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        Timber.d("BatteryWidgetProvider enabled")
    }
    
    override fun onDisabled(context: Context) {
        Timber.d("BatteryWidgetProvider disabled")
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_REFRESH -> {
                Timber.d("Widget refresh requested")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, BatteryWidgetProvider::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        scope.launch {
            try {
                val batteries = repository.getAllBatteries().first()
                val views = createRemoteViews(context, batteries)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Timber.e(e, "Error updating widget")
                // Afficher un état d'erreur
                val views = createErrorViews(context)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
    
    private fun createRemoteViews(context: Context, batteries: List<Battery>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_summary)
        
        // Calculer les statistiques
        val totalCount = batteries.size
        val chargedCount = batteries.count { it.status == BatteryStatus.CHARGED }
        val dischargedCount = batteries.count { it.status == BatteryStatus.DISCHARGED }
        val storageCount = batteries.count { it.status == BatteryStatus.STORAGE }
        val outOfServiceCount = batteries.count { it.status == BatteryStatus.OUT_OF_SERVICE }
        
        // Mettre à jour les vues
        views.setTextViewText(R.id.widget_total_count, totalCount.toString())
        views.setTextViewText(R.id.widget_charged_count, chargedCount.toString())
        views.setTextViewText(R.id.widget_discharged_count, dischargedCount.toString())
        views.setTextViewText(R.id.widget_storage_count, storageCount.toString())
        
        // Intent pour ouvrir l'app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, openAppPendingIntent)
        
        // Intent pour rafraîchir
        val refreshIntent = Intent(context, BatteryWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
        
        return views
    }
    
    private fun createErrorViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_battery_summary)
        views.setTextViewText(R.id.widget_total_count, "-")
        views.setTextViewText(R.id.widget_charged_count, "-")
        views.setTextViewText(R.id.widget_discharged_count, "-")
        views.setTextViewText(R.id.widget_storage_count, "-")
        return views
    }
    
    companion object {
        const val ACTION_REFRESH = "leonfvt.skyfuel_app.WIDGET_REFRESH"
        
        /**
         * Met à jour tous les widgets de l'application
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, BatteryWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.sendBroadcast(intent)
        }
    }
}
