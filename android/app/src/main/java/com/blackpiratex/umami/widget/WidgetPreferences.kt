package com.blackpiratex.umami.widget

import android.content.Context
import android.content.SharedPreferences
import com.blackpiratex.umami.data.api.models.TimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WidgetPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "umami_widget_prefs"
        private const val KEY_WEBSITE_ID_PREFIX = "widget_site_id_"
        private const val KEY_WEBSITE_NAME_PREFIX = "widget_site_name_"
        private const val KEY_TIME_RANGE_PREFIX = "widget_time_range_"
        private const val KEY_VIEWS_PREFIX = "widget_views_"
        private const val KEY_VISITS_PREFIX = "widget_visits_"
        private const val KEY_LAST_UPDATED_PREFIX = "widget_last_updated_"
        private const val KEY_UPDATE_INTERVAL_HOURS = "widget_update_interval_hours"
    }

    fun saveWidgetConfig(appWidgetId: Int, websiteId: String, websiteName: String, timeRange: TimeRange) {
        prefs.edit()
            .putString(KEY_WEBSITE_ID_PREFIX + appWidgetId, websiteId)
            .putString(KEY_WEBSITE_NAME_PREFIX + appWidgetId, websiteName)
            .putString(KEY_TIME_RANGE_PREFIX + appWidgetId, timeRange.name)
            .apply()
    }

    fun getWidgetWebsiteId(appWidgetId: Int): String? {
        return prefs.getString(KEY_WEBSITE_ID_PREFIX + appWidgetId, null)
    }

    fun getWidgetWebsiteName(appWidgetId: Int): String? {
        return prefs.getString(KEY_WEBSITE_NAME_PREFIX + appWidgetId, "Homepage")
    }

    fun getWidgetTimeRange(appWidgetId: Int): TimeRange {
        val name = prefs.getString(KEY_TIME_RANGE_PREFIX + appWidgetId, TimeRange.LAST_24_HOURS.name)
        return try {
            TimeRange.valueOf(name ?: TimeRange.LAST_24_HOURS.name)
        } catch (e: Exception) {
            TimeRange.LAST_24_HOURS
        }
    }

    fun saveWidgetStats(appWidgetId: Int, views: Long, visits: Long) {
        val nowStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        prefs.edit()
            .putLong(KEY_VIEWS_PREFIX + appWidgetId, views)
            .putLong(KEY_VISITS_PREFIX + appWidgetId, visits)
            .putString(KEY_LAST_UPDATED_PREFIX + appWidgetId, nowStr)
            .apply()
    }

    fun getWidgetViews(appWidgetId: Int): Long {
        return prefs.getLong(KEY_VIEWS_PREFIX + appWidgetId, 0L)
    }

    fun getWidgetVisits(appWidgetId: Int): Long {
        return prefs.getLong(KEY_VISITS_PREFIX + appWidgetId, 0L)
    }

    fun getLastUpdatedTime(appWidgetId: Int): String {
        return prefs.getString(KEY_LAST_UPDATED_PREFIX + appWidgetId, "Just now") ?: "Just now"
    }

    fun saveUpdateIntervalHours(hours: Int) {
        prefs.edit().putInt(KEY_UPDATE_INTERVAL_HOURS, hours).apply()
    }

    fun getUpdateIntervalHours(): Int {
        return prefs.getInt(KEY_UPDATE_INTERVAL_HOURS, 2) // Default 2 hours
    }

    fun deleteWidgetData(appWidgetId: Int) {
        prefs.edit()
            .remove(KEY_WEBSITE_ID_PREFIX + appWidgetId)
            .remove(KEY_WEBSITE_NAME_PREFIX + appWidgetId)
            .remove(KEY_TIME_RANGE_PREFIX + appWidgetId)
            .remove(KEY_VIEWS_PREFIX + appWidgetId)
            .remove(KEY_VISITS_PREFIX + appWidgetId)
            .remove(KEY_LAST_UPDATED_PREFIX + appWidgetId)
            .apply()
    }
}
