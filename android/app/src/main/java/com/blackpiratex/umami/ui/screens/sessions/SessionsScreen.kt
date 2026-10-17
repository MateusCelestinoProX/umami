package com.blackpiratex.umami.ui.screens.sessions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackpiratex.umami.data.api.models.SessionItemDto
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.ui.components.FilterChipGroup
import com.blackpiratex.umami.ui.components.FilterDialog
import com.blackpiratex.umami.ui.components.OfflineNoticeBar
import com.blackpiratex.umami.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.selectedWebsiteName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Filter and Time Range Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showFilterDialog = true },
                    shape = RoundedCornerShape(10.dp)
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
                            shape = RoundedCornerShape(10.dp)
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
                        Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next period")
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
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs: Activity | Properties (Matching Screenshot)
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Activity", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Properties", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Grid View toggle (Matching Screenshot)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "View Mode",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sessions Table / List
            if (uiState.sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sessions found for this period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Horizontal scrollable table container to perfectly layout columns
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val tableScrollState = rememberScrollState()

                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(tableScrollState)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Session", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(70.dp))
                        Text("Visits", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                        Text("Views", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                        Text("Events", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(60.dp))
                        Text("Location", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(170.dp))
                        Text("Browser", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(110.dp))
                        Text("OS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(130.dp))
                        Text("Device", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
                        Text("Last seen", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(130.dp))
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(uiState.sessions) { session ->
                            SessionRowItem(
                                session = session,
                                scrollState = tableScrollState
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRowItem(
    session: SessionItemDto,
    scrollState: androidx.compose.foundation.ScrollState
) {
    val locationStr = buildString {
        append(DateUtils.getFlagEmoji(session.country))
        append(" ")
        val locParts = listOfNotNull(
            session.city?.ifBlank { null },
            session.country?.ifBlank { null }
        )
        append(if (locParts.isNotEmpty()) locParts.joinToString(", ") else "Unknown Location")
    }

    val relativeLastSeen = DateUtils.formatRelativeTime(session.lastAt ?: session.createdAt)
    val browserName = session.browser ?: "Chrome"
    val osName = session.os ?: "Linux"
    val deviceName = session.device ?: "Laptop"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Session Avatar Badge
        Box(
            modifier = Modifier.width(70.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(getAvatarColor(session.id)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.id.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = "${session.visits}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = "${session.views}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = "${session.events}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )

        Text(
            text = locationStr,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(170.dp)
        )

        // Browser Column
        Row(
            modifier = Modifier.width(110.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("🌐", fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = browserName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        // OS Column
        Row(
            modifier = Modifier.width(130.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(getOsIconEmoji(osName), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = osName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        // Device Column
        Row(
            modifier = Modifier.width(100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getDeviceIcon(deviceName),
                contentDescription = deviceName,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = deviceName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }

        // Last seen Column
        Text(
            text = relativeLastSeen,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
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

private fun getOsIconEmoji(os: String): String {
    return when {
        os.contains("linux", true) -> "🐧"
        os.contains("windows", true) -> "🪟"
        os.contains("mac", true) || os.contains("ios", true) -> "🍎"
        os.contains("android", true) -> "🤖"
        else -> "💻"
    }
}

private fun getDeviceIcon(device: String): ImageVector {
    return when {
        device.contains("mobile", true) || device.contains("phone", true) -> Icons.Default.PhoneIphone
        device.contains("desktop", true) -> Icons.Default.Computer
        else -> Icons.Default.Laptop
    }
}
