package com.derekwinters.chores

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.derekwinters.chores.data.repository.NotificationRepository
import com.derekwinters.chores.ui.ChoresApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * App entry point.
 *
 * Screens stay plain composables reading state from Hilt ViewModels.
 *
 * Issue #43: a tapped system notification launches this activity carrying
 * [EXTRA_ACK_NOTIFICATION_ID]; handling it acknowledges that notification via
 * [NotificationRepository] (the only place ack is ever sent — posting never acks). Idempotent
 * server-side, so a re-delivered intent is harmless.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAckIntent(intent)
        setContent {
            ChoresApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAckIntent(intent)
    }

    private fun handleAckIntent(intent: Intent?) {
        val notificationId = intent?.getIntExtra(EXTRA_ACK_NOTIFICATION_ID, INVALID_ID) ?: INVALID_ID
        if (notificationId != INVALID_ID) {
            lifecycleScope.launch {
                notificationRepository.acknowledge(notificationId)
            }
        }
    }

    companion object {
        /** Intent extra carrying the id of the notification a tap should acknowledge. */
        const val EXTRA_ACK_NOTIFICATION_ID = "ack_notification_id"
        private const val INVALID_ID = -1
    }
}
