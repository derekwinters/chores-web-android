package com.derekwinters.chores.data.local

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #44: round-trip coverage for the real SharedPreferences-backed notification settings store,
 * including defaults and persistence across instances (simulating a process restart). Runs under
 * Robolectric so it exercises actual `SharedPreferences` persistence, not a fake.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedPrefsNotificationSettingsStoreTest {

    private fun newStore(): SharedPrefsNotificationSettingsStore =
        SharedPrefsNotificationSettingsStore(ApplicationProvider.getApplicationContext())

    @Test
    fun defaults_matchShippedValues() {
        val store = newStore()
        assertEquals(60L, store.pollIntervalMinutes())
        assertTrue(store.offlineAlertEnabled())
        assertEquals(3, store.offlineAlertThresholdDays())
        assertFalse(store.offlineAlertPosted())
    }

    @Test
    fun pollInterval_roundTripsAndPersistsAcrossInstances() {
        newStore().setPollIntervalMinutes(180L)

        assertEquals(180L, newStore().pollIntervalMinutes())
    }

    @Test
    fun offlineAlertEnabled_roundTrips() {
        val store = newStore()

        store.setOfflineAlertEnabled(false)

        assertFalse(newStore().offlineAlertEnabled())
    }

    @Test
    fun offlineAlertThresholdDays_roundTrips() {
        val store = newStore()

        store.setOfflineAlertThresholdDays(7)

        assertEquals(7, newStore().offlineAlertThresholdDays())
    }

    @Test
    fun offlineAlertPosted_latchRoundTrips() {
        val store = newStore()

        store.setOfflineAlertPosted(true)
        assertTrue(newStore().offlineAlertPosted())

        store.setOfflineAlertPosted(false)
        assertFalse(newStore().offlineAlertPosted())
    }

    @Test
    fun settings_areIndependent() {
        val store = newStore()

        store.setPollIntervalMinutes(30L)
        store.setOfflineAlertThresholdDays(5)
        store.setOfflineAlertEnabled(false)

        val reopened = newStore()
        assertEquals(30L, reopened.pollIntervalMinutes())
        assertEquals(5, reopened.offlineAlertThresholdDays())
        assertFalse(reopened.offlineAlertEnabled())
    }
}
