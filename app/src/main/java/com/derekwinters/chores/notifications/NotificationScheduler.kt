package com.derekwinters.chores.notifications

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.derekwinters.chores.data.local.NotificationSettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Enqueues the unique periodic [NotificationPollWorker]. The cadence is sourced from
 * [NotificationSettingsStore] (issue #44 made it user-configurable; issue #43 originally hardcoded
 * 60 minutes here), resolved from the Hilt graph via an `@EntryPoint` — the same pattern the
 * worker itself uses — so this stays a plain `object` callable from the composable app shell
 * without threading the interval through composition.
 *
 * Two entry points:
 * - [schedule] (post-login, from `ui/ChoresApp.kt`) uses [ExistingPeriodicWorkPolicy.KEEP] so
 *   re-invoking on every login/recomposition doesn't disturb an already-scheduled poll.
 * - [reschedule] (after the user changes the interval, from `SettingsNotificationsViewModel`) uses
 *   [ExistingPeriodicWorkPolicy.UPDATE] to apply the new interval to the *same* unique work
 *   without creating a duplicate request.
 *
 * A [NetworkType.CONNECTED] constraint keeps the worker from waking to a guaranteed-failing poll
 * while offline.
 */
object NotificationScheduler {

    /** Unique periodic-work name — re-exported from the worker for callers/tests. */
    const val WORK_NAME = NotificationPollWorker.WORK_NAME

    /** The subset of the Hilt graph the scheduler needs, exposed for runtime access. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun notificationSettingsStore(): NotificationSettingsStore
    }

    /** Production resolution: read the real app-scoped Hilt graph. */
    private val productionSettingsProvider: (Context) -> NotificationSettingsStore = { context ->
        EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
            .notificationSettingsStore()
    }

    /**
     * Seam for resolving the settings store. Defaults to [productionSettingsProvider]; tests
     * replace it to supply a fake without a Hilt test component, and call [resetSettingsProvider]
     * in teardown.
     */
    @VisibleForTesting
    var settingsProvider: (Context) -> NotificationSettingsStore = productionSettingsProvider

    @VisibleForTesting
    fun resetSettingsProvider() {
        settingsProvider = productionSettingsProvider
    }

    /** Post-login enqueue: schedule the poll if not already scheduled (interval from settings). */
    fun schedule(context: Context) = enqueue(context, ExistingPeriodicWorkPolicy.KEEP)

    /**
     * Re-arm the unique poll work to reflect the currently-persisted interval, replacing the
     * existing schedule in place (no duplicate work request). Called after the user changes the
     * poll interval.
     */
    fun reschedule(context: Context) = enqueue(context, ExistingPeriodicWorkPolicy.UPDATE)

    private fun enqueue(context: Context, policy: ExistingPeriodicWorkPolicy) {
        val intervalMinutes = settingsProvider(context).pollIntervalMinutes()
            .coerceAtLeast(NotificationSettingsStore.MINIMUM_POLL_INTERVAL_MINUTES)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationPollWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotificationPollWorker.WORK_NAME,
            policy,
            request
        )
    }
}
