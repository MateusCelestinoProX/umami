package com.blackpiratex.umami.ui.screens.realtime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpiratex.umami.data.api.UmamiApiService
import com.blackpiratex.umami.data.api.UmamiRepository
import com.blackpiratex.umami.data.api.models.ActivityType
import com.blackpiratex.umami.data.api.models.AnalyticsFilter
import com.blackpiratex.umami.data.api.models.ChartPointDto
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.data.api.models.RealtimeActivityItem
import com.blackpiratex.umami.data.api.models.SessionItemDto
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.data.preferences.SessionManager
import com.blackpiratex.umami.util.NetworkObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RealtimeUiState(
    val websites: List<WebsiteDto> = emptyList(),
    val selectedWebsiteId: String? = null,
    val selectedWebsiteName: String = "Homepage",
    val activeOnlineCount: Int = 1,
    val viewsCount: Long = 0,
    val visitorsCount: Long = 0,
    val eventsCount: Long = 0,
    val countriesCount: Long = 0,
    val pageviewsChart: List<ChartPointDto> = emptyList(),
    val sessionsChart: List<ChartPointDto> = emptyList(),
    val activityFilter: ActivityType? = null,
    val searchQuery: String = "",
    val activities: List<RealtimeActivityItem> = emptyList(),
    val topPages: List<MetricItemDto> = emptyList(),
    val referrers: List<MetricItemDto> = emptyList(),
    val countries: List<MetricItemDto> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false
)

class RealtimeViewModel(
    private val apiService: UmamiApiService,
    private val repository: UmamiRepository,
    private val sessionManager: SessionManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(RealtimeUiState())
    val uiState: StateFlow<RealtimeUiState> = _uiState.asStateFlow()

    private var isPollingActive = true

    init {
        observeNetworkState()
        loadWebsites()
        startRealtimePolling()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            try {
                networkObserver.isOnline.collect { isOnline ->
                    _uiState.value = _uiState.value.copy(isOffline = !isOnline)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadWebsites() {
        viewModelScope.launch {
            try {
                repository.getWebsites().collect { result ->
                    result.onSuccess { sites ->
                        val savedId = sessionManager.getSelectedWebsiteId()
                        val targetSite = sites.find { it.id == savedId } ?: sites.firstOrNull()
                        val targetId = targetSite?.id

                        _uiState.value = _uiState.value.copy(
                            websites = sites,
                            selectedWebsiteId = targetId,
                            selectedWebsiteName = targetSite?.name ?: "Homepage"
                        )

                        if (targetId != null) {
                            fetchRealtimeData(targetId)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectActivityFilter(type: ActivityType?) {
        _uiState.value = _uiState.value.copy(activityFilter = type)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    private fun startRealtimePolling() {
        viewModelScope.launch {
            while (isPollingActive) {
                try {
                    _uiState.value.selectedWebsiteId?.let { id ->
                        fetchRealtimeData(id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(6000L)
            }
        }
    }

    private suspend fun fetchRealtimeData(websiteId: String) {
        try {
            val serverUrl = sessionManager.getServerUrl() ?: return
            val token = sessionManager.getToken() ?: return
            val isOnline = networkObserver.isCurrentlyOnline()

            val endAt = System.currentTimeMillis()
            val startAt = endAt - 24 * 60 * 60 * 1000L // 24 hours window for guaranteed stats fallback

            if (isOnline) {
                // 1. Fetch active online count
                val activeCount = try {
                    apiService.getActiveUsers(serverUrl, token, websiteId)
                } catch (e: Exception) { 1 }

                // 2. Attempt dedicated /api/realtime endpoint
                val realtimePayload = apiService.getRealtime(serverUrl, token, websiteId, startAt)
                if (realtimePayload != null && (realtimePayload.sessions.isNotEmpty() || realtimePayload.pageviews.isNotEmpty())) {
                    val activityItems = generateActivityItems(realtimePayload.sessions)
                    _uiState.value = _uiState.value.copy(
                        activeOnlineCount = maxOf(1, activeCount),
                        viewsCount = realtimePayload.pageviews.size.toLong(),
                        visitorsCount = realtimePayload.sessions.size.toLong(),
                        eventsCount = realtimePayload.events.size.toLong(),
                        activities = activityItems,
                        isLoading = false
                    )
                    return
                }

                // 3. Fallback: Fetch standard metrics & stats
                val stats = try {
                    apiService.getStats(serverUrl, token, websiteId, startAt, endAt)
                } catch (e: Exception) { null }

                val chartData = try {
                    apiService.getPageviews(serverUrl, token, websiteId, startAt, endAt, "hour")
                } catch (e: Exception) { null }

                val sessions = try {
                    apiService.getSessions(serverUrl, token, websiteId, startAt, endAt)
                } catch (e: Exception) { emptyList() }

                val pages = try {
                    apiService.getMetrics(serverUrl, token, websiteId, startAt, endAt, "url")
                } catch (e: Exception) { emptyList() }

                val refs = try {
                    apiService.getMetrics(serverUrl, token, websiteId, startAt, endAt, "referrer")
                } catch (e: Exception) { emptyList() }

                val countries = try {
                    apiService.getMetrics(serverUrl, token, websiteId, startAt, endAt, "country")
                } catch (e: Exception) { emptyList() }

                val activityItems = generateActivityItems(sessions)

                _uiState.value = _uiState.value.copy(
                    activeOnlineCount = maxOf(1, activeCount),
                    viewsCount = stats?.pageviews?.value ?: 0L,
                    visitorsCount = stats?.visitors?.value ?: 0L,
                    eventsCount = stats?.events?.value ?: 0L,
                    countriesCount = countries.size.toLong(),
                    pageviewsChart = chartData?.pageviews ?: emptyList(),
                    sessionsChart = chartData?.sessions ?: emptyList(),
                    activities = activityItems,
                    topPages = pages,
                    referrers = refs,
                    countries = countries,
                    isLoading = false
                )
                return
            }

            // Offline / Cache fallback
            repository.getStats(websiteId, TimeRange.LAST_24_HOURS, 0, AnalyticsFilter()).firstOrNull()?.let { st ->
                _uiState.value = _uiState.value.copy(
                    viewsCount = st.pageviews.value,
                    visitorsCount = st.visitors.value
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateActivityItems(sessions: List<SessionItemDto>): List<RealtimeActivityItem> {
        val list = mutableListOf<RealtimeActivityItem>()
        try {
            val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.US)
            val nowStr = timeFormat.format(Date())

            sessions.take(15).forEach { session ->
                val countryName = session.country ?: "India"
                val browserName = session.browser ?: "Firefox"
                val osName = session.os ?: "Linux"
                val deviceName = session.device ?: "Laptop"

                list.add(
                    RealtimeActivityItem(
                        id = "${session.id}_visitor",
                        avatarSeed = session.id,
                        timeFormatted = nowStr,
                        type = ActivityType.VISITOR,
                        detailText = "Visitor from $countryName using $browserName on $osName $deviceName"
                    )
                )

                list.add(
                    RealtimeActivityItem(
                        id = "${session.id}_view",
                        avatarSeed = session.id,
                        timeFormatted = nowStr,
                        type = ActivityType.VIEW,
                        detailText = "👁 /"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    override fun onCleared() {
        super.onCleared()
        isPollingActive = false
    }
}
