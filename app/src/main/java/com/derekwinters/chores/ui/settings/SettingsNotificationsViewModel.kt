package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.local.NotificationSettingsStore
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.NotificationPreferencesDto
import com.derekwinters.chores.data.repository.NotificationRepository
import com.derekwinters.chores.notifications.NotificationRescheduler
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Combined UI state for the Notifications settings screen (issue #44): the device-local settings
 * (poll interval, offline alert) plus the account-level per-type preference map fetched from the
 * server.
 *
 * @param preferences the server per-type enablement map (empty until loaded / on load failure).
 * @param preferencesError non-null when the server preferences failed to load — surfaced as a
 *   non-blocking banner so the device-local controls above stay usable even offline (which is
 *   exactly when a user may want to lengthen the interval or check the offline-alert threshold).
 */
data class NotificationSettingsUiState(
    val pollIntervalMinutes: Long,
    val offlineAlertEnabled: Boolean,
    val offlineAlertThresholdDays: Int,
    val preferences: NotificationPreferencesDto = emptyMap(),
    val preferencesError: String? = null
)

/**
 * Issue #44: drives the Notifications settings screen. Device-local settings
 * ([NotificationSettingsStore]) load synchronously and never fail; changing the interval re-arms
 * the periodic worker via [NotificationRescheduler]. The account-level per-type preferences load
 * from and save to the backend ([NotificationRepository.getPreferences] /
 * [NotificationRepository.updatePreferences]).
 *
 * Reachable by every user, not just admins (preferences are per-user) — see `ChoresApp.kt`.
 */
@HiltViewModel
class SettingsNotificationsViewModel @Inject constructor(
    private val settingsStore: NotificationSettingsStore,
    private val notificationRepository: NotificationRepository,
    private val rescheduler: NotificationRescheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<NotificationSettingsUiState>>(UiState.Loading)
    val uiState: StateFlow<UiState<NotificationSettingsUiState>> = _uiState.asStateFlow()

    /** Feedback for the per-type preference PUT (mirrors other settings screens' saveState). */
    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    init {
        load()
    }

    fun load() {
        // Local settings can't fail — publish them immediately so the screen is usable offline.
        _uiState.value = UiState.Success(
            NotificationSettingsUiState(
                pollIntervalMinutes = settingsStore.pollIntervalMinutes(),
                offlineAlertEnabled = settingsStore.offlineAlertEnabled(),
                offlineAlertThresholdDays = settingsStore.offlineAlertThresholdDays()
            )
        )
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            notificationRepository.getPreferences()
                .onSuccess { prefs ->
                    updateState { it.copy(preferences = prefs, preferencesError = null) }
                }
                .onFailure { error ->
                    updateState { it.copy(preferencesError = errorMessage(error)) }
                }
        }
    }

    /** Persist the new poll interval (device-local) and re-arm the periodic worker with it. */
    fun setPollInterval(minutes: Long) {
        settingsStore.setPollIntervalMinutes(minutes)
        updateState { it.copy(pollIntervalMinutes = minutes) }
        rescheduler.reschedule()
    }

    fun setOfflineAlertEnabled(enabled: Boolean) {
        settingsStore.setOfflineAlertEnabled(enabled)
        updateState { it.copy(offlineAlertEnabled = enabled) }
    }

    fun setOfflineAlertThresholdDays(days: Int) {
        settingsStore.setOfflineAlertThresholdDays(days)
        updateState { it.copy(offlineAlertThresholdDays = days) }
    }

    /**
     * Toggle one server-side per-type preference. Optimistically updates the UI, PUTs the whole
     * map, and adopts the server's echoed result on success; on failure reverts the toggle and
     * surfaces an error banner via [saveState].
     */
    fun setPreference(type: String, enabled: Boolean) {
        val current = (uiState.value as? UiState.Success)?.data ?: return
        val previous = current.preferences
        val optimistic = previous.toMutableMap().apply { put(type, enabled) }

        updateState { it.copy(preferences = optimistic) }
        _saveState.value = UiState.Loading
        viewModelScope.launch {
            notificationRepository.updatePreferences(optimistic)
                .onSuccess { updated ->
                    updateState { it.copy(preferences = updated) }
                    _saveState.value = UiState.Success(Unit)
                }
                .onFailure { error ->
                    updateState { it.copy(preferences = previous) }
                    _saveState.value = UiState.Error(errorMessage(error))
                }
        }
    }

    private inline fun updateState(transform: (NotificationSettingsUiState) -> NotificationSettingsUiState) {
        val state = _uiState.value
        if (state is UiState.Success) {
            _uiState.value = UiState.Success(transform(state.data))
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
