package com.derekwinters.chores.ui

import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.NotificationDto
import com.derekwinters.chores.data.repository.NotificationRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Issue #45: an Activity-scoped [NotificationBadgeViewModel] with its own polling loop sources the
 * top-bar bell's unread badge, decoupled from the log screen's own ViewModel (see
 * [NavBadgeViewModelTest] for the same pattern/rationale). Cancels the endless auto-refresh loop
 * before the test body ends so `runTest`'s implicit drain doesn't hang.
 */
class NotificationBadgeViewModelTest {

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
    fun refresh_loadsNotificationsFromRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            notificationsResult = listOf(
                notificationDto(id = 1),
                notificationDto(id = 2, acknowledgedAt = "2026-07-14T09:00:00Z")
            )
        )
        val viewModel = NotificationBadgeViewModel(NotificationRepository(api))
        runCurrent()

        val notifications = viewModel.notifications.value
        assertEquals(2, notifications.size)
        // Count derivation happens at the call site: one unread of the two fetched.
        assertEquals(1, unreadNotificationCount(notifications))

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun initialState_isEmptyBeforeFirstLoadCompletes() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(notificationsResult = listOf(notificationDto(id = 1)))
        val viewModel = NotificationBadgeViewModel(NotificationRepository(api))

        assertEquals(emptyList<Any>(), viewModel.notifications.value)

        runCurrent()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun apiFailure_leavesListEmptyRatherThanCrashing() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(notificationsError = RuntimeException("boom"))
        val viewModel = NotificationBadgeViewModel(NotificationRepository(api))
        runCurrent()

        assertEquals(emptyList<Any>(), viewModel.notifications.value)

        viewModel.viewModelScope.cancel()
    }
}
