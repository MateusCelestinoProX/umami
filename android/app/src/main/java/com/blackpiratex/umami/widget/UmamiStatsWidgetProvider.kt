package com.blackpiratex.umami.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.blackpiratex.umami.MainActivity
import com.blackpiratex.umami.R
import com.blackpiratex.umami.data.api.models.TimeRange

class UmamiStatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = WidgetPreferences(context)
        for (appWidgetId in appWidgetIds) {
            prefs.deleteWidgetData(appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        val prefs = WidgetPreferences(context)
        WidgetSyncScheduler.schedulePeriodicWork(context, prefs.getUpdateIntervalHours())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (ACTION_REFRESH_WIDGET == intent.action) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.blackpiratex.umami.widget.ACTION_REFRESH_WIDGET"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = WidgetPreferences(context)
            val views = RemoteViews(context.packageName, R.layout.widget_umami_stats)

            val siteName = prefs.getWidgetWebsiteName(appWidgetId) ?: "Homepage"
            val timeRange = prefs.getWidgetTimeRange(appWidgetId)
            val viewsCount = prefs.getWidgetViews(appWidgetId)
            val visitsCount = prefs.getWidgetVisits(appWidgetId)
            val updatedTime = prefs.getLastUpdatedTime(appWidgetId)

            views.setTextViewText(R.id.tv_widget_site_name, siteName)
            views.setTextViewText(R.id.tv_widget_time_range, formatTimeRangeLabel(timeRange))
            views.setTextViewText(R.id.tv_widget_views, "$viewsCount")
            views.setTextViewText(R.id.tv_widget_visits, "$visitsCount")
            views.setTextViewText(R.id.tv_widget_updated, "Updated: $updatedTime")

            // Click root to open MainActivity
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, appPendingIntent)

            // Refresh button intent
            val refreshIntent = Intent(context, UmamiStatsWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_widget_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatTimeRangeLabel(range: TimeRange): String {
            return when (range) {
                TimeRange.LAST_24_HOURS -> "Last 24h"
                TimeRange.THIS_WEEK -> "This Week"
                TimeRange.LAST_7_DAYS -> "Last 7d"
                TimeRange.LAST_30_DAYS -> "Last 30d"
                TimeRange.THIS_MONTH -> "This Month"
                TimeRange.THIS_YEAR -> "This Year"
                TimeRange.LAST_6_MONTHS -> "Last 6m"
                TimeRange.LAST_12_MONTHS -> "Last 12m"
                TimeRange.ALL_TIME -> "All Time"
            }
        }
    }
}
