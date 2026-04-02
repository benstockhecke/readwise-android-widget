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

/**
 * [CoroutineWorker] that syncs highlights from the Readwise API and refreshes
 * all widget instances on the home screen.
 *
 * Scheduled via WorkManager using either a periodic or one-shot work request.
 * On success the worker returns [Result.success]; on any exception it returns
 * [Result.failure] (WorkManager will not retry by default).
 *
 * @param context Application context provided by WorkManager.
 * @param params Worker configuration and input data provided by WorkManager.
 */
class RefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    /**
     * Performs the background sync:
     * 1. Fetches all highlights and books from the Readwise API.
     * 2. Persists them to the local Room database.
     * 3. Triggers a UI update on every active widget instance.
     *
     * @return [Result.success] when the sync completes without error,
     *   or [Result.failure] if an exception is thrown.
     */
    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as ReadwiseApp
            val repository = app.highlightRepository

            repository.syncHighlights()

            // Update all widget instances after sync so they display fresh highlights
            HighlightWidget().updateAll(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        /** Unique work name used to identify the periodic sync task in WorkManager. */
        private const val WORK_NAME = "readwise_widget_periodic_sync"

        /**
         * Schedules a recurring sync that runs at the given [intervalMinutes].
         *
         * Uses [ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE] so that changing
         * the interval always takes effect immediately by replacing the previous request.
         *
         * @param context Application context used to access WorkManager.
         * @param intervalMinutes How often the sync should repeat, in minutes.
         */
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

        /**
         * Enqueues a single immediate sync, typically triggered by the user tapping
         * "Sync now" in the settings screen.
         *
         * @param context Application context used to access WorkManager.
         */
        fun enqueueSingleSync(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
