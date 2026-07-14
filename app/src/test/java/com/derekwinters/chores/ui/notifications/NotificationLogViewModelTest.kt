package com.derekwinters.chores.ui.notifications

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.NotificationDto
import com.derekwinters.chores.data.repository.NotificationRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #45: the Notification Log loads unread + acknowledged rows via
 * [NotificationRepository.getNotificationLog] and acknowledges a row optimistically. Uses the
 * in-memory [FakeChoresApi] (no real thread hop) so the `viewModelScope.launch` work is
 * deterministic under `advanceUntilIdle()`, the same style as ActivityLogViewModelTest.
 */
class NotificationLogViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun notificationDto(id: Int, acknowledgedAt: String? = null) = NotificationDto(
        id = id,
        person_id = 7,
        type = "chore_due",
        chore_id = id,
        title = "Notification $id",
        body = "Body $id",
        created_at = "2026-07-14T08:00:00Z",
        acknowledged_at = acknowledgedAt
    )

    @Test
    fun load_success_populatesNotificationsIncludingAcknowledged() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            notificationsResult = listOf(
                notificationDto(id = 1),
                notificationDto(id = 2, acknowledgedAt = "2026-07-14T09:00:00Z")
            )
        )
        val viewModel = NotificationLogViewModel(NotificationRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val notifications = (state as UiState.Success).data
        assertEquals(2, notifications.size)
        // Acknowledged item is retained (read history), not filtered out.
        assertTrue(notifications[0].isUnread)
        assertFalse(notifications[1].isUnread)
        // The log fetch excludes dismissed items server-side.
        assertEquals(false, api.lastGetNotificationsIncludeDismissed)
    }

    @Test
    fun load_failure_mapsToNetworkErrorMessage() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(notificationsError = RuntimeException("boom"))
        val viewModel = NotificationLogViewModel(NotificationRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(
            "Unable to reach the server. Check your connection and server URL.",
            (state as UiState.Error).message
        )
    }

    @Test
    fun acknowledge_marksRowReadAndCallsServer() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(notificationsResult = listOf(notificationDto(id = 1)))
        val viewModel = NotificationLogViewModel(NotificationRepository(api))
        advanceUntilIdle()

        viewModel.acknowledge(1)
        advanceUntilIdle()

        val notifications = (viewModel.uiState.value as UiState.Success).data
        assertFalse(notifications.single().isUnread)
        assertEquals(listOf(1), api.ackedNotificationIds.toList())
    }

    @Test
    fun acknowledge_alreadyReadRow_isNoOp() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            notificationsResult = listOf(notificationDto(id = 1, acknowledgedAt = "2026-07-14T09:00:00Z"))
        )
        val viewModel = NotificationLogViewModel(NotificationRepository(api))
        advanceUntilIdle()

        viewModel.acknowledge(1)
        advanceUntilIdle()

        assertTrue(api.ackedNotificationIds.isEmpty())
    }

    @Test
    fun acknowledge_serverFailure_revertsRowToUnread() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            notificationsResult = listOf(notificationDto(id = 1)),
            notificationAckError = RuntimeException("boom")
        )
        val viewModel = NotificationLogViewModel(NotificationRepository(api))
        advanceUntilIdle()

        viewModel.acknowledge(1)
        advanceUntilIdle()

        // The optimistic read flip is rolled back so the row shows unread again.
        val notifications = (viewModel.uiState.value as UiState.Success).data
        assertTrue(notifications.single().isUnread)
    }
}
