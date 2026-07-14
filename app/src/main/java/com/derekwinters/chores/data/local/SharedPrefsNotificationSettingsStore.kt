package com.derekwinters.chores.data.local

import android.content.Context
import android.content.SharedPreferences
import com.derekwinters.chores.data.local.NotificationSettingsStore.Companion.DEFAULT_OFFLINE_THRESHOLD_DAYS
import com.derekwinters.chores.data.local.NotificationSettingsStore.Companion.DEFAULT_POLL_INTERVAL_MINUTES
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [NotificationSettingsStore] backed by plain (unencrypted) [SharedPreferences] — like
 * [SharedPrefsVersionCheckCache] / [SharedPrefsConnectionStatusStore], none of this data (a poll
 * cadence, an alert threshold) is sensitive, so it doesn't need Keystore-backed encryption.
 *
 * Its own prefs file, kept separate from the [SharedPrefsConnectionStatusStore] file so the
 * device-settings concern and the connection-status/posted-ids concern stay independent.
 */
@Singleton
class SharedPrefsNotificationSettingsStore @Inject constructor(
    @ApplicationContext context: Context
) : NotificationSettingsStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    override fun pollIntervalMinutes(): Long =
        prefs.getLong(KEY_POLL_INTERVAL_MINUTES, DEFAULT_POLL_INTERVAL_MINUTES)

    override fun setPollIntervalMinutes(minutes: Long) {
        prefs.edit().putLong(KEY_POLL_INTERVAL_MINUTES, minutes).apply()
    }

    override fun offlineAlertEnabled(): Boolean =
        prefs.getBoolean(KEY_OFFLINE_ALERT_ENABLED, true)

    override fun setOfflineAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_ALERT_ENABLED, enabled).apply()
    }

    override fun offlineAlertThresholdDays(): Int =
        prefs.getInt(KEY_OFFLINE_THRESHOLD_DAYS, DEFAULT_OFFLINE_THRESHOLD_DAYS)

    override fun setOfflineAlertThresholdDays(days: Int) {
        prefs.edit().putInt(KEY_OFFLINE_THRESHOLD_DAYS, days).apply()
    }

    override fun offlineAlertPosted(): Boolean =
        prefs.getBoolean(KEY_OFFLINE_ALERT_POSTED, false)

    override fun setOfflineAlertPosted(posted: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_ALERT_POSTED, posted).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "chores_notification_settings_prefs"
        const val KEY_POLL_INTERVAL_MINUTES = "poll_interval_minutes"
        const val KEY_OFFLINE_ALERT_ENABLED = "offline_alert_enabled"
        const val KEY_OFFLINE_THRESHOLD_DAYS = "offline_alert_threshold_days"
        const val KEY_OFFLINE_ALERT_POSTED = "offline_alert_posted"
    }
}
