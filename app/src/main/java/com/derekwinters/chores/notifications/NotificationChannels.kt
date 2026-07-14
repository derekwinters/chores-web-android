package com.derekwinters.chores.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * The app's system notification channels. Issue #43 introduced the "Chores due" channel that every
 * polled notification is posted on; issue #44 adds the "Connection alerts" channel used by the
 * offline "not connected in X days" alert, kept separate so the user can silence connection
 * warnings without muting chore reminders.
 *
 * minSdk is 33, so [NotificationChannel] (API 26+) is always available — no version guard needed.
 */
object NotificationChannels {

    /** Channel id used by [NotificationPollWorker] when posting chores; user-visible "Chores due". */
    const val CHORES_CHANNEL_ID = "chores"

    /** Issue #44: channel id for the local offline alert; user-visible "Connection alerts". */
    const val CONNECTION_CHANNEL_ID = "connection"

    /**
     * Creates the app's channels if they don't already exist. Idempotent: re-registering an
     * existing channel id is a no-op, so this is safe to call before every post.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val choresChannel = NotificationChannel(
            CHORES_CHANNEL_ID,
            context.getString(com.derekwinters.chores.R.string.notification_channel_chores_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(
                com.derekwinters.chores.R.string.notification_channel_chores_description
            )
        }
        val connectionChannel = NotificationChannel(
            CONNECTION_CHANNEL_ID,
            context.getString(com.derekwinters.chores.R.string.notification_channel_connection_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(
                com.derekwinters.chores.R.string.notification_channel_connection_description
            )
        }
        manager.createNotificationChannel(choresChannel)
        manager.createNotificationChannel(connectionChannel)
    }
}
