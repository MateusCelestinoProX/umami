package com.blackpiratex.umami.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "websites")
data class WebsiteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val domain: String?,
    val shareId: String?,
    val createdAt: String?
)

@Entity(tableName = "stats", primaryKeys = ["websiteId", "timeRangeKey", "filterKey"])
data class StatsEntity(
    val websiteId: String,
    val timeRangeKey: String,
    val filterKey: String,
    val pageviews: Long,
    val pageviewsChange: Long,
    val visitors: Long,
    val visitorsChange: Long,
    val visits: Long,
    val visitsChange: Long,
    val bounces: Long,
    val bouncesChange: Long,
    val totaltime: Long,
    val totaltimeChange: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pageview_points", primaryKeys = ["websiteId", "timeRangeKey", "filterKey", "timestampX"])
data class PageviewPointEntity(
    val websiteId: String,
    val timeRangeKey: String,
    val filterKey: String,
    val timestampX: String,
    val pageviewsY: Long,
    val sessionsY: Long
)

@Entity(tableName = "metrics", primaryKeys = ["websiteId", "timeRangeKey", "filterKey", "metricType", "itemLabelX"])
data class MetricEntity(
    val websiteId: String,
    val timeRangeKey: String,
    val filterKey: String,
    val metricType: String, // url, referrer, browser, os, device, country, region
    val itemLabelX: String,
    val countY: Long
)

@Entity(tableName = "sessions", primaryKeys = ["id", "websiteId", "timeRangeKey", "filterKey"])
data class SessionEntity(
    val id: String,
    val websiteId: String,
    val timeRangeKey: String,
    val filterKey: String,
    val hostname: String?,
    val browser: String?,
    val os: String?,
    val device: String?,
    val country: String?,
    val region: String?,
    val city: String?,
    val views: Int,
    val visits: Int,
    val events: Int,
    val lastAt: String?
)
