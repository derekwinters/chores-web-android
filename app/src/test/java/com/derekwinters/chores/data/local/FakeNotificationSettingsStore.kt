package com.derekwinters.chores.data.local

import com.derekwinters.chores.data.local.NotificationSettingsStore.Companion.DEFAULT_OFFLINE_THRESHOLD_DAYS
import com.derekwinters.chores.data.local.NotificationSettingsStore.Companion.DEFAULT_POLL_INTERVAL_MINUTES

/**
 * In-memory [NotificationSettingsStore] test double (issue #44). Defaults mirror the production
 * SharedPreferences defaults so tests that don't override them see the same values the app ships.
 */
class FakeNotificationSettingsStore(
    private var pollIntervalMinutes: Long = DEFAULT_POLL_INTERVAL_MINUTES,
    private var offlineAlertEnabled: Boolean = true,
    private var offlineAlertThresholdDays: Int = DEFAULT_OFFLINE_THRESHOLD_DAYS,
    private var offlineAlertPosted: Boolean = false
) : NotificationSettingsStore {

    override fun pollIntervalMinutes(): Long = pollIntervalMinutes

    override fun setPollIntervalMinutes(minutes: Long) {
        pollIntervalMinutes = minutes
    }

    override fun offlineAlertEnabled(): Boolean = offlineAlertEnabled

    override fun setOfflineAlertEnabled(enabled: Boolean) {
        offlineAlertEnabled = enabled
    }

    override fun offlineAlertThresholdDays(): Int = offlineAlertThresholdDays

    override fun setOfflineAlertThresholdDays(days: Int) {
        offlineAlertThresholdDays = days
    }

    override fun offlineAlertPosted(): Boolean = offlineAlertPosted

    override fun setOfflineAlertPosted(posted: Boolean) {
        offlineAlertPosted = posted
    }
}
