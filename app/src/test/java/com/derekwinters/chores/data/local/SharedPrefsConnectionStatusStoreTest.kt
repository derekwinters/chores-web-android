package com.derekwinters.chores.data.local

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #43: round-trip coverage for the real SharedPreferences-backed store, covering both the
 * [ConnectionStatusStore] last-contact timestamp (consumed by #44) and the
 * [PostedNotificationsStore] posted-ids record (the once-per-item guarantee). Runs under
 * Robolectric so it exercises the actual `SharedPreferences` persistence, not a fake.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SharedPrefsConnectionStatusStoreTest {

    private fun newStore(): SharedPrefsConnectionStatusStore =
        SharedPrefsConnectionStatusStore(ApplicationProvider.getApplicationContext())

    @Test
    fun lastSuccessfulContact_isNullBeforeAnyContact() {
        assertNull(newStore().lastSuccessfulContact())
    }

    @Test
    fun recordSuccessfulContact_roundTripsTimestamp() {
        val store = newStore()

        store.recordSuccessfulContact(1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, store.lastSuccessfulContact())
    }

    @Test
    fun recordSuccessfulContact_overwritesPreviousTimestamp() {
        val store = newStore()

        store.recordSuccessfulContact(1L)
        store.recordSuccessfulContact(2L)

        assertEquals(2L, store.lastSuccessfulContact())
    }

    @Test
    fun postedIds_roundTripAndPersistAcrossInstances() {
        newStore().markPosted(1)
        newStore().markPosted(2)

        // A fresh instance over the same prefs file sees both (simulating a process restart).
        val reopened = newStore()
        assertTrue(reopened.isPosted(1))
        assertTrue(reopened.isPosted(2))
        assertFalse(reopened.isPosted(3))
    }

    @Test
    fun contactAndPostedIds_areIndependent() {
        val store = newStore()

        store.markPosted(5)
        store.recordSuccessfulContact(42L)

        assertTrue(store.isPosted(5))
        assertEquals(42L, store.lastSuccessfulContact())
    }
}
