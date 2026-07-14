package com.derekwinters.chores

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt's generated dependency graph.
 *
 * Issue #5 / ADR 0002: Hilt is introduced at the same time as the first ViewModels and
 * repositories, so this class exists purely to anchor `@HiltAndroidApp` component generation.
 *
 * Issue #43 / ADR 0007: the notification poll worker is a plain `CoroutineWorker` that pulls its
 * dependencies from the Hilt graph via an `@EntryPoint` (see
 * [com.derekwinters.chores.notifications.NotificationPollWorker]), so no `HiltWorkerFactory` /
 * `Configuration.Provider` wiring is needed here and WorkManager's default initializer is used.
 */
@HiltAndroidApp
class ChoresApplication : Application()
