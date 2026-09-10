package com.salesnetwork.avon.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.salesnetwork.avon.app.data.CatalogSyncWorker
import com.salesnetwork.avon.app.update.CrashLogger
import java.util.concurrent.TimeUnit

class SalesNetworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        val request = PeriodicWorkRequestBuilder<CatalogSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "catalog-periodic-sync", ExistingPeriodicWorkPolicy.KEEP, request
        )
        CrashLogger.getInstance(this).log(
            CrashLogger.Level.INFO,
            "AppLifecycle",
            "Application created - ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
    }
}
