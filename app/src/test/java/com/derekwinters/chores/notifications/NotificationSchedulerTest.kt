package com.derekwinters.chores.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.derekwinters.chores.data.local.FakeNotificationSettingsStore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #44: verifies the unique periodic-work scheduling/re-arm behavior via WorkManager's test
 * harness ([WorkManagerTestInitHelper]). The interval is sourced from
 * [com.derekwinters.chores.data.local.NotificationSettingsStore] through the
 * [NotificationScheduler.settingsProvider] seam (reset in teardown).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        NotificationScheduler.resetSettingsProvider()
    }

    private fun workInfos(): List<WorkInfo> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(NotificationPollWorker.WORK_NAME)
            .get()

    @Test
    fun schedule_enqueuesSingleUniquePeriodicWork() {
        NotificationScheduler.settingsProvider = { FakeNotificationSettingsStore(pollIntervalMinutes = 60L) }

        NotificationScheduler.schedule(context)

        val infos = workInfos()
        assertEquals(1, infos.size)
        assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
    }

    @Test
    fun reschedule_reArmsSameUniqueWork_withoutDuplicating() {
        val settings = FakeNotificationSettingsStore(pollIntervalMinutes = 60L)
        NotificationScheduler.settingsProvider = { settings }

        NotificationScheduler.schedule(context)
        val originalId = workInfos().single().id

        // User picks a new interval; UPDATE re-arms the same unique work in place.
        settings.setPollIntervalMinutes(180L)
        NotificationScheduler.reschedule(context)

        val infos = workInfos()
        assertEquals("re-arm must not create a duplicate work request", 1, infos.size)
        // UPDATE keeps the work identity (a fresh REPLACE-style enqueue would change the id).
        assertEquals(originalId, infos.single().id)
    }
}
