package com.derekwinters.chores.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.NotificationRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #45: drives the in-app Notification Log screen. Loads the caller's notifications (unread +
 * acknowledged, dismissed excluded) via [NotificationRepository.getNotificationLog] using the
 * [UiState] conventions, and acknowledges a row from the screen via
 * [NotificationRepository.acknowledge] — the same server acknowledgement the system-notification
 * tap performs.
 *
 * Ack is optimistic (mirroring [com.derekwinters.chores.ui.settings.SettingsNotificationsViewModel]'s
 * per-type toggle): the row flips to read immediately and reverts if the PUT fails, so a failed ack
 * never leaves the list in an error state — the unread bell badge, sourced by the decoupled
 * [com.derekwinters.chores.ui.NotificationBadgeViewModel]'s own poll, reconciles on its next tick.
 */
@HiltViewModel
class NotificationLogViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Notification>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Notification>>> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            notificationRepository.getNotificationLog()
                .onSuccess { notifications -> _uiState.value = UiState.Success(notifications) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Refresh support — re-fetches without forcing the screen back through a spinner mid-list. */
    fun refresh() = load()

    /**
     * Acknowledge one unread row from the screen. Optimistically stamps [Notification.acknowledgedAt]
     * so the row re-renders as read, PUTs the ack, and reverts that single row on failure. A no-op
     * for an already-read row or once the list isn't in a success state.
     */
    fun acknowledge(notificationId: Int) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val target = current.firstOrNull { it.id == notificationId } ?: return
        if (!target.isUnread) return

        val optimistic = current.map { notification ->
            if (notification.id == notificationId) {
                notification.copy(acknowledgedAt = Instant.now().toString())
            } else {
                notification
            }
        }
        _uiState.value = UiState.Success(optimistic)

        viewModelScope.launch {
            notificationRepository.acknowledge(notificationId)
                .onFailure {
                    // Revert just this row; leave any newer acks the user made in the meantime.
                    val reverted = (uiState.value as? UiState.Success)?.data?.map { notification ->
                        if (notification.id == notificationId) notification.copy(acknowledgedAt = target.acknowledgedAt) else notification
                    }
                    if (reverted != null) _uiState.value = UiState.Success(reverted)
                }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
