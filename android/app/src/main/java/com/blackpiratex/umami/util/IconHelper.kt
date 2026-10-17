package com.blackpiratex.umami.util

import com.blackpiratex.umami.R

object IconHelper {

    fun getFaviconUrl(domainOrUrl: String?): String? {
        if (domainOrUrl.isNullOrBlank()) return null
        val cleanDomain = domainOrUrl
            .replace(Regex("^https?://"), "")
            .split("/")
            .firstOrNull()
            ?.trim() ?: return null

        if (cleanDomain.isBlank() || cleanDomain.contains("Direct / None", true) || cleanDomain.contains("(direct)", true)) {
            return null
        }

        return "https://www.google.com/s2/favicons?domain=$cleanDomain&sz=64"
    }

    fun getOsDrawableRes(os: String?): Int {
        if (os.isNullOrBlank()) return R.drawable.ic_os_linux
        val name = os.lowercase()
        return when {
            name.contains("windows") -> R.drawable.ic_os_windows
            name.contains("mac") || name.contains("ios") || name.contains("apple") -> R.drawable.ic_os_apple
            name.contains("linux") || name.contains("ubuntu") || name.contains("debian") || name.contains("fedora") || name.contains("arch") -> R.drawable.ic_os_linux
            name.contains("android") -> R.drawable.ic_os_android
            else -> R.drawable.ic_os_linux
        }
    }

    fun getBrowserEmoji(browser: String?): String {
        if (browser.isNullOrBlank()) return "🌐"
        val name = browser.lowercase()
        return when {
            name.contains("chrome") -> "🌐"
            name.contains("firefox") -> "🦊"
            name.contains("safari") -> "🧭"
            name.contains("edge") -> "🌐"
            name.contains("brave") -> "🦁"
            name.contains("opera") -> "🔴"
            name.contains("arc") -> "🌈"
            name.contains("tor") -> "🧅"
            name.contains("vivaldi") -> "🔴"
            name.contains("samsung") -> "📱"
            name.contains("duckduckgo") -> "🦆"
            name.contains("android") -> "🤖"
            else -> "🌐"
        }
    }
}
