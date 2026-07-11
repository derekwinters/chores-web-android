// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.2.1" apply false
    // AGP 9.0+'s built-in Kotlin support is incompatible with org.jetbrains.kotlin.kapt (used
    // below for Hilt), so built-in Kotlin is disabled (see gradle.properties:
    // android.builtInKotlin=false / android.newDsl=false) and the traditional standalone Kotlin
    // Android plugin stays applied — see app/build.gradle.kts.
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0" apply false
    id("org.jetbrains.kotlin.kapt") version "2.4.0" apply false
    // Kotlin 2.0+ decoupled the Compose compiler from the core Kotlin plugin; version must match
    // the Kotlin version above (2.4.0). Replaces the pre-2.0 composeOptions.
    // kotlinCompilerExtensionVersion pin that used to live in app/build.gradle.kts.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0" apply false
    id("com.google.dagger.hilt.android") version "2.60" apply false
    // Issue #15 (design-token rollout iteration 5, derekwinters/chores-web-docs#11): snapshot
    // testing. Roborazzi, not Paparazzi — Paparazzi does not support com.android.application
    // modules (cashapp/paparazzi#107) and all composables live in the single :app application
    // module; Roborazzi runs on the Robolectric stack this repo's compose tests already use.
    // See docs/snapshot-testing.md.
    id("io.github.takahirom.roborazzi") version "1.64.0" apply false
}
