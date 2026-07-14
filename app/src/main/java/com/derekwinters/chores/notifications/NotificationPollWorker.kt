package com.derekwinters.chores.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derekwinters.chores.MainActivity
import com.derekwinters.chores.R
import com.derekwinters.chores.data.local.ConnectionStatusStore
import com.derekwinters.chores.data.local.NotificationSettingsStore
import com.derekwinters.chores.data.local.PostedNotificationsStore
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.data.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Issue #43: periodic worker that polls `GET /v1/notifications` and posts each new item as an
 * Android system notification, exactly once. Scheduled by [NotificationScheduler] as unique
 * periodic work.
 *
 * This is a **plain** [CoroutineWorker] (not `@HiltWorker`): it obtains its dependencies from the
 * Hilt graph at runtime via an `@EntryPoint` ([Deps]) rather than through `HiltWorkerFactory`.
 * That deliberately avoids the `androidx.hilt:hilt-compiler` annotation processor, which is too
 * old to read AGP 9's Kotlin 2.x metadata and breaks the kapt build (issue #43 CI). WorkManager's
 * default factory instantiates this worker via its `(Context, WorkerParameters)` constructor, so
 * no custom `Configuration.Provider` wiring is needed.
 *
 * Each run:
 * 1. Fetch un-dismissed notifications. On failure, run the issue #44 offline-alert check (below)
 *    and [Result.retry] — nothing else is recorded, so the last-successful-contact timestamp only
 *    ever reflects real successes.
 * 2. On success, record the last-successful-contact timestamp (consumed by the offline alert) and
 *    clear any standing offline alert.
 * 3. Post each item that is not acked, not dismissed, and not already locally recorded as posted;
 *    then record its id. Posting is skipped silently when `POST_NOTIFICATIONS` is not granted —
 *    and such items are *not* marked posted, so they still appear once the permission is later
 *    granted. Acknowledgement is a user action (notification tap → `MainActivity`), never sent
 *    merely for posting.
 *
 * **Offline alert (issue #44):** on a *failed* poll, if the offline alert is enabled and the last
 * successful contact is older than the configured threshold (and one was ever recorded — no
 * pre-login firing), a single local "not connected" notification is posted on the
 * [NotificationChannels.CONNECTION_CHANNEL_ID] channel with a stable id, at most once per breach
 * (latched via [NotificationSettingsStore.offlineAlertPosted]). It carries no server id and is
 * never acked; the next successful contact cancels it and clears the latch.
 */
class NotificationPollWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    /** The subset of the Hilt graph this worker needs, exposed for runtime access. */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun notificationRepository(): NotificationRepository
        fun connectionStatusStore(): ConnectionStatusStore
        fun postedNotificationsStore(): PostedNotificationsStore
        fun notificationSettingsStore(): NotificationSettingsStore
    }

    override suspend fun doWork(): Result {
        val deps = depsProvider(applicationContext)
        val repository = deps.notificationRepository()
        val connectionStatusStore = deps.connectionStatusStore()
        val postedStore = deps.postedNotificationsStore()
        val settingsStore = deps.notificationSettingsStore()

        val fetchResult = repository.getNotifications()
        if (fetchResult.isFailure) {
            // A failed poll is the offline alert's whole point: the last successful contact may now
            // be stale enough to warn the user. Nothing else is recorded.
            maybePostOfflineAlert(connectionStatusStore, settingsStore)
            return Result.retry()
        }
        val notifications = fetchResult.getOrThrow()

        connectionStatusStore.recordSuccessfulContact(System.currentTimeMillis())
        // A successful contact resets the offline alert: cancel any standing warning and clear the
        // once-per-breach latch so a future breach can warn again.
        clearOfflineAlert(settingsStore)

        val canPost = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            NotificationChannels.ensureChannels(applicationContext)
        }
        val manager = NotificationManagerCompat.from(applicationContext)

        for (notification in notifications) {
            // Acked/dismissed rows are never posted; already-posted ids are never re-posted.
            if (!notification.isActionable) continue
            if (postedStore.isPosted(notification.id)) continue
            // No permission: skip silently WITHOUT recording as posted, so it can still surface
            // once the user grants POST_NOTIFICATIONS.
            if (!canPost) continue

            manager.notify(notification.id, buildNotification(notification))
            postedStore.markPosted(notification.id)
        }

        return Result.success()
    }

    /**
     * Issue #44: post the local offline alert if enabled, past threshold, and a contact was ever
     * recorded — at most once per breach (latched). No-op if the alert is disabled, within
     * threshold, never-contacted (pre-login), already latched, or `POST_NOTIFICATIONS` isn't
     * granted (the latch is not set in that case, so it can still fire once permission is granted).
     */
    private fun maybePostOfflineAlert(
        connectionStatusStore: ConnectionStatusStore,
        settingsStore: NotificationSettingsStore
    ) {
        if (!settingsStore.offlineAlertEnabled()) return
        if (settingsStore.offlineAlertPosted()) return
        val lastContact = connectionStatusStore.lastSuccessfulContact() ?: return

        val thresholdDays = settingsStore.offlineAlertThresholdDays()
        val thresholdMillis = thresholdDays.toLong() * MILLIS_PER_DAY
        val elapsed = System.currentTimeMillis() - lastContact
        if (elapsed < thresholdMillis) return

        val canPost = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!canPost) return

        NotificationChannels.ensureChannels(applicationContext)
        NotificationManagerCompat.from(applicationContext)
            .notify(OFFLINE_ALERT_NOTIFICATION_ID, buildOfflineNotification(thresholdDays))
        settingsStore.setOfflineAlertPosted(true)
    }

    /** Issue #44: cancel a standing offline alert and clear its latch after a successful contact. */
    private fun clearOfflineAlert(settingsStore: NotificationSettingsStore) {
        if (!settingsStore.offlineAlertPosted()) return
        NotificationManagerCompat.from(applicationContext).cancel(OFFLINE_ALERT_NOTIFICATION_ID)
        settingsStore.setOfflineAlertPosted(false)
    }

    private fun buildOfflineNotification(thresholdDays: Int): android.app.Notification {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            OFFLINE_ALERT_NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = applicationContext.getString(
            R.string.notification_offline_body_format,
            thresholdDays
        )
        return NotificationCompat.Builder(applicationContext, NotificationChannels.CONNECTION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.notification_offline_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    private fun buildNotification(notification: Notification): android.app.Notification {
        val tapIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ACK_NOTIFICATION_ID, notification.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.id,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, NotificationChannels.CHORES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    companion object {
        /** Unique periodic-work name (see [NotificationScheduler]). */
        const val WORK_NAME = "notification-poll"

        /**
         * Issue #44: stable system-tray id for the local offline alert, so re-posts *update* the
         * one warning rather than stacking. Negative to stay clear of backend notification ids,
         * which are positive autoincrement primary keys.
         */
        const val OFFLINE_ALERT_NOTIFICATION_ID = -1000

        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

        /** Production resolution: read the real app-scoped Hilt graph. */
        private val productionDepsProvider: (Context) -> Deps = { context ->
            EntryPointAccessors.fromApplication(context.applicationContext, Deps::class.java)
        }

        /**
         * Seam for resolving [Deps]. Defaults to [productionDepsProvider]; tests replace it to
         * inject fakes without standing up a Hilt test component, and call [resetDepsProvider] in
         * teardown.
         */
        @VisibleForTesting
        var depsProvider: (Context) -> Deps = productionDepsProvider

        @VisibleForTesting
        fun resetDepsProvider() {
            depsProvider = productionDepsProvider
        }
    }
}
