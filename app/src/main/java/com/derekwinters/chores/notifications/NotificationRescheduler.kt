package com.derekwinters.chores.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #44: re-arms the periodic poll worker to the currently-persisted interval. A thin seam
 * over [NotificationScheduler.reschedule] so [com.derekwinters.chores.ui.settings.SettingsNotificationsViewModel]
 * can trigger the re-arm without holding a [Context] itself — which keeps its unit tests plain
 * JUnit (a fake rescheduler) rather than requiring Robolectric + a WorkManager test harness.
 */
interface NotificationRescheduler {
    fun reschedule()
}

/**
 * Production [NotificationRescheduler]: delegates to [NotificationScheduler.reschedule] with the
 * application context, which reads the new interval from
 * [com.derekwinters.chores.data.local.NotificationSettingsStore] and re-enqueues the unique work
 * with `UPDATE`.
 */
@Singleton
class WorkManagerNotificationRescheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationRescheduler {
    override fun reschedule() = NotificationScheduler.reschedule(context)
}
