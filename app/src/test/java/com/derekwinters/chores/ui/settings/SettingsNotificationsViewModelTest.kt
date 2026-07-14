package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.local.FakeNotificationSettingsStore
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.NotificationRepository
import com.derekwinters.chores.notifications.NotificationRescheduler
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #44: unit coverage for [SettingsNotificationsViewModel] — device-local settings load
 * synchronously and never fail; changing the interval re-arms the worker; server preferences load
 * and save via the repository with success/error feedback. Uses the Fake* pattern (no Hilt / no
 * Robolectric), mirroring [SettingsViewModelTest].
 */
class SettingsNotificationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRescheduler : NotificationRescheduler {
        var count = 0
            private set

        override fun reschedule() {
            count++
        }
    }

    private fun successData(vm: SettingsNotificationsViewModel): NotificationSettingsUiState {
        val state = vm.uiState.value
        assertTrue("expected Success, was $state", state is UiState.Success)
        return (state as UiState.Success).data
    }

    private fun viewModel(
        settingsStore: FakeNotificationSettingsStore = FakeNotificationSettingsStore(),
        api: FakeChoresApi = FakeChoresApi(notificationPreferencesResult = mapOf("chore_due" to true)),
        rescheduler: FakeRescheduler = FakeRescheduler()
    ): SettingsNotificationsViewModel =
        SettingsNotificationsViewModel(settingsStore, NotificationRepository(api), rescheduler)

    @Test
    fun load_populatesLocalSettingsFromStore() = runTest(mainDispatcherRule.testDispatcher) {
        val store = FakeNotificationSettingsStore(
            pollIntervalMinutes = 180L,
            offlineAlertEnabled = false,
            offlineAlertThresholdDays = 5
        )
        val vm = viewModel(settingsStore = store)
        advanceUntilIdle()

        val data = successData(vm)
        assertEquals(180L, data.pollIntervalMinutes)
        assertFalse(data.offlineAlertEnabled)
        assertEquals(5, data.offlineAlertThresholdDays)
    }

    @Test
    fun load_populatesServerPreferences_onSuccess() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = viewModel(api = FakeChoresApi(notificationPreferencesResult = mapOf("chore_due" to false)))
        advanceUntilIdle()

        val data = successData(vm)
        assertEquals(mapOf("chore_due" to false), data.preferences)
        assertNull(data.preferencesError)
    }

    @Test
    fun load_preferencesFailure_setsErrorButKeepsLocalSettingsUsable() = runTest(mainDispatcherRule.testDispatcher) {
        val store = FakeNotificationSettingsStore(pollIntervalMinutes = 30L)
        val vm = viewModel(
            settingsStore = store,
            api = FakeChoresApi(notificationPreferencesError = ApiException(-1, "offline"))
        )
        advanceUntilIdle()

        val data = successData(vm)
        // Local settings still present (usable offline), preferences empty + error surfaced.
        assertEquals(30L, data.pollIntervalMinutes)
        assertTrue(data.preferences.isEmpty())
        // safeApiCall maps a thrown exception to the app's friendly network-error message (the
        // user-facing string every repository surfaces), so that is what the VM reports — not the
        // raw exception text.
        assertEquals(HttpErrorMessages.NETWORK_ERROR, data.preferencesError)
    }

    @Test
    fun setPollInterval_persistsAndReschedules() = runTest(mainDispatcherRule.testDispatcher) {
        val store = FakeNotificationSettingsStore()
        val rescheduler = FakeRescheduler()
        val vm = viewModel(settingsStore = store, rescheduler = rescheduler)
        advanceUntilIdle()

        vm.setPollInterval(720L)
        advanceUntilIdle()

        assertEquals(720L, store.pollIntervalMinutes())
        assertEquals(720L, successData(vm).pollIntervalMinutes)
        assertEquals(1, rescheduler.count)
    }

    @Test
    fun setOfflineAlertEnabled_persists() = runTest(mainDispatcherRule.testDispatcher) {
        val store = FakeNotificationSettingsStore(offlineAlertEnabled = true)
        val vm = viewModel(settingsStore = store)
        advanceUntilIdle()

        vm.setOfflineAlertEnabled(false)
        advanceUntilIdle()

        assertFalse(store.offlineAlertEnabled())
        assertFalse(successData(vm).offlineAlertEnabled)
    }

    @Test
    fun setOfflineAlertThresholdDays_persists() = runTest(mainDispatcherRule.testDispatcher) {
        val store = FakeNotificationSettingsStore()
        val vm = viewModel(settingsStore = store)
        advanceUntilIdle()

        vm.setOfflineAlertThresholdDays(10)
        advanceUntilIdle()

        assertEquals(10, store.offlineAlertThresholdDays())
        assertEquals(10, successData(vm).offlineAlertThresholdDays)
    }

    @Test
    fun setPreference_success_updatesMapAndSaveState() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(notificationPreferencesResult = mapOf("chore_due" to true))
        val vm = viewModel(api = api)
        advanceUntilIdle()

        vm.setPreference("chore_due", false)
        advanceUntilIdle()

        assertEquals(mapOf("chore_due" to false), successData(vm).preferences)
        assertEquals(mapOf("chore_due" to false), api.lastUpdatedNotificationPreferences)
        assertEquals(UiState.Success(Unit), vm.saveState.value)
    }

    @Test
    fun setPreference_failure_revertsAndSurfacesError() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            notificationPreferencesResult = mapOf("chore_due" to true),
            updateNotificationPreferencesError = ApiException(500, "save failed")
        )
        val vm = viewModel(api = api)
        advanceUntilIdle()

        vm.setPreference("chore_due", false)
        advanceUntilIdle()

        // Reverted to the pre-toggle server value; error surfaced via saveState. safeApiCall maps
        // the thrown exception to the app's friendly network-error message (consistent with every
        // other repository), so that — not the raw exception text — is what the VM surfaces.
        assertEquals(mapOf("chore_due" to true), successData(vm).preferences)
        val saveState = vm.saveState.value
        assertTrue(saveState is UiState.Error)
        assertEquals(HttpErrorMessages.NETWORK_ERROR, (saveState as UiState.Error).message)
    }
}
