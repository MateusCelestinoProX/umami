package com.blackpiratex.umami.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blackpiratex.umami.data.api.models.AnalyticsFilter

@Composable
fun FilterDialog(
    currentFilter: AnalyticsFilter,
    onDismiss: () -> Unit,
    onApplyFilter: (AnalyticsFilter) -> Unit
) {
    var pageInput by remember { mutableStateOf(currentFilter.page ?: "") }
    var countryInput by remember { mutableStateOf(currentFilter.country ?: "") }
    var regionInput by remember { mutableStateOf(currentFilter.region ?: "") }
    var referrerInput by remember { mutableStateOf(currentFilter.referrer ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Filter Analytics Data") },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Filter by Page (e.g. /home)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = countryInput,
                    onValueChange = { countryInput = it },
                    label = { Text("Filter by Country (e.g. US or India)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = regionInput,
                    onValueChange = { regionInput = it },
                    label = { Text("Filter by Region") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = referrerInput,
                    onValueChange = { referrerInput = it },
                    label = { Text("Filter by Referrer (e.g. google.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newFilter = AnalyticsFilter(
                        page = pageInput.trim().ifEmpty { null },
                        country = countryInput.trim().ifEmpty { null },
                        region = regionInput.trim().ifEmpty { null },
                        referrer = referrerInput.trim().ifEmpty { null }
                    )
                    onApplyFilter(newFilter)
                }
            ) {
                Text("Apply Filters")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
