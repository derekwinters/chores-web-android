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
 * 1. Fetch un-dismissed notifications. On failure, [Result.retry] — nothing is recorded, so the
 *    last-successful-contact timestamp only ever reflects real successes.
 * 2. Record the last-successful-contact timestamp (consumed by issue #44's offline alert).
 * 3. Post each item that is not acked, not dismissed, and not already locally recorded as posted;
 *    then record its id. Posting is skipped silently when `POST_NOTIFICATIONS` is not granted —
 *    and such items are *not* marked posted, so they still appear once the permission is later
 *    granted. Acknowledgement is a user action (notification tap → `MainActivity`), never sent
 *    merely for posting.
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
    }

    override suspend fun doWork(): Result {
        val deps = depsProvider(applicationContext)
        val repository = deps.notificationRepository()
        val connectionStatusStore = deps.connectionStatusStore()
        val postedStore = deps.postedNotificationsStore()

        val notifications = repository.getNotifications().getOrElse { return Result.retry() }

        connectionStatusStore.recordSuccessfulContact(System.currentTimeMillis())

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
