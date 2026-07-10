// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Issue #15 (design-token rollout iteration 5, derekwinters/chores-web-docs#11): snapshot
    // testing. Roborazzi, not Paparazzi — Paparazzi does not support com.android.application
    // modules (cashapp/paparazzi#107) and all composables live in the single :app application
    // module; Roborazzi runs on the Robolectric stack this repo's compose tests already use.
    // See docs/snapshot-testing.md.
    id("io.github.takahirom.roborazzi") version "1.26.0" apply false
}
