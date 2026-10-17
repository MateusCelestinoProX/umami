package com.blackpiratex.umami.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackpiratex.umami.data.api.models.AnalyticsFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterChipGroup(
    filter: AnalyticsFilter,
    onRemovePageFilter: () -> Unit,
    onRemoveCountryFilter: () -> Unit,
    onRemoveRegionFilter: () -> Unit,
    onRemoveReferrerFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (filter.isEmpty()) return

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filter.page?.let { p ->
            FilterChip(
                selected = true,
                onClick = onRemovePageFilter,
                label = { Text("Page: $p") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        filter.country?.let { c ->
            FilterChip(
                selected = true,
                onClick = onRemoveCountryFilter,
                label = { Text("Country: $c") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        filter.region?.let { r ->
            FilterChip(
                selected = true,
                onClick = onRemoveRegionFilter,
                label = { Text("Region: $r") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
        filter.referrer?.let { ref ->
            FilterChip(
                selected = true,
                onClick = onRemoveReferrerFilter,
                label = { Text("Referrer: $ref") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}
