package com.blackpiratex.umami.ui.screens.realtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackpiratex.umami.data.api.models.ActivityType
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.data.api.models.RealtimeActivityItem
import com.blackpiratex.umami.ui.components.AnalyticsChart
import com.blackpiratex.umami.ui.components.OfflineNoticeBar
import com.blackpiratex.umami.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealtimeScreen(
    viewModel: RealtimeViewModel,
    onOpenDrawer: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.selectedWebsiteName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(10.dp))

                            // Online Badge (🟢 1 Online)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${uiState.activeOnlineCount} Online",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(onClick = {}, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.loadWebsites() }) {
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
            // 4 Top Realtime Stat Cards (Views, Visitors, Events, Countries) - Matching Screenshot 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RealtimeStatCard("Views", uiState.viewsCount.toString(), Modifier.weight(1f))
                RealtimeStatCard("Visitors", uiState.visitorsCount.toString(), Modifier.weight(1f))
                RealtimeStatCard("Events", uiState.eventsCount.toString(), Modifier.weight(1f))
                RealtimeStatCard("Countries", uiState.countriesCount.toString(), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Realtime 30-Minute Minute-by-Minute Graph - Matching Screenshot 0
            AnalyticsChart(
                pageviews = uiState.pageviewsChart,
                sessions = uiState.sessionsChart
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Activity Feed Section - Matching Screenshot 1
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Filter Pills: All | Views | Visitors | Events
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = uiState.activityFilter == null,
                                onClick = { viewModel.selectActivityFilter(null) },
                                label = { Text("All", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = uiState.activityFilter == ActivityType.VIEW,
                                onClick = { viewModel.selectActivityFilter(ActivityType.VIEW) },
                                label = { Text("Views", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = uiState.activityFilter == ActivityType.VISITOR,
                                onClick = { viewModel.selectActivityFilter(ActivityType.VISITOR) },
                                label = { Text("Visitors", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = uiState.activityFilter == ActivityType.EVENT,
                                onClick = { viewModel.selectActivityFilter(ActivityType.EVENT) },
                                label = { Text("Events", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input box
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search activity...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Activity Items List
                    val filteredActivities = uiState.activities.filter { item ->
                        val filterMatch = when (uiState.activityFilter) {
                            ActivityType.VIEW -> item.type == ActivityType.VIEW
                            ActivityType.VISITOR -> item.type == ActivityType.VISITOR
                            ActivityType.EVENT -> item.type == ActivityType.EVENT
                            null -> true
                        }
                        val searchMatch = uiState.searchQuery.isBlank() || item.detailText.contains(uiState.searchQuery, true)
                        filterMatch && searchMatch
                    }

                    if (filteredActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No recent activity", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredActivities.take(8).forEach { activity ->
                                ActivityRow(item = activity)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Realtime Pages Breakdown Card (Matching Screenshot 1)
            RealtimeMetricCard(title = "Pages", labelHeader = "Path", valueHeader = "Views", items = uiState.topPages)

            Spacer(modifier = Modifier.height(16.dp))

            // Realtime Referrers Breakdown Card (Matching Screenshot 1)
            RealtimeMetricCard(title = "Referrers", labelHeader = "Domain", valueHeader = "Views", items = uiState.referrers)

            Spacer(modifier = Modifier.height(16.dp))

            // Realtime Countries Breakdown Card (Matching Screenshot 1)
            RealtimeMetricCard(title = "Countries", labelHeader = "Country", valueHeader = "Visitors", items = uiState.countries, isCountry = true)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RealtimeStatCard(
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
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActivityRow(item: RealtimeActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(getAvatarColor(item.avatarSeed)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.avatarSeed.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = item.timeFormatted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(90.dp)
        )

        Text(
            text = item.detailText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RealtimeMetricCard(
    title: String,
    labelHeader: String,
    valueHeader: String,
    items: List<MetricItemDto>,
    isCountry: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueHeader,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val totalSum = items.sumOf { it.y }.coerceAtLeast(1L)
                items.take(8).forEach { item ->
                    val label = item.x ?: "/"
                    val flag = if (isCountry) DateUtils.getFlagEmoji(label) else ""
                    val displayLabel = if (flag.isNotEmpty()) "$flag $label" else label
                    val percentage = ((item.y.toFloat() / totalSum.toFloat()) * 100).toInt()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.y}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$percentage%",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getAvatarColor(seed: String): Color {
    val colors = listOf(
        Color(0xFF6366F1), Color(0xFFEC4899), Color(0xFF8B5CF6),
        Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF3B82F6)
    )
    val index = (seed.hashCode() and 0x7FFFFFFF) % colors.size
    return colors[index]
}
