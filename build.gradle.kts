// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.3.1" apply false
    // AGP 9.0+ bundles Kotlin support; the standalone org.jetbrains.kotlin.android plugin is no
    // longer applied anywhere in this project (see app/build.gradle.kts) and is intentionally
    // not declared here. https://developer.android.com/build/migrate-to-built-in-kotlin.
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    // Replaces org.jetbrains.kotlin.kapt (incompatible with built-in Kotlin support) for kapt
    // (still used for Hilt's annotation processing), per the official migration guide's
    // supported path for projects not yet moving to KSP. Pinned to the AGP version (kept in
    // lockstep with com.android.application above), not the Kotlin version.
    id("com.android.legacy-kapt") version "9.3.1" apply false
    // Kotlin 2.0+ decoupled the Compose compiler from the core Kotlin plugin; version must match
    // the Kotlin version above (2.4.0). Replaces the pre-2.0 composeOptions.
    // kotlinCompilerExtensionVersion pin that used to live in app/build.gradle.kts.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
    // Issue #15 (design-token rollout iteration 5, derekwinters/chores-web-docs#11): snapshot
    // testing. Roborazzi, not Paparazzi — Paparazzi does not support com.android.application
    // modules (cashapp/paparazzi#107) and all composables live in the single :app application
    // module; Roborazzi runs on the Robolectric stack this repo's compose tests already use.
    // See docs/snapshot-testing.md.
    id("io.github.takahirom.roborazzi") version "1.67.0" apply false
}
