package com.blackpiratex.umami.data.api

import com.blackpiratex.umami.data.api.models.ActiveUserDto
import com.blackpiratex.umami.data.api.models.AnalyticsFilter
import com.blackpiratex.umami.data.api.models.ChartPointDto
import com.blackpiratex.umami.data.api.models.LoginRequest
import com.blackpiratex.umami.data.api.models.LoginResponse
import com.blackpiratex.umami.data.api.models.MetricItemDto
import com.blackpiratex.umami.data.api.models.PageviewsResponseDto
import com.blackpiratex.umami.data.api.models.RealtimeDataDto
import com.blackpiratex.umami.data.api.models.SessionItemDto
import com.blackpiratex.umami.data.api.models.WebsiteDto
import com.blackpiratex.umami.data.api.models.WebsiteStatsDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

class UmamiApiService {

    private val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonInstance)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("KtorLog: $message")
                }
            }
            level = LogLevel.INFO
        }
    }

    suspend fun login(baseUrl: String, request: LoginRequest): LoginResponse {
        val cleanUrl = baseUrl.trimEnd('/')
        return client.post("$cleanUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getWebsites(baseUrl: String, token: String): List<WebsiteDto> {
        val cleanUrl = baseUrl.trimEnd('/')
        val responseText = client.get("$cleanUrl/api/websites") {
            header("Authorization", "Bearer $token")
        }.body<String>()

        val element = jsonInstance.parseToJsonElement(responseText)
        return when (element) {
            is JsonArray -> jsonInstance.decodeFromJsonElement<List<WebsiteDto>>(element)
            is JsonObject -> {
                val dataArray = element["data"]?.jsonArray
                if (dataArray != null) {
                    jsonInstance.decodeFromJsonElement<List<WebsiteDto>>(dataArray)
                } else emptyList()
            }
            else -> emptyList()
        }
    }

    suspend fun getActiveUsers(baseUrl: String, token: String, websiteId: String): Int {
        val cleanUrl = baseUrl.trimEnd('/')
        return try {
            val responseText = client.get("$cleanUrl/api/websites/$websiteId/active") {
                header("Authorization", "Bearer $token")
            }.body<String>()

            val element = jsonInstance.parseToJsonElement(responseText)
            when (element) {
                is JsonArray -> element.firstOrNull()?.jsonObject?.get("x")?.jsonPrimitive?.intOrNull ?: 1
                is JsonObject -> element["x"]?.jsonPrimitive?.intOrNull ?: element["count"]?.jsonPrimitive?.intOrNull ?: 1
                else -> 1
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun getRealtime(baseUrl: String, token: String, websiteId: String, startAt: Long): RealtimeDataDto? {
        val cleanUrl = baseUrl.trimEnd('/')
        return try {
            // Try /api/realtime/{websiteId} or /api/websites/{websiteId}/realtime
            val url1 = "$cleanUrl/api/realtime/$websiteId"
            val responseText = try {
                client.get(url1) {
                    header("Authorization", "Bearer $token")
                    parameter("startAt", startAt)
                }.body<String>()
            } catch (e: Exception) {
                val url2 = "$cleanUrl/api/websites/$websiteId/realtime"
                client.get(url2) {
                    header("Authorization", "Bearer $token")
                    parameter("startAt", startAt)
                }.body<String>()
            }

            jsonInstance.decodeFromString<RealtimeDataDto>(responseText)
        } catch (e: Exception) {
            println("Realtime endpoint error: ${e.message}")
            null
        }
    }

    suspend fun getStats(
        baseUrl: String,
        token: String,
        websiteId: String,
        startAt: Long,
        endAt: Long,
        filter: AnalyticsFilter = AnalyticsFilter()
    ): WebsiteStatsDto {
        val cleanUrl = baseUrl.trimEnd('/')
        return client.get("$cleanUrl/api/websites/$websiteId/stats") {
            header("Authorization", "Bearer $token")
            parameter("startAt", startAt)
            parameter("endAt", endAt)
            filter.page?.let { if (it.isNotBlank()) parameter("url", it) }
            filter.country?.let { if (it.isNotBlank()) parameter("country", it) }
            filter.region?.let { if (it.isNotBlank()) parameter("region", it) }
            filter.referrer?.let { if (it.isNotBlank()) parameter("referrer", it) }
        }.body()
    }

    suspend fun getPageviews(
        baseUrl: String,
        token: String,
        websiteId: String,
        startAt: Long,
        endAt: Long,
        unit: String,
        filter: AnalyticsFilter = AnalyticsFilter()
    ): PageviewsResponseDto {
        val cleanUrl = baseUrl.trimEnd('/')
        return client.get("$cleanUrl/api/websites/$websiteId/pageviews") {
            header("Authorization", "Bearer $token")
            parameter("startAt", startAt)
            parameter("endAt", endAt)
            parameter("unit", unit)
            parameter("tz", "UTC")
            filter.page?.let { if (it.isNotBlank()) parameter("url", it) }
            filter.country?.let { if (it.isNotBlank()) parameter("country", it) }
            filter.region?.let { if (it.isNotBlank()) parameter("region", it) }
            filter.referrer?.let { if (it.isNotBlank()) parameter("referrer", it) }
        }.body()
    }

    suspend fun getMetrics(
        baseUrl: String,
        token: String,
        websiteId: String,
        startAt: Long,
        endAt: Long,
        type: String,
        filter: AnalyticsFilter = AnalyticsFilter()
    ): List<MetricItemDto> {
        val cleanUrl = baseUrl.trimEnd('/')
        return client.get("$cleanUrl/api/websites/$websiteId/metrics") {
            header("Authorization", "Bearer $token")
            parameter("startAt", startAt)
            parameter("endAt", endAt)
            parameter("type", type)
            filter.page?.let { if (it.isNotBlank()) parameter("url", it) }
            filter.country?.let { if (it.isNotBlank()) parameter("country", it) }
            filter.region?.let { if (it.isNotBlank()) parameter("region", it) }
            filter.referrer?.let { if (it.isNotBlank()) parameter("referrer", it) }
        }.body()
    }

    suspend fun getSessions(
        baseUrl: String,
        token: String,
        websiteId: String,
        startAt: Long,
        endAt: Long,
        searchQuery: String? = null,
        filter: AnalyticsFilter = AnalyticsFilter()
    ): List<SessionItemDto> {
        val cleanUrl = baseUrl.trimEnd('/')
        val responseText = client.get("$cleanUrl/api/websites/$websiteId/sessions") {
            header("Authorization", "Bearer $token")
            parameter("startAt", startAt)
            parameter("endAt", endAt)
            searchQuery?.let { if (it.isNotBlank()) parameter("search", it) }
            filter.page?.let { if (it.isNotBlank()) parameter("url", it) }
            filter.country?.let { if (it.isNotBlank()) parameter("country", it) }
            filter.region?.let { if (it.isNotBlank()) parameter("region", it) }
            filter.referrer?.let { if (it.isNotBlank()) parameter("referrer", it) }
        }.body<String>()

        val element = jsonInstance.parseToJsonElement(responseText)
        return when (element) {
            is JsonArray -> jsonInstance.decodeFromJsonElement<List<SessionItemDto>>(element)
            is JsonObject -> {
                val dataArray = element["data"]?.jsonArray
                if (dataArray != null) {
                    jsonInstance.decodeFromJsonElement<List<SessionItemDto>>(dataArray)
                } else emptyList()
            }
            else -> emptyList()
        }
    }
}
