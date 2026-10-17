package com.blackpiratex.umami.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackpiratex.umami.UmamiApplication
import com.blackpiratex.umami.data.api.models.TimeRange
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.ui.theme.UmamiTheme
import com.blackpiratex.umami.util.DateUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val intentExtras = intent.extras
        if (intentExtras != null) {
            appWidgetId = intentExtras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val app = application as UmamiApplication

        setContent {
            UmamiTheme(themeMode = app.sessionManager.getThemeMode()) {
                WidgetConfigScreen(
                    app = app,
                    onSave = { selectedSite, selectedRange ->
                        saveAndFinish(app, selectedSite, selectedRange)
                    }
                )
            }
        }
    }

    private fun saveAndFinish(app: UmamiApplication, site: WebsiteDto, range: TimeRange) {
        val widgetPrefs = WidgetPreferences(this)
        widgetPrefs.saveWidgetConfig(appWidgetId, site.id, site.name, range)

        val scope = (application as UmamiApplication)
        val (startAt, endAt) = DateUtils.getStartAndEndTimestamps(range, 0)
        val serverUrl = app.sessionManager.getServerUrl()
        val token = app.sessionManager.getToken()

        kotlinx.coroutines.GlobalScope.launch {
            if (serverUrl != null && token != null && app.networkObserver.isCurrentlyOnline()) {
                try {
                    val stats = app.apiService.getStats(serverUrl, token, site.id, startAt, endAt)
                    widgetPrefs.saveWidgetStats(appWidgetId, stats.pageviews.value, stats.visits.value)
                } catch (e: Exception) {
                    // Ignore, fallback to cache
                }
            }

            val appWidgetManager = AppWidgetManager.getInstance(this@WidgetConfigActivity)
            UmamiStatsWidgetProvider.updateWidget(this@WidgetConfigActivity, appWidgetManager, appWidgetId)

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    app: UmamiApplication,
    onSave: (WebsiteDto, TimeRange) -> Unit
) {
    var websites by remember { mutableStateOf<List<WebsiteDto>>(emptyList()) }
    var selectedSite by remember { mutableStateOf<WebsiteDto?>(null) }
    var selectedRange by remember { mutableStateOf(TimeRange.LAST_24_HOURS) }

    var siteDropdownExpanded by remember { mutableStateOf(false) }
    var rangeDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val serverUrl = app.sessionManager.getServerUrl()
        val token = app.sessionManager.getToken()
        if (serverUrl != null && token != null) {
            try {
                val sites = app.apiService.getWebsites(serverUrl, token)
                websites = sites
                selectedSite = sites.firstOrNull()
            } catch (e: Exception) {
                val localSites = app.database.websiteDao().getAllWebsites().firstOrNull() ?: emptyList()
                val dtoList = localSites.map { WebsiteDto(it.id, it.name, it.domain, it.shareId, it.createdAt) }
                websites = dtoList
                selectedSite = dtoList.firstOrNull()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Text(
                text = "Select Website",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                OutlinedButton(
                    onClick = { siteDropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = selectedSite?.name ?: "Loading websites...",
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = siteDropdownExpanded,
                    onDismissRequest = { siteDropdownExpanded = false }
                ) {
                    websites.forEach { site ->
                        DropdownMenuItem(
                            text = { Text(site.name) },
                            onClick = {
                                selectedSite = site
                                siteDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Select Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                OutlinedButton(
                    onClick = { rangeDropdownExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = selectedRange.label, fontWeight = FontWeight.Bold)
                }

                DropdownMenu(
                    expanded = rangeDropdownExpanded,
                    onDismissRequest = { rangeDropdownExpanded = false }
                ) {
                    TimeRange.values().forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.label) },
                            onClick = {
                                selectedRange = range
                                rangeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedSite?.let { site ->
                        onSave(site, selectedRange)
                    }
                },
                enabled = selectedSite != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Widget", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
