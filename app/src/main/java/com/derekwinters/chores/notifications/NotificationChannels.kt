package com.derekwinters.chores.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * The app's system notification channels (issue #43). v1 has exactly one: the "Chores due"
 * channel that every polled notification is posted on.
 *
 * minSdk is 33, so [NotificationChannel] (API 26+) is always available — no version guard needed.
 */
object NotificationChannels {

    /** Channel id used by [NotificationPollWorker] when posting; user-visible name "Chores due". */
    const val CHORES_CHANNEL_ID = "chores"

    /**
     * Creates the "Chores due" channel if it doesn't already exist. Idempotent: re-registering an
     * existing channel id is a no-op server-side, so this is safe to call before every post.
     */
    fun ensureChannels(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHORES_CHANNEL_ID,
            context.getString(com.derekwinters.chores.R.string.notification_channel_chores_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(
                com.derekwinters.chores.R.string.notification_channel_chores_description
            )
        }
        manager.createNotificationChannel(channel)
    }
}
