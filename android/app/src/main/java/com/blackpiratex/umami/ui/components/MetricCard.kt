package com.blackpiratex.umami.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.util.DateUtils
import com.blackpiratex.umami.util.IconHelper

enum class MetricType {
    PAGE,
    SOURCE,
    BROWSER,
    OS,
    DEVICE,
    COUNTRY,
    GENERIC
}

@Composable
fun MetricCard(
    title: String,
    items: List<MetricItemDto>,
    type: MetricType = MetricType.GENERIC,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxCount = items.maxOfOrNull { it.y } ?: 1L

                items.take(10).forEach { item ->
                    val rawLabel = item.x?.trim()
                    val label = when {
                        rawLabel.isNullOrBlank() -> "/"
                        type == MetricType.PAGE && !rawLabel.startsWith("/") && !rawLabel.startsWith("http") -> "/$rawLabel"
                        else -> rawLabel
                    }
                    val progress = (item.y.toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Dynamic Leading Icon based on MetricType
                                when (type) {
                                    MetricType.SOURCE -> {
                                        val faviconUrl = IconHelper.getFaviconUrl(label)
                                        if (faviconUrl != null) {
                                            AsyncImage(
                                                model = faviconUrl,
                                                contentDescription = label,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text("🔗", fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    MetricType.BROWSER -> {
                                        Text(IconHelper.getBrowserEmoji(label), fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    MetricType.OS -> {
                                        Image(
                                            painter = painterResource(id = IconHelper.getOsDrawableRes(label)),
                                            contentDescription = label,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    MetricType.COUNTRY -> {
                                        Text(DateUtils.getFlagEmoji(label), fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    else -> {}
                                }

                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${item.y}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
