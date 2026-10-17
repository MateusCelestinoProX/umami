package com.blackpiratex.umami.ui.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.ui.components.AnalyticsChart
import com.blackpiratex.umami.ui.components.FilterChipGroup
import com.blackpiratex.umami.ui.components.FilterDialog
import com.blackpiratex.umami.ui.components.MetricCard
import com.blackpiratex.umami.ui.components.MetricType
import com.blackpiratex.umami.ui.components.OfflineNoticeBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    viewModel: OverviewViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterDialog by remember { mutableStateOf(false) }
    var timeDropdownExpanded by remember { mutableStateOf(false) }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = uiState.activeFilter,
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { newFilter ->
                showFilterDialog = false
                viewModel.applyFilter(newFilter)
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = uiState.selectedWebsiteName,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                OfflineNoticeBar(isOffline = uiState.isOffline)

                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Time range & Filter bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text("Filter")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateTimeOffset(1) }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous period")
                    }

                    Box {
                        OutlinedButton(
                            onClick = { timeDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(uiState.selectedTimeRange.label)
                        }

                        DropdownMenu(
                            expanded = timeDropdownExpanded,
                            onDismissRequest = { timeDropdownExpanded = false }
                        ) {
                            TimeRange.values().forEach { range ->
                                DropdownMenuItem(
                                    text = { Text(range.label) },
                                    onClick = {
                                        timeDropdownExpanded = false
                                        viewModel.setTimeRange(range)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.navigateTimeOffset(-1) },
                        enabled = uiState.dateOffsetIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = "Next period")
                    }
                }
            }

            // Removable Filter Chips
            FilterChipGroup(
                filter = uiState.activeFilter,
                onRemovePageFilter = { viewModel.removePageFilter() },
                onRemoveCountryFilter = { viewModel.removeCountryFilter() },
                onRemoveRegionFilter = { viewModel.removeRegionFilter() },
                onRemoveReferrerFilter = { viewModel.removeReferrerFilter() },
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stat Cards Row (Views, Visitors, Visits)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    title = "Views",
                    value = uiState.stats.pageviews.value.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Visitors",
                    value = uiState.stats.visitors.value.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Visits",
                    value = uiState.stats.visits.value.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visitors and Views Graph
            AnalyticsChart(
                pageviews = uiState.pageviews,
                sessions = uiState.sessions
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pages and Visitors
            MetricCard(
                title = "Pages",
                items = uiState.topPages,
                type = MetricType.PAGE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sources (Referrers with Favicons)
            MetricCard(
                title = "Sources",
                items = uiState.sources,
                type = MetricType.SOURCE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Environment: Browsers (with Icons)
            MetricCard(
                title = "Browsers",
                items = uiState.browsers,
                type = MetricType.BROWSER
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Operating Systems (with Icons)
            MetricCard(
                title = "Operating Systems",
                items = uiState.osList,
                type = MetricType.OS
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Devices
            MetricCard(
                title = "Devices",
                items = uiState.devices,
                type = MetricType.DEVICE
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Countries (with Flag Emojis)
            MetricCard(
                title = "Countries",
                items = uiState.countries,
                type = MetricType.COUNTRY
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
