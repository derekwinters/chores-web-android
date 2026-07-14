package com.derekwinters.chores.data.local

/**
 * Device-local notification settings (issue #44). These are **device** concerns, not account
 * concerns: a phone on cellular may want to poll less often than a tablet at home, so they are
 * stored per-device in plain [android.content.SharedPreferences] (mirroring [VersionCheckCache] /
 * [ConnectionStatusStore]) rather than round-tripped through the backend. The per-type
 * enable/disable map is the account-level counterpart and lives server-side instead
 * (`GET/PUT /v1/notifications/preferences`, see
 * [com.derekwinters.chores.data.repository.NotificationRepository]).
 *
 * Holds three user-visible settings plus one internal piece of offline-alert state:
 * - **poll interval** — the [com.derekwinters.chores.notifications.NotificationPollWorker]
 *   periodic cadence (default 60 min; WorkManager clamps to a 15-minute floor regardless);
 * - **offline-alert enabled** — whether the "not connected in X days" local alert fires at all
 *   (default on);
 * - **offline-alert threshold days** — how many days without a successful backend contact before
 *   that alert fires (default 3);
 * - **offline-alert posted** — internal latch so the alert is posted at most once per threshold
 *   breach (reset on the next successful contact), NOT a user-facing setting.
 */
interface NotificationSettingsStore {
    /** Poll cadence in minutes (default [DEFAULT_POLL_INTERVAL_MINUTES]). */
    fun pollIntervalMinutes(): Long
    fun setPollIntervalMinutes(minutes: Long)

    /** Whether the offline "not connected in X days" alert is enabled (default true). */
    fun offlineAlertEnabled(): Boolean
    fun setOfflineAlertEnabled(enabled: Boolean)

    /** Days without a successful contact before the offline alert fires (default [DEFAULT_OFFLINE_THRESHOLD_DAYS]). */
    fun offlineAlertThresholdDays(): Int
    fun setOfflineAlertThresholdDays(days: Int)

    /**
     * Internal latch: true once the offline alert has been posted for the current breach, so the
     * worker doesn't re-notify on every subsequent run. Reset to false on the next successful
     * contact (see [com.derekwinters.chores.notifications.NotificationPollWorker]).
     */
    fun offlineAlertPosted(): Boolean
    fun setOfflineAlertPosted(posted: Boolean)

    companion object {
        /** Default poll cadence when the user hasn't chosen one. */
        const val DEFAULT_POLL_INTERVAL_MINUTES = 60L

        /** WorkManager's periodic-work floor; interval choices never go below this. */
        const val MINIMUM_POLL_INTERVAL_MINUTES = 15L

        /** Default offline-alert threshold. */
        const val DEFAULT_OFFLINE_THRESHOLD_DAYS = 3
    }
}
