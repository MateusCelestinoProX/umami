package com.blackpiratex.umami.util

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class WebResourceCacheManager(private val context: Context) {

    private val cacheDir = File(context.cacheDir, "umami_web_cache").apply {
        if (!exists()) mkdirs()
    }

    private val threadPool = Executors.newFixedThreadPool(6)
    private val downloadingUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val shellCacheFile = File(cacheDir, "app_shell_websites.html")

    /**
     * Pre-warms DNS and caches initial application shell in background
     */
    fun prewarmConnection(primaryHost: String, targetUrl: String) {
        threadPool.execute {
            try {
                // 1. Pre-resolve DNS
                InetAddress.getAllByName(primaryHost)

                // 2. Pre-fetch and update HTML shell cache if missing or older than 12h
                if (!shellCacheFile.exists() || (System.currentTimeMillis() - shellCacheFile.lastModified() > 12 * 3600 * 1000L)) {
                    val conn = URL(targetUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "UmamiMobile/1.4 (Android; UltraPerformance)")
                    val encodedToken = java.net.URLEncoder.encode(com.blackpiratex.umami.UmamiApplication.AUTH_TOKEN, "UTF-8")
                    conn.setRequestProperty("Cookie", "umami.auth=$encodedToken")
                    conn.connect()
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val bytes = conn.inputStream.readBytes()
                        conn.disconnect()
                        FileOutputStream(shellCacheFile).use { it.write(bytes) }
                    }
                }
            } catch (_: Exception) {
                // Background warmup is best-effort
            }
        }
    }

    /**
     * Intercepts and serves static assets directly from disk/assets with 0 latency.
     * Never blocks Chromium worker threads on missing cache.
     */
    fun interceptRequest(request: WebResourceRequest?): WebResourceResponse? {
        val uri = request?.url ?: return null
        val path = uri.path ?: return null
        val urlStr = uri.toString()

        // 1. Serve packaged WebGL shaders directly from APK assets (0ms latency)
        if (path.contains("mcp-os-webgl.js")) {
            return try {
                val stream = context.assets.open("mcp-os/mcp-os-webgl.js")
                createResponse("application/javascript", stream)
            } catch (e: Exception) {
                null
            }
        }

        if (path.contains("ogl.js")) {
            return try {
                val stream = context.assets.open("mcp-os/ogl.js")
                createResponse("application/javascript", stream)
            } catch (e: Exception) {
                null
            }
        }

        // 2. Instant App Shell Caching for the websites dashboard
        val isWebsitesHtml = (path == "/websites" || path == "/websites/") && request.isForMainFrame
        if (isWebsitesHtml && shellCacheFile.exists() && shellCacheFile.length() > 0) {
            // Trigger background refresh of the shell
            refreshShellCacheAsync(urlStr)
            return try {
                createResponse("text/html", FileInputStream(shellCacheFile))
            } catch (_: Exception) {
                null
            }
        }

        // 3. Intercept and cache Next.js immutable static assets & Google Fonts
        val isNextStatic = path.startsWith("/_next/static/")
        val isGoogleFont = uri.host?.contains("gstatic.com") == true || uri.host?.contains("googleapis.com") == true

        if (isNextStatic || isGoogleFont) {
            val mimeType = getMimeType(path)
            val cacheKey = hashUrl(urlStr)
            val cachedFile = File(cacheDir, "$cacheKey.${getExtension(path)}")

            if (cachedFile.exists() && cachedFile.length() > 0) {
                return try {
                    createResponse(mimeType, FileInputStream(cachedFile))
                } catch (e: Exception) {
                    null
                }
            }

            // High Performance Non-Blocking Pattern:
            // Let Chromium download immediately through its native HTTP/2 multiplexed socket (0ms delay)
            // Meanwhile, asynchronously fetch in background to warm cache for subsequent launches
            if (downloadingUrls.add(urlStr)) {
                threadPool.execute {
                    try {
                        val conn = URL(urlStr).openConnection() as HttpURLConnection
                        conn.connectTimeout = 4000
                        conn.readTimeout = 6000
                        conn.connect()
                        if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                            val bytes = conn.inputStream.readBytes()
                            conn.disconnect()
                            FileOutputStream(cachedFile).use { it.write(bytes) }
                        }
                    } catch (_: Exception) {
                    } finally {
                        downloadingUrls.remove(urlStr)
                    }
                }
            }
            return null
        }

        return null
    }

    private fun refreshShellCacheAsync(targetUrl: String) {
        if (downloadingUrls.add(targetUrl)) {
            threadPool.execute {
                try {
                    val conn = URL(targetUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "UmamiMobile/1.4 (Android; UltraPerformance)")
                    val encodedToken = java.net.URLEncoder.encode(com.blackpiratex.umami.UmamiApplication.AUTH_TOKEN, "UTF-8")
                    conn.setRequestProperty("Cookie", "umami.auth=$encodedToken")
                    conn.connect()
                    if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                        val bytes = conn.inputStream.readBytes()
                        conn.disconnect()
                        FileOutputStream(shellCacheFile).use { it.write(bytes) }
                    }
                } catch (_: Exception) {
                } finally {
                    downloadingUrls.remove(targetUrl)
                }
            }
        }
    }

    private fun createResponse(mimeType: String, stream: java.io.InputStream): WebResourceResponse {
        val headers = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Cache-Control" to "public, max-age=31536000, immutable"
        )
        return WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, stream)
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".ttf") -> "font/ttf"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".json") -> "application/json"
            else -> "application/octet-stream"
        }
    }

    private fun getExtension(path: String): String {
        val idx = path.lastIndexOf('.')
        return if (idx >= 0 && idx < path.length - 1) path.substring(idx + 1) else "cache"
    }

    private fun hashUrl(url: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(url.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
