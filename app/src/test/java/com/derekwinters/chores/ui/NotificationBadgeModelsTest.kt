package com.derekwinters.chores.ui

import com.derekwinters.chores.data.model.Notification
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue #45: unread-count derivation for the bell badge — unread = `acknowledged_at == null`,
 * empty/unloaded yields 0. The rule lives here (shared with the log screen's `isUnread`) rather
 * than inside the badge ViewModel, mirroring [dueNowCountForUser].
 */
class NotificationBadgeModelsTest {

    private fun notification(id: Int, acknowledgedAt: String? = null) = Notification(
        id = id,
        personId = 7,
        type = "chore_due",
        choreId = id,
        title = "Notification $id",
        body = "Body $id",
        createdAt = "2026-07-14T08:00:00Z",
        deliveredAt = null,
        acknowledgedAt = acknowledgedAt,
        dismissedAt = null
    )

    @Test
    fun emptyList_countsZero() {
        assertEquals(0, unreadNotificationCount(emptyList()))
    }

    @Test
    fun countsOnlyUnacknowledgedItems() {
        val notifications = listOf(
            notification(id = 1),
            notification(id = 2, acknowledgedAt = "2026-07-14T09:00:00Z"),
            notification(id = 3)
        )

        assertEquals(2, unreadNotificationCount(notifications))
    }

    @Test
    fun allAcknowledged_countsZero() {
        val notifications = listOf(
            notification(id = 1, acknowledgedAt = "2026-07-14T09:00:00Z"),
            notification(id = 2, acknowledgedAt = "2026-07-14T09:05:00Z")
        )

        assertEquals(0, unreadNotificationCount(notifications))
    }
}
