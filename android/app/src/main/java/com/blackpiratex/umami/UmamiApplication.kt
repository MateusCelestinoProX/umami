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

        if (!sessionManager.isLoggedIn()) {
            sessionManager.saveSession(
                serverUrl = "https://umami-chi-topaz.vercel.app",
                token = "AZ3LnzBh1mE9H1wO86LIGs8VJlSaY7rEPJPng9ikgODVBTh61e8gjMWigMtfFrKm3AGvbbi7bk1U7LT47G9OFrLS5EeCk6vJ6MtJQti2S/Kc87Q1RBJBKCigTVAl3ps6nYQy+b78cvWeFF3PT8W1SmzDobmRg0inMuonkcboe9k6TkakKdpAzE7P51fHBpQc9m3/ZL8Fz52hq/BoW8Xlm5OSNSQ4d5JsOZilbf3MvPXIvbOM3Nh21NBDlYc+YbFNUn1MLVOedthXjUMNj9AmpQKxeF0kFO4cg/OvYcENg6y/Q4M5iSa+fQRntgZwo2l2LiPIjPEi0NwPIz7QgXwBVUMFPJc6GdrSMogm1t20yHzP0rM+gU+BmTBHNLL9y+9oD+MDMCm3jEaLqnst5FMp97EuWpqUorgOeDE14rtkc8WZIlMpsWb23roijiuRHdQGrfIt9l9gSpF3P/E8jl7wiNcZRFAlS2b9goZDouWb1oGJSGJldEPTBnuFaXUwZgK8mLF88KKb+xt59gTqnjwtnZ19gvqdGo7gnw7ug1an1biH94t91oVSSQ6vfN8uzwnU3GmMgUzEBxf04a5U02dzqpQIojodTDX4kfXljg5KmACtAUwO",
                username = "mateuscelestino"
            )
            sessionManager.saveSelectedWebsite("f4cb9526-35ff-4975-9e7b-1691b79800c2")
        }
    }
}
