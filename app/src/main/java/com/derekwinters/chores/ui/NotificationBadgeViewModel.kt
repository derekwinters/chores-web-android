package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #45: sources the top-bar bell's unread-notification badge. A deliberate sibling of
 * [NavBadgeViewModel] following the same documented rationale (own 60s polling loop, own fetch,
 * decoupled from [com.derekwinters.chores.ui.notifications.NotificationLogViewModel]) — nav chrome
 * lives outside any single screen's lifecycle, so this is Activity-scoped (see ADR-0004) alongside
 * [CurrentUserViewModel]/[NavBadgeViewModel] rather than to a NavHost destination.
 *
 * Exposes the raw notification list rather than a pre-computed count so the count is derived at the
 * call site via [unreadNotificationCount] (unread = `acknowledged_at == null`), keeping the "what
 * counts as unread" rule in one place shared with the log screen.
 */
@HiltViewModel
class NotificationBadgeViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private var pollingStarted = false

    init {
        refresh()
        startPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            notificationRepository.getNotificationLog()
                .onSuccess { notifications -> _notifications.value = notifications }
        }
    }

    private fun startPolling() {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                refresh()
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 60_000L
    }
}
