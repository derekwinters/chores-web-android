package com.derekwinters.chores

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point for Hilt's generated dependency graph.
 *
 * Issue #5 / ADR 0002: Hilt is introduced at the same time as the first ViewModels and
 * repositories, so this class exists to anchor `@HiltAndroidApp` component generation.
 *
 * Issue #43 / ADR 0007: also supplies the WorkManager [Configuration] with the Hilt-aware
 * [HiltWorkerFactory] so `@HiltWorker` workers (e.g.
 * [com.derekwinters.chores.notifications.NotificationPollWorker]) can be constructor-injected.
 * The default `WorkManagerInitializer` is removed in the manifest so WorkManager uses this
 * on-demand configuration instead of its non-Hilt default.
 */
@HiltAndroidApp
class ChoresApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
