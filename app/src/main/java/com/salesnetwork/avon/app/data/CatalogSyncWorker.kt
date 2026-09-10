package com.salesnetwork.avon.app.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CatalogSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val result = ProductCatalogRepository.getInstance(applicationContext).refreshFromSupabase()
        return if (result.isSuccess) Result.success() else if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
