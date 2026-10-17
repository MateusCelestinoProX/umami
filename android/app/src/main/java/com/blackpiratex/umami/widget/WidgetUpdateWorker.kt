package com.blackpiratex.umami.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.blackpiratex.umami.UmamiApplication
import com.blackpiratex.umami.util.DateUtils
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = appContext.applicationContext as? UmamiApplication ?: return Result.failure()
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val componentName = ComponentName(appContext, UmamiStatsWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)

        val widgetPrefs = WidgetPreferences(appContext)

        val serverUrl = app.sessionManager.getServerUrl()
        val token = app.sessionManager.getToken()

        for (widgetId in widgetIds) {
            val websiteId = widgetPrefs.getWidgetWebsiteId(widgetId) ?: app.sessionManager.getSelectedWebsiteId()
            if (websiteId.isNullOrBlank()) continue

            val timeRange = widgetPrefs.getWidgetTimeRange(widgetId)
            val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(timeRange, 0)

            var views = widgetPrefs.getWidgetViews(widgetId)
            var visits = widgetPrefs.getWidgetVisits(widgetId)

            if (serverUrl != null && token != null && app.networkObserver.isCurrentlyOnline()) {
                try {
                    val stats = app.apiService.getStats(serverUrl, token, websiteId, startAt, endAt)
                    views = stats.pageviews.value
                    visits = stats.visits.value
                    widgetPrefs.saveWidgetStats(widgetId, views, visits)
                } catch (e: Exception) {
                    // Ignore, fallback to cached stats
                }
            } else {
                // Fallback to local DB cache
                val timeKey = "${timeRange.name}_0"
                val cachedStats = app.database.statsDao().getStats(websiteId, timeKey, "p=_c=_r=_ref=").firstOrNull()
                if (cachedStats != null) {
                    views = cachedStats.pageviews
                    visits = cachedStats.visits
                    widgetPrefs.saveWidgetStats(widgetId, views, visits)
                }
            }

            UmamiStatsWidgetProvider.updateWidget(appContext, appWidgetManager, widgetId)
        }

        return Result.success()
    }
}

object WidgetSyncScheduler {
    private const val WORK_NAME = "umami_widget_periodic_update_work"

    fun schedulePeriodicWork(context: Context, intervalHours: Int = 2) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val safeIntervalMinutes = maxOf(15L, intervalHours.toLong() * 60L)

        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            safeIntervalMinutes, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
