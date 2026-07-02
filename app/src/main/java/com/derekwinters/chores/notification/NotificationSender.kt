package com.derekwinters.chores.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.derekwinters.chores.R

private const val TEST_NOTIFICATION_CHANNEL_ID = "test_notifications"
private const val TEST_NOTIFICATION_ID = 1

/**
 * Posts a local "test notification" triggered from the Notification screen's button tap.
 *
 * Behavior: "fire local notification on button tap" (issue #2, area: android). This class
 * assumes the POST_NOTIFICATIONS runtime permission has already been requested by the caller
 * (see MainActivity) — it checks the permission before posting and no-ops if not granted,
 * matching standard NotificationManagerCompat guidance for API 33+.
 */
class NotificationSender(private val context: Context) {

    fun sendTestNotification() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureNotificationChannel()

        val notification = NotificationCompat.Builder(context, TEST_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        val channel = NotificationChannel(
            TEST_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
