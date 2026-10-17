package com.blackpiratex.umami.util

import com.blackpiratex.umami.data.api.models.TimeRange
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    fun getStartAndEndTimestamps(timeRange: TimeRange, offsetIndex: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endAt = cal.timeInMillis

        val startCal = Calendar.getInstance()
        when (timeRange) {
            TimeRange.LAST_24_HOURS -> {
                startCal.add(Calendar.HOUR_OF_DAY, -24)
                val duration = 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.THIS_WEEK -> {
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                val duration = 7 * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.LAST_7_DAYS -> {
                startCal.add(Calendar.DAY_OF_YEAR, -7)
                val duration = 7 * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.LAST_30_DAYS -> {
                startCal.add(Calendar.DAY_OF_YEAR, -30)
                val duration = 30L * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.THIS_MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                val duration = 30L * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.THIS_YEAR -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                val duration = 365L * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.LAST_6_MONTHS -> {
                startCal.add(Calendar.MONTH, -6)
                val duration = 180L * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.LAST_12_MONTHS -> {
                startCal.add(Calendar.MONTH, -12)
                val duration = 365L * 24 * 3600 * 1000L
                val shift = offsetIndex * duration
                return Pair(startCal.timeInMillis - shift, endAt - shift)
            }
            TimeRange.ALL_TIME -> {
                startCal.set(2020, Calendar.JANUARY, 1, 0, 0, 0)
                return Pair(startCal.timeInMillis, endAt)
            }
        }
    }

    fun formatRelativeTime(isoOrMs: String?): String {
        if (isoOrMs.isNullOrEmpty()) return "recently"
        val timestamp = parseToMillis(isoOrMs)
        val diffMs = System.currentTimeMillis() - timestamp
        if (diffMs < 0) return "just now"

        val seconds = diffMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "just now"
            minutes < 60 -> "about $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
            hours < 24 -> "about $hours ${if (hours == 1L) "hour" else "hours"} ago"
            days < 30 -> "about $days ${if (days == 1L) "day" else "days"} ago"
            else -> {
                val months = days / 30
                "about $months ${if (months == 1L) "month" else "months"} ago"
            }
        }
    }

    private fun parseToMillis(raw: String): Long {
        return try {
            raw.toLong()
        } catch (e: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(raw)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    format.parse(raw)?.time ?: System.currentTimeMillis()
                } catch (e3: Exception) {
                    System.currentTimeMillis()
                }
            }
        }
    }

    fun getFlagEmoji(countryCodeOrName: String?): String {
        if (countryCodeOrName.isNullOrBlank()) return "🌐"
        val code = when (countryCodeOrName.lowercase().trim()) {
            "india", "in" -> "IN"
            "singapore", "sg" -> "SG"
            "japan", "jp" -> "JP"
            "spain", "es" -> "ES"
            "france", "fr" -> "FR"
            "united states", "us", "usa" -> "US"
            "germany", "de" -> "DE"
            "united kingdom", "gb", "uk" -> "GB"
            "canada", "ca" -> "CA"
            "australia", "au" -> "AU"
            "brazil", "br" -> "BR"
            "china", "cn" -> "CN"
            else -> {
                if (countryCodeOrName.length == 2) countryCodeOrName.uppercase() else null
            }
        } ?: return "🌐"

        val firstChar = Character.codePointAt(code, 0) - 0x41 + 0x1F1E6
        val secondChar = Character.codePointAt(code, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    fun formatDurationSeconds(seconds: Long): String {
        if (seconds <= 0) return "0s"
        val mins = seconds / 60
        val sec = seconds % 60
        return if (mins > 0) "${mins}m ${sec}s" else "${sec}s"
    }
}
