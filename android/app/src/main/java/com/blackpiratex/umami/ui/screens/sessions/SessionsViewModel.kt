package com.blackpiratex.umami.ui.screens.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blackpiratex.umami.data.api.UmamiRepository
import com.blackpiratex.umami.data.api.models.AnalyticsFilter
import com.blackpiratex.umami.data.api.models.SessionItemDto
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.data.preferences.SessionManager
import com.blackpiratex.umami.util.NetworkObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionsUiState(
    val websites: List<WebsiteDto> = emptyList(),
    val selectedWebsiteId: String? = null,
    val selectedWebsiteName: String = "Homepage",
    val selectedTab: Int = 0, // 0: Activity, 1: Properties
    val searchQuery: String = "",
    val sessions: List<SessionItemDto> = emptyList(),
    val selectedTimeRange: TimeRange = TimeRange.LAST_24_HOURS,
    val dateOffsetIndex: Int = 0,
    val activeFilter: AnalyticsFilter = AnalyticsFilter(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)

class SessionsViewModel(
    private val repository: UmamiRepository,
    private val sessionManager: SessionManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionsUiState())
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        observeNetworkState()
        loadWebsitesAndSessions()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkObserver.isOnline.collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOffline = !isOnline)
            }
        }
    }

    fun loadWebsitesAndSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getWebsites().collect { result ->
                result.onSuccess { sites ->
                    val savedId = sessionManager.getSelectedWebsiteId()
                    val targetSite = sites.find { it.id == savedId } ?: sites.firstOrNull()
                    val targetId = targetSite?.id

                    _uiState.value = _uiState.value.copy(
                        websites = sites,
                        selectedWebsiteId = targetId,
                        selectedWebsiteName = targetSite?.name ?: "Homepage",
                        isLoading = false
                    )

                    if (targetId != null) {
                        loadSessions(targetId)
                    }
                }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tabIndex)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _uiState.value.selectedWebsiteId?.let { loadSessions(it) }
    }

    fun setTimeRange(range: TimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range, dateOffsetIndex = 0)
        _uiState.value.selectedWebsiteId?.let { loadSessions(it) }
    }

    fun navigateTimeOffset(delta: Int) {
        val newOffset = maxOf(0, _uiState.value.dateOffsetIndex + delta)
        _uiState.value = _uiState.value.copy(dateOffsetIndex = newOffset)
        _uiState.value.selectedWebsiteId?.let { loadSessions(it) }
    }

    fun applyFilter(filter: AnalyticsFilter) {
        _uiState.value = _uiState.value.copy(activeFilter = filter)
        _uiState.value.selectedWebsiteId?.let { loadSessions(it) }
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
        _uiState.value.selectedWebsiteId?.let { loadSessions(it) }
    }

    private fun loadSessions(websiteId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val range = _uiState.value.selectedTimeRange
            val offset = _uiState.value.dateOffsetIndex
            val query = _uiState.value.searchQuery
            val filter = _uiState.value.activeFilter

            repository.getSessions(websiteId, range, offset, query, filter).collect { sessionList ->
                _uiState.value = _uiState.value.copy(
                    sessions = sessionList,
                    isLoading = false
                )
            }
        }
    }
}
