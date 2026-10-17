package com.blackpiratex.umami.data.api.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String? = null,
    val user: UserDto? = null
)

@Serializable
data class UserDto(
    val id: String? = null,
    val username: String? = null,
    val role: String? = null,
    val createdAt: String? = null
)

@Serializable
data class WebsiteDto(
    val id: String,
    val name: String,
    val domain: String? = null,
    val shareId: String? = null,
    val createdAt: String? = null
)

object StatValueSerializer : KSerializer<StatValue> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StatValue", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): StatValue {
        val input = decoder as? JsonDecoder ?: return StatValue()
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> {
                val num = element.longOrNull ?: 0L
                StatValue(value = num, change = 0L)
            }
            is JsonObject -> {
                val valObj = element["value"]
                val valNum = if (valObj is JsonPrimitive) valObj.longOrNull ?: 0L else 0L
                val changeObj = element["change"]
                val changeNum = if (changeObj is JsonPrimitive) changeObj.longOrNull ?: 0L else 0L
                StatValue(value = valNum, change = changeNum)
            }
            else -> StatValue()
        }
    }

    override fun serialize(encoder: Encoder, value: StatValue) {
        encoder.encodeLong(value.value)
    }
}

@Serializable(with = StatValueSerializer::class)
data class StatValue(
    val value: Long = 0,
    val change: Long = 0
)

@Serializable
data class WebsiteStatsDto(
    val pageviews: StatValue = StatValue(),
    val visitors: StatValue = StatValue(),
    val visits: StatValue = StatValue(),
    val bounces: StatValue = StatValue(),
    val totaltime: StatValue = StatValue(),
    val events: StatValue = StatValue()
)

@Serializable
data class ChartPointDto(
    val x: String,
    val y: Long = 0
)

@Serializable
data class PageviewsResponseDto(
    val pageviews: List<ChartPointDto> = emptyList(),
    val sessions: List<ChartPointDto> = emptyList()
)

object MetricItemSerializer : KSerializer<MetricItemDto> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MetricItemDto")

    override fun deserialize(decoder: Decoder): MetricItemDto {
        val input = decoder as? JsonDecoder ?: return MetricItemDto()
        val element = input.decodeJsonElement()
        if (element is JsonObject) {
            val labelStr = element["x"]?.jsonPrimitive?.contentOrNull
                ?: element["url"]?.jsonPrimitive?.contentOrNull
                ?: element["name"]?.jsonPrimitive?.contentOrNull
                ?: element["page"]?.jsonPrimitive?.contentOrNull
                ?: element["domain"]?.jsonPrimitive?.contentOrNull
                ?: element["element"]?.jsonPrimitive?.contentOrNull
                ?: ""
            val valNum = element["y"]?.jsonPrimitive?.longOrNull
                ?: element["views"]?.jsonPrimitive?.longOrNull
                ?: element["count"]?.jsonPrimitive?.longOrNull
                ?: element["pageviews"]?.jsonPrimitive?.longOrNull
                ?: 0L
            return MetricItemDto(x = labelStr, y = valNum)
        }
        return MetricItemDto()
    }

    override fun serialize(encoder: Encoder, value: MetricItemDto) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeStringElement(descriptor, 0, value.x ?: "")
        composite.encodeLongElement(descriptor, 1, value.y)
        composite.endStructure(descriptor)
    }
}

@Serializable(with = MetricItemSerializer::class)
data class MetricItemDto(
    val x: String? = "",
    val y: Long = 0
)

@Serializable
data class SessionItemDto(
    val id: String,
    val hostname: String? = null,
    val browser: String? = null,
    val os: String? = null,
    val device: String? = null,
    val screen: String? = null,
    val language: String? = null,
    val country: String? = null,
    val subdivision1: String? = null,
    val city: String? = null,
    val views: Int = 1,
    val visits: Int = 1,
    val events: Int = 0,
    val createdAt: String? = null,
    val firstAt: String? = null,
    val lastAt: String? = null
)

@Serializable
data class ActiveUserDto(
    val x: Int = 0
)

@Serializable
data class RealtimeDataDto(
    val pageviews: List<SessionItemDto> = emptyList(),
    val sessions: List<SessionItemDto> = emptyList(),
    val events: List<SessionItemDto> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class RealtimeActivityItem(
    val id: String,
    val avatarSeed: String,
    val timeFormatted: String,
    val type: ActivityType,
    val detailText: String
)

enum class ActivityType {
    VIEW,
    VISITOR,
    EVENT
}

enum class TimeRange(
    val label: String,
    val unit: String
) {
    LAST_24_HOURS("Last 24 hours", "hour"),
    THIS_WEEK("This week", "day"),
    LAST_7_DAYS("Last 7 days", "day"),
    LAST_30_DAYS("Last 30 days", "day"),
    THIS_MONTH("This month", "day"),
    THIS_YEAR("This year", "month"),
    LAST_6_MONTHS("Last 6 months", "month"),
    LAST_12_MONTHS("Last 12 months", "month"),
    ALL_TIME("All time", "month")
}

data class AnalyticsFilter(
    val page: String? = null,
    val country: String? = null,
    val region: String? = null,
    val referrer: String? = null
) {
    fun isEmpty() = page.isNullOrBlank() && country.isNullOrBlank() && region.isNullOrBlank() && referrer.isNullOrBlank()

    fun toCacheKey(): String {
        return "p=${page ?: ""}_c=${country ?: ""}_r=${region ?: ""}_ref=${referrer ?: ""}"
    }
}
