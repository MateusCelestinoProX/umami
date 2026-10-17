package com.blackpiratex.umami.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpiratex.umami.data.api.UmamiRepository
import com.blackpiratex.umami.data.api.models.AnalyticsFilter
import com.blackpiratex.umami.data.api.models.ChartPointDto
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.data.api.models.WebsiteStatsDto
import com.blackpiratex.umami.data.preferences.SessionManager
import com.blackpiratex.umami.util.NetworkObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class OverviewUiState(
    val websites: List<WebsiteDto> = emptyList(),
    val selectedWebsiteId: String? = null,
    val selectedWebsiteName: String = "Homepage",
    val stats: WebsiteStatsDto = WebsiteStatsDto(),
    val pageviews: List<ChartPointDto> = emptyList(),
    val sessions: List<ChartPointDto> = emptyList(),
    val topPages: List<MetricItemDto> = emptyList(),
    val sources: List<MetricItemDto> = emptyList(),
    val browsers: List<MetricItemDto> = emptyList(),
    val osList: List<MetricItemDto> = emptyList(),
    val devices: List<MetricItemDto> = emptyList(),
    val countries: List<MetricItemDto> = emptyList(),
    val regions: List<MetricItemDto> = emptyList(),
    val selectedTimeRange: TimeRange = TimeRange.LAST_24_HOURS,
    val dateOffsetIndex: Int = 0,
    val activeFilter: AnalyticsFilter = AnalyticsFilter(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class OverviewViewModel(
    private val repository: UmamiRepository,
    private val sessionManager: SessionManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        observeNetworkState()
        loadWebsites()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkObserver.isOnline.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOffline = !isOnline)
            }
        }
    }

    fun loadWebsites() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getWebsites().collect { result ->
                result.onSuccess { sites ->
                    val savedId = sessionManager.getSelectedWebsiteId()
                    val targetSite = sites.find { it.id == savedId } ?: sites.firstOrNull()
                    val targetId = targetSite?.id

                    if (targetId != null) {
                        sessionManager.saveSelectedWebsite(targetId)
                    }

                    _uiState.value = _uiState.value.copy(
                        websites = sites,
                        selectedWebsiteId = targetId,
                        selectedWebsiteName = targetSite?.name ?: "Homepage",
                        isLoading = false
                    )

                    if (targetId != null) {
                        loadOverviewData(targetId)
                    }
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message
                    )
                }
            }
        }
    }

    fun selectWebsite(website: WebsiteDto) {
        sessionManager.saveSelectedWebsite(website.id)
        _uiState.value = _uiState.value.copy(
            selectedWebsiteId = website.id,
            selectedWebsiteName = website.name
        )
        loadOverviewData(website.id)
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range, dateOffsetIndex = 0)
        _uiState.value.selectedWebsiteId?.let { loadOverviewData(it) }
    }

    fun navigateTimeOffset(delta: Int) {
        val newOffset = maxOf(0, _uiState.value.dateOffsetIndex + delta)
        _uiState.value = _uiState.value.copy(dateOffsetIndex = newOffset)
        _uiState.value.selectedWebsiteId?.let { loadOverviewData(it) }
    }

    fun applyFilter(filter: AnalyticsFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
        _uiState.value.selectedWebsiteId?.let { loadOverviewData(it) }
    }

    fun removePageFilter() {
        applyFilter(_uiState.value.activeFilter.copy(page = null))
    }

    fun removeCountryFilter() {
        applyFilter(_uiState.value.activeFilter.copy(country = null))
    }

    fun removeRegionFilter() {
        applyFilter(_uiState.value.activeFilter.copy(region = null))
    }

    fun removeReferrerFilter() {
        applyFilter(_uiState.value.activeFilter.copy(referrer = null))
    }

    fun refreshData() {
        _uiState.value.selectedWebsiteId?.let { loadOverviewData(it) }
    }

    private fun loadOverviewData(websiteId: String) {
        val range = _uiState.value.selectedTimeRange
        val offset = _uiState.value.dateOffsetIndex
        val filter = _uiState.value.activeFilter

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                repository.getStats(websiteId, range, offset, filter),
                repository.getPageviews(websiteId, range, offset, filter),
                repository.getMetrics(websiteId, range, offset, "url", filter),
                repository.getMetrics(websiteId, range, offset, "referrer", filter)
            ) { stats, pageviews, pages, referrers ->
                OverviewDataChunk1(stats, pageviews.pageviews, pageviews.sessions, pages, referrers)
            }.collect { chunk1 ->
                _uiState.value = _uiState.value.copy(
                    stats = chunk1.stats,
                    pageviews = chunk1.pageviews,
                    sessions = chunk1.sessions,
                    topPages = chunk1.pages,
                    sources = chunk1.referrers
                )
            }
        }

        viewModelScope.launch {
            combine(
                repository.getMetrics(websiteId, range, offset, "browser", filter),
                repository.getMetrics(websiteId, range, offset, "os", filter),
                repository.getMetrics(websiteId, range, offset, "device", filter),
                repository.getMetrics(websiteId, range, offset, "country", filter),
                repository.getMetrics(websiteId, range, offset, "region", filter)
            ) { browsers, os, devices, countries, regions ->
                OverviewDataChunk2(browsers, os, devices, countries, regions)
            }.collect { chunk2 ->
                _uiState.value = _uiState.value.copy(
                    browsers = chunk2.browsers,
                    osList = chunk2.os,
                    devices = chunk2.devices,
                    countries = chunk2.countries,
                    regions = chunk2.regions,
                    isLoading = false
                )
            }
        }
    }
}

private data class OverviewDataChunk1(
    val stats: WebsiteStatsDto,
    val pageviews: List<ChartPointDto>,
    val sessions: List<ChartPointDto>,
    val pages: List<MetricItemDto>,
    val referrers: List<MetricItemDto>
)

private data class OverviewDataChunk2(
    val browsers: List<MetricItemDto>,
    val os: List<MetricItemDto>,
    val devices: List<MetricItemDto>,
    val countries: List<MetricItemDto>,
    val regions: List<MetricItemDto>
)
