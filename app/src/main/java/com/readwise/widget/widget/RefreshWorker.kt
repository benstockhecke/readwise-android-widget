package com.readwise.widget.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.readwise.widget.ReadwiseApp
import java.util.concurrent.TimeUnit

class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as ReadwiseApp
            val repository = app.highlightRepository

            repository.syncHighlights()

            // Update all widget instances after sync
            HighlightWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "readwise_widget_periodic_sync"

        fun enqueuePeriodicSync(context: Context, intervalMinutes: Long) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(
                intervalMinutes, TimeUnit.MINUTES,
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }

        fun enqueueSingleSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
