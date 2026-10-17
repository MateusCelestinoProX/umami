package com.blackpiratex.umami.data.api

import com.blackpiratex.umami.data.api.models.AnalyticsFilter
import com.blackpiratex.umami.data.api.models.ChartPointDto
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.data.api.models.PageviewsResponseDto
import com.blackpiratex.umami.data.api.models.SessionItemDto
import com.blackpiratex.umami.data.api.models.StatValue
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.data.api.models.WebsiteStatsDto
import com.blackpiratex.umami.data.db.UmamiDatabase
import com.blackpiratex.umami.data.db.entities.MetricEntity
import com.blackpiratex.umami.data.db.entities.PageviewPointEntity
import com.blackpiratex.umami.data.db.entities.SessionEntity
import com.blackpiratex.umami.data.db.entities.StatsEntity
import com.blackpiratex.umami.data.db.entities.WebsiteEntity
import com.blackpiratex.umami.data.preferences.SessionManager
import com.blackpiratex.umami.util.DateUtils
import com.blackpiratex.umami.util.NetworkObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class UmamiRepository(
    private val apiService: UmamiApiService,
    private val db: UmamiDatabase,
    private val sessionManager: SessionManager,
    private val networkObserver: NetworkObserver
) {

    fun getWebsites(): Flow<Result<List<WebsiteDto>>> = flow {
        val serverUrl = sessionManager.getServerUrl()
        val token = sessionManager.getToken()
        val isOnline = networkObserver.isCurrentlyOnline()

        if (serverUrl != null && token != null && isOnline) {
            try {
                val remoteWebsites = apiService.getWebsites(serverUrl, token)
                val entities = remoteWebsites.map {
                    WebsiteEntity(it.id, it.name, it.domain, it.shareId, it.createdAt)
                }
                db.websiteDao().insertWebsites(entities)
                emit(Result.success(remoteWebsites))
                return@flow
            } catch (e: Exception) {
                // Ignore and fallback to DB
            }
        }

        // Fallback to local Room cache
        val localEntities = db.websiteDao().getAllWebsites().firstOrNull() ?: emptyList()
        val dtoList = localEntities.map {
            WebsiteDto(it.id, it.name, it.domain, it.shareId, it.createdAt)
        }
        emit(Result.success(dtoList))
    }

    fun getStats(
        websiteId: String,
        timeRange: TimeRange,
        offsetIndex: Int,
        filter: AnalyticsFilter
    ): Flow<WebsiteStatsDto> = flow {
        val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(timeRange, offsetIndex)
        val timeKey = "${timeRange.name}_$offsetIndex"
        val filterKey = filter.toCacheKey()
        val isOnline = networkObserver.isCurrentlyOnline()

        val serverUrl = sessionManager.getServerUrl()
        val token = sessionManager.getToken()

        if (serverUrl != null && token != null && isOnline) {
            try {
                val stats = apiService.getStats(serverUrl, token, websiteId, startAt, endAt, filter)
                val entity = StatsEntity(
                    websiteId = websiteId,
                    timeRangeKey = timeKey,
                    filterKey = filterKey,
                    pageviews = stats.pageviews.value,
                    pageviewsChange = stats.pageviews.change,
                    visitors = stats.visitors.value,
                    visitorsChange = stats.visitors.change,
                    visits = stats.visits.value,
                    visitsChange = stats.visits.change,
                    bounces = stats.bounces.value,
                    bouncesChange = stats.bounces.change,
                    totaltime = stats.totaltime.value,
                    totaltimeChange = stats.totaltime.change
                )
                db.statsDao().insertStats(entity)
                emit(stats)
                return@flow
            } catch (e: Exception) {
                // Fallback to cache
            }
        }

        val cached = db.statsDao().getStats(websiteId, timeKey, filterKey).firstOrNull()
        if (cached != null) {
            emit(
                WebsiteStatsDto(
                    pageviews = StatValue(cached.pageviews, cached.pageviewsChange),
                    visitors = StatValue(cached.visitors, cached.visitorsChange),
                    visits = StatValue(cached.visits, cached.visitsChange),
                    bounces = StatValue(cached.bounces, cached.bouncesChange),
                    totaltime = StatValue(cached.totaltime, cached.totaltimeChange)
                )
            )
        } else {
            emit(WebsiteStatsDto())
        }
    }

    fun getPageviews(
        websiteId: String,
        timeRange: TimeRange,
        offsetIndex: Int,
        filter: AnalyticsFilter
    ): Flow<PageviewsResponseDto> = flow {
        val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(timeRange, offsetIndex)
        val timeKey = "${timeRange.name}_$offsetIndex"
        val filterKey = filter.toCacheKey()
        val isOnline = networkObserver.isCurrentlyOnline()

        val serverUrl = sessionManager.getServerUrl()
        val token = sessionManager.getToken()

        if (serverUrl != null && token != null && isOnline) {
            try {
                val pageviewsData = apiService.getPageviews(
                    serverUrl, token, websiteId, startAt, endAt, timeRange.unit, filter
                )

                db.pageviewDao().deletePageviewPoints(websiteId, timeKey, filterKey)
                val points = pageviewsData.pageviews.mapIndexed { idx, pv ->
                    val sess = pageviewsData.sessions.getOrNull(idx)
                    PageviewPointEntity(
                        websiteId = websiteId,
                        timeRangeKey = timeKey,
                        filterKey = filterKey,
                        timestampX = pv.x,
                        pageviewsY = pv.y,
                        sessionsY = sess?.y ?: 0
                    )
                }
                db.pageviewDao().insertPageviewPoints(points)
                emit(pageviewsData)
                return@flow
            } catch (e: Exception) {
                // Fallback to cache
            }
        }

        val cachedPoints = db.pageviewDao().getPageviewPoints(websiteId, timeKey, filterKey).firstOrNull() ?: emptyList()
        val pvList = cachedPoints.map { ChartPointDto(it.timestampX, it.pageviewsY) }
        val sessList = cachedPoints.map { ChartPointDto(it.timestampX, it.sessionsY) }
        emit(PageviewsResponseDto(pageviews = pvList, sessions = sessList))
    }

    fun getMetrics(
        websiteId: String,
        timeRange: TimeRange,
        offsetIndex: Int,
        type: String,
        filter: AnalyticsFilter
    ): Flow<List<MetricItemDto>> = flow {
        val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(timeRange, offsetIndex)
        val timeKey = "${timeRange.name}_$offsetIndex"
        val filterKey = filter.toCacheKey()
        val isOnline = networkObserver.isCurrentlyOnline()

        val serverUrl = sessionManager.getServerUrl()
        val token = sessionManager.getToken()

        if (serverUrl != null && token != null && isOnline) {
            try {
                val metrics = apiService.getMetrics(
                    serverUrl, token, websiteId, startAt, endAt, type, filter
                )

                db.metricDao().deleteMetrics(websiteId, timeKey, filterKey, type)
                val entities = metrics.map {
                    MetricEntity(
                        websiteId = websiteId,
                        timeRangeKey = timeKey,
                        filterKey = filterKey,
                        metricType = type,
                        itemLabelX = it.x ?: "Unknown",
                        countY = it.y
                    )
                }
                db.metricDao().insertMetrics(entities)
                emit(metrics)
                return@flow
            } catch (e: Exception) {
                // Fallback to cache
            }
        }

        val cachedMetrics = db.metricDao().getMetrics(websiteId, timeKey, filterKey, type).firstOrNull() ?: emptyList()
        val dtoList = cachedMetrics.map { MetricItemDto(x = it.itemLabelX, y = it.countY) }
        emit(dtoList)
    }

    fun getSessions(
        websiteId: String,
        timeRange: TimeRange,
        offsetIndex: Int,
        searchQuery: String?,
        filter: AnalyticsFilter
    ): Flow<List<SessionItemDto>> = flow {
        val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(timeRange, offsetIndex)
        val timeKey = "${timeRange.name}_$offsetIndex"
        val filterKey = filter.toCacheKey()
        val isOnline = networkObserver.isCurrentlyOnline()

        val serverUrl = sessionManager.getServerUrl()
        val token = sessionManager.getToken()

        if (serverUrl != null && token != null && isOnline) {
            try {
                val sessions = apiService.getSessions(
                    serverUrl, token, websiteId, startAt, endAt, searchQuery, filter
                )

                db.sessionDao().deleteSessions(websiteId, timeKey, filterKey)
                val entities = sessions.map {
                    SessionEntity(
                        id = it.id,
                        websiteId = websiteId,
                        timeRangeKey = timeKey,
                        filterKey = filterKey,
                        hostname = it.hostname,
                        browser = it.browser,
                        os = it.os,
                        device = it.device,
                        country = it.country,
                        region = it.subdivision1,
                        city = it.city,
                        views = it.views,
                        visits = it.visits,
                        events = it.events,
                        lastAt = it.lastAt ?: it.createdAt
                    )
                }
                db.sessionDao().insertSessions(entities)
                emit(sessions)
                return@flow
            } catch (e: Exception) {
                // Fallback to cache
            }
        }

        val cachedSessions = db.sessionDao().getSessions(websiteId, timeKey, filterKey).firstOrNull() ?: emptyList()
        val dtoList = cachedSessions.map {
            SessionItemDto(
                id = it.id,
                hostname = it.hostname,
                browser = it.browser,
                os = it.os,
                device = it.device,
                country = it.country,
                subdivision1 = it.region,
                city = it.city,
                views = it.views,
                visits = it.visits,
                events = it.events,
                lastAt = it.lastAt
            )
        }
        val filtered = if (!searchQuery.isNullOrBlank()) {
            dtoList.filter {
                (it.browser?.contains(searchQuery, true) == true) ||
                (it.os?.contains(searchQuery, true) == true) ||
                (it.country?.contains(searchQuery, true) == true) ||
                (it.city?.contains(searchQuery, true) == true) ||
                (it.id.contains(searchQuery, true))
            }
        } else dtoList

        emit(filtered)
    }
}
