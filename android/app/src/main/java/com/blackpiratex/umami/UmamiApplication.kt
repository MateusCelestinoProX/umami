package com.blackpiratex.umami

import android.app.Application
import com.blackpiratex.umami.data.api.UmamiApiService
import com.blackpiratex.umami.data.db.UmamiDatabase
import com.blackpiratex.umami.data.preferences.SessionManager
import com.blackpiratex.umami.util.NetworkObserver

class UmamiApplication : Application() {

    lateinit var database: UmamiDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var apiService: UmamiApiService
        private set

    lateinit var networkObserver: NetworkObserver
        private set

    override fun onCreate() {
        super.onCreate()
        database = UmamiDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        apiService = UmamiApiService()
        networkObserver = NetworkObserver(this)
    }
}
