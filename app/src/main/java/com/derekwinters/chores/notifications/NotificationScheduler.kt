package com.derekwinters.chores.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Enqueues the unique periodic [NotificationPollWorker] (issue #43). Called post-login from the
 * app shell (`ui/ChoresApp.kt`).
 *
 * Uses [ExistingPeriodicWorkPolicy.KEEP] so re-invoking on every login/recomposition doesn't
 * disturb an already-scheduled poll. The interval is a hardcoded [DEFAULT_INTERVAL_MINUTES] here;
 * issue #44 makes it user-configurable and re-arms the work with `UPDATE`. A
 * [NetworkType.CONNECTED] constraint keeps the worker from waking to a guaranteed-failing poll
 * while offline.
 */
object NotificationScheduler {

    /** Default poll cadence. WorkManager clamps periodic work to a 15-minute floor regardless. */
    const val DEFAULT_INTERVAL_MINUTES = 60L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationPollWorker>(
            DEFAULT_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationPollWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
