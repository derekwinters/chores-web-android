package com.derekwinters.chores

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point for Hilt's generated dependency graph.
 *
 * Issue #5 / ADR 0002: Hilt is introduced at the same time as the first ViewModels and
 * repositories, so this class exists purely to anchor `@HiltAndroidApp` component generation.
 */
@HiltAndroidApp
class ChoresApplication : Application()
