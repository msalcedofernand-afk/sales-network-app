package com.salesnetwork.avon.app

import android.app.Application
import com.salesnetwork.avon.app.update.CrashLogger

class SalesNetworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        CrashLogger.getInstance(this).log(
            CrashLogger.Level.INFO,
            "AppLifecycle",
            "Application created - ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    }
}
