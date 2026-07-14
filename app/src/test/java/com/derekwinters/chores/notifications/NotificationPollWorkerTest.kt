package com.derekwinters.chores.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.derekwinters.chores.data.local.FakeConnectionStatusStore
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.NotificationDto
import com.derekwinters.chores.data.repository.NotificationRepository
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Issue #43: unit coverage for [NotificationPollWorker] via `androidx.work:work-testing`'s
 * [TestListenableWorkerBuilder], under Robolectric so the real `NotificationManagerCompat`
 * behaves like the device.
 *
 * The worker is a plain [androidx.work.CoroutineWorker] that resolves its Hilt dependencies via
 * an `@EntryPoint`; these tests inject fakes through [NotificationPollWorker.depsProvider] rather
 * than standing up a Hilt test component (reset in [tearDown]).
 *
 * Covers: once-per-item across re-runs, skip-acked/skip-dismissed, last-successful-contact
 * recording, and the retry-without-recording failure path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationPollWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // minSdk 33 requires the runtime grant for posting to actually reach the tray.
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        NotificationPollWorker.resetDepsProvider()
    }

    private fun notification(
        id: Int,
        acknowledgedAt: String? = null,
        dismissedAt: String? = null
    ) = NotificationDto(
        id = id,
        person_id = 7,
        type = "chore_due",
        chore_id = 42,
        title = "Chore $id due",
        body = "Body $id",
        created_at = "2026-07-14T08:00:00Z",
        acknowledged_at = acknowledgedAt,
        dismissed_at = dismissedAt
    )

    private fun buildWorker(
        api: FakeChoresApi,
        store: FakeConnectionStatusStore
    ): NotificationPollWorker {
        val repository = NotificationRepository(api)
        NotificationPollWorker.depsProvider = {
            object : NotificationPollWorker.Deps {
                override fun notificationRepository() = repository
                override fun connectionStatusStore() = store
                override fun postedNotificationsStore() = store
            }
        }
        return TestListenableWorkerBuilder<NotificationPollWorker>(context).build()
    }

    private fun activeCount(): Int {
        val manager = context.getSystemService(NotificationManager::class.java)
        return shadowOf(manager).size()
    }

    @Test
    fun postsEachActionableItemAndRecordsThem() = runBlocking {
        val api = FakeChoresApi(
            notificationsResult = listOf(notification(1), notification(2))
        )
        val store = FakeConnectionStatusStore()

        val result = buildWorker(api, store).doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertEquals(2, activeCount())
        assertEquals(setOf(1, 2), store.postedIds())
    }

    @Test
    fun skipsAckedAndDismissedItems() = runBlocking {
        val api = FakeChoresApi(
            notificationsResult = listOf(
                notification(1),
                notification(2, acknowledgedAt = "2026-07-14T09:00:00Z"),
                notification(3, dismissedAt = "2026-07-14T09:00:00Z")
            )
        )
        val store = FakeConnectionStatusStore()

        buildWorker(api, store).doWork()

        // Only the actionable item is posted / recorded; acked + dismissed are never posted.
        assertEquals(1, activeCount())
        assertEquals(setOf(1), store.postedIds())
        assertFalse(store.isPosted(2))
        assertFalse(store.isPosted(3))
    }

    @Test
    fun doesNotRepostAlreadyPostedItemsAcrossRuns() = runBlocking {
        val api = FakeChoresApi(
            notificationsResult = listOf(notification(1), notification(2))
        )
        val store = FakeConnectionStatusStore()

        buildWorker(api, store).doWork()
        assertEquals(2, activeCount())

        // Clear the tray, then re-run with the same (persisted) posted-ids store.
        NotificationManagerCompat.from(context).cancelAll()
        assertEquals(0, activeCount())

        val secondResult = buildWorker(api, store).doWork()

        assertEquals(ListenableWorker.Result.success(), secondResult)
        // Nothing re-posted: the once-per-item guarantee holds across worker re-runs.
        assertEquals(0, activeCount())
        assertEquals(setOf(1, 2), store.postedIds())
    }

    @Test
    fun recordsLastSuccessfulContactOnSuccessfulPoll() = runBlocking {
        val api = FakeChoresApi(notificationsResult = listOf(notification(1)))
        val store = FakeConnectionStatusStore()

        val before = System.currentTimeMillis()
        buildWorker(api, store).doWork()
        val after = System.currentTimeMillis()

        val recorded = store.lastSuccessfulContact()
        assertTrue("expected a recorded contact timestamp", recorded != null)
        assertTrue(recorded!! in before..after)
        assertEquals(1, store.recordContactCallCount)
    }

    @Test
    fun failedPollRetriesWithoutRecordingContact() = runBlocking {
        val api = FakeChoresApi(notificationsError = IOException("offline"))
        val store = FakeConnectionStatusStore()

        val result = buildWorker(api, store).doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
        // A failed poll is not a successful contact, and nothing is posted.
        assertEquals(0, store.recordContactCallCount)
        assertTrue(store.postedIds().isEmpty())
        assertEquals(0, activeCount())
    }
}
