package com.blackpiratex.umami.data.preferences

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "umami_session_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USERNAME = "username"
        private const val KEY_SELECTED_WEBSITE_ID = "selected_website_id"
        private const val KEY_THEME_MODE = "theme_mode" // "system", "dark", "light"
    }

    fun saveSession(serverUrl: String, token: String, username: String) {
        val formattedUrl = if (serverUrl.endsWith("/")) serverUrl.dropLast(1) else serverUrl
        prefs.edit()
            .putString(KEY_SERVER_URL, formattedUrl)
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun saveSelectedWebsite(websiteId: String) {
        prefs.edit().putString(KEY_SELECTED_WEBSITE_ID, websiteId).apply()
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getSelectedWebsiteId(): String? = prefs.getString(KEY_SELECTED_WEBSITE_ID, null)

    fun getThemeMode(): String = prefs.getString(KEY_THEME_MODE, "system") ?: "system"

    fun isLoggedIn(): Boolean {
        return !getServerUrl().isNullOrBlank() && !getToken().isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
