package com.derekwinters.chores.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.derekwinters.chores.MainActivity
import com.derekwinters.chores.R
import com.derekwinters.chores.data.local.ConnectionStatusStore
import com.derekwinters.chores.data.local.PostedNotificationsStore
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.data.repository.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Issue #43: periodic worker that polls `GET /v1/notifications` and posts each new item as an
 * Android system notification, exactly once. Scheduled by [NotificationScheduler] as unique
 * periodic work; Hilt-injected via [androidx.hilt.work.HiltWorkerFactory] (see
 * [com.derekwinters.chores.ChoresApplication]).
 *
 * Each run:
 * 1. Fetch un-dismissed notifications. On failure, [androidx.work.ListenableWorker.Result.retry]
 *    — nothing is recorded, so the last-successful-contact timestamp only ever reflects real
 *    successes.
 * 2. Record the last-successful-contact timestamp (consumed by issue #44's offline alert).
 * 3. Post each item that is not acked, not dismissed, and not already locally recorded as posted;
 *    then record its id. Posting is skipped silently when `POST_NOTIFICATIONS` is not granted —
 *    and such items are *not* marked posted, so they still appear once the permission is later
 *    granted. Acknowledgement is a user action (notification tap → `MainActivity`), never sent
 *    merely for posting.
 */
@HiltWorker
class NotificationPollWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: NotificationRepository,
    private val connectionStatusStore: ConnectionStatusStore,
    private val postedStore: PostedNotificationsStore
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
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
    }
}
