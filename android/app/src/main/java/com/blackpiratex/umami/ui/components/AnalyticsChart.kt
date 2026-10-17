package com.blackpiratex.umami.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.blackpiratex.umami.data.api.models.ChartPointDto

@Composable
fun AnalyticsChart(
    pageviews: List<ChartPointDto>,
    sessions: List<ChartPointDto>,
    modifier: Modifier = Modifier
) {
    val pvColor = MaterialTheme.colorScheme.primary.toArgb()
    val sessColor = MaterialTheme.colorScheme.secondary.toArgb()
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f).toArgb()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Views & Visitors",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Views",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Visitors",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pageviews.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data for selected period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description.isEnabled = false
                            setDrawGridBackground(false)
                            setDrawBarShadow(false)
                            setDrawValueAboveBar(true)
                            setPinchZoom(false)
                            setScaleEnabled(true)
                            legend.isEnabled = false
                            setTouchEnabled(true)

                            xAxis.apply {
                                position = XAxis.XAxisPosition.BOTTOM
                                textColor = labelTextColor
                                gridColor = gridLineColor
                                setDrawGridLines(false)
                                granularity = 1f
                            }

                            axisLeft.apply {
                                textColor = labelTextColor
                                gridColor = gridLineColor
                                axisMinimum = 0f
                            }

                            axisRight.isEnabled = false
                        }
                    },
                    update = { chart ->
                        try {
                            val pvEntries = ArrayList<BarEntry>()
                            val sessEntries = ArrayList<BarEntry>()

                            pageviews.forEachIndexed { index, pv ->
                                pvEntries.add(BarEntry(index.toFloat(), pv.y.toFloat()))
                                val sess = sessions.getOrNull(index)
                                sessEntries.add(BarEntry(index.toFloat(), (sess?.y ?: 0L).toFloat()))
                            }

                            if (pvEntries.isNotEmpty() && sessEntries.isNotEmpty()) {
                                val pvDataSet = BarDataSet(pvEntries, "Views").apply {
                                    color = pvColor
                                    valueTextColor = labelTextColor
                                    valueTextSize = 9f
                                    setDrawValues(false)
                                }

                                val sessDataSet = BarDataSet(sessEntries, "Visitors").apply {
                                    color = sessColor
                                    valueTextColor = labelTextColor
                                    valueTextSize = 9f
                                    setDrawValues(false)
                                }

                                val groupSpace = 0.3f
                                val barSpace = 0.05f
                                val barWidth = 0.3f

                                val barData = BarData(pvDataSet, sessDataSet).apply {
                                    this.barWidth = barWidth
                                }

                                chart.data = barData

                                chart.xAxis.valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val idx = value.toInt()
                                        if (idx < 0 || idx >= pageviews.size) return ""
                                        val point = pageviews.getOrNull(idx) ?: return ""
                                        return formatXAxisLabel(point.x)
                                    }
                                }

                                chart.groupBars(0f, groupSpace, barSpace)
                                chart.notifyDataSetChanged()
                                chart.invalidate()
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }
    }
}

private fun formatXAxisLabel(raw: String): String {
    if (raw.isBlank()) return ""
    return try {
        when {
            raw.contains(" ") -> {
                val parts = raw.split(" ")
                val timePart = parts.getOrNull(1) ?: ""
                if (timePart.length >= 5) timePart.substring(0, 5) else timePart
            }
            raw.contains("T") -> {
                val timePart = raw.split("T").getOrNull(1) ?: ""
                if (timePart.length >= 5) timePart.substring(0, 5) else timePart
            }
            raw.length >= 10 && raw.count { it == '-' } == 2 -> {
                val parts = raw.split("-")
                val month = parts.getOrNull(1) ?: ""
                val day = parts.getOrNull(2) ?: ""
                "$month/$day"
            }
            else -> raw
        }
    } catch (e: Exception) {
        raw
    }
}
