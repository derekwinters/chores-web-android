import java.time.Duration

plugins {
    id("com.android.application")
    // AGP 9.0+ has Kotlin support built in; the standalone Kotlin Android plugin is no longer
    // applied (and errors if it is). See https://developer.android.com/build/migrate-to-built-in-kotlin.
    id("org.jetbrains.kotlin.plugin.serialization")
    // org.jetbrains.kotlin.kapt is incompatible with built-in Kotlin support and, per the
    // official migration guide, is fully replaced (not supplemented) by com.android.legacy-kapt,
    // pinned to the AGP version (see root build.gradle.kts), for projects like this one that
    // aren't migrating off kapt to KSP yet. Applying both at once fails with "Cannot add
    // extension with name 'kapt', as there is an extension already registered with that name" —
    // both plugins register the same `kapt {}` / `kapt(...)` dependency-configuration surface.
    id("com.android.legacy-kapt")
    // Kotlin 2.0+ decoupled the Jetpack Compose compiler from the core Kotlin plugin; it now
    // needs its own Gradle plugin (replaces composeOptions.kotlinCompilerExtensionVersion below,
    // which was the pre-2.0 pinning mechanism).
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    // Issue #15: Roborazzi snapshot testing (recordRoborazziDebug / verifyRoborazziDebug tasks).
    // Version is pinned in the root build.gradle.kts plugins block. Goldens live in
    // app/src/test/snapshots — the path is passed per-capture in ComponentSnapshotTest
    // (version-stable), not via the plugin's gradle DSL. See docs/snapshot-testing.md.
    id("io.github.takahirom.roborazzi")
}

android {
    namespace = "com.derekwinters.chores"
    // Several of this bump's transitive AndroidX dependencies (androidx.core:core-ktx:1.19.0,
    // androidx.lifecycle:lifecycle-{runtime,viewmodel}-compose:2.11.0) require compiling against
    // API 37; compileSdk only affects which APIs are visible at compile time, not the app's
    // actual minSdk/targetSdk runtime behavior, so this is safe to raise on its own.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.derekwinters.chores"
        minSdk = 33
        targetSdk = 34
        // Version is sourced from gradle.properties (VERSION_NAME / VERSION_CODE) so the
        // Release Please workflow can bump it in one place — see
        // .github/release-please/config.json.
        // VERSION_NAME carries a trailing "# x-release-please-version" marker comment that
        // Release Please's generic updater requires on the same line as the value; strip it
        // here since gradle.properties (java.util.Properties) doesn't treat inline "#" as a
        // comment delimiter, only leading ones.
        versionCode = (project.findProperty("VERSION_CODE") as String).toInt()
        versionName = (project.findProperty("VERSION_NAME") as String).substringBefore("#").trim()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Debug-signed until Play Store launch is planned — see
            // docs/adr/0001-debug-signing-until-play-store-launch.md. This lets CI produce
            // installable release-candidate and tagged-release APKs without a real keystore.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // No kotlinOptions { jvmTarget = ... } block: that DSL is deprecated under AGP 9's built-in
    // Kotlin support (migrate to kotlin.compilerOptions{} instead), and it isn't needed here
    // anyway — with built-in Kotlin, jvmTarget defaults to compileOptions.targetCompatibility
    // (VERSION_17, set above).

    buildFeatures {
        compose = true
        // Issue #35: BuildConfig.VERSION_NAME (release-please managed, see the versionName
        // wiring above) is the real "current app version" for the About screen's client-side
        // GitHub-releases check — not previously used anywhere, so buildConfig generation wasn't
        // turned on until now.
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

// Hilt's generated components reference each other before kapt has generated all of them;
// this tells kapt to treat those forward references as stubs instead of hard errors.
kapt {
    correctErrorTypes = true
}

androidComponents {
    // Compose UI tests rely on androidx.compose.ui:ui-test-manifest, which supplies a test host
    // activity via manifest merging and is only wired up for the debug variant (see
    // debugImplementation dependency below). There is no release-specific behavior in this
    // bootstrap app, so we disable the release unit test variant rather than duplicating the
    // test-manifest dependency into release.
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        // Plain `variantBuilder.enableUnitTest = false` (which is exactly what this project used
        // successfully pre-AGP-9) fails to compile here with "Unresolved reference
        // 'enableUnitTest'" — applying com.android.legacy-kapt (needed for kapt, see the plugins
        // block above) appears to widen the Kotlin-DSL-visible static type of `variantBuilder`
        // away from the ApplicationVariantBuilder/HasUnitTestBuilder subtype that declares this
        // property. A safe cast sidesteps the static-typing gap; the property still exists at
        // runtime on the concrete variant builder AGP hands us.
        (variantBuilder as? com.android.build.api.variant.HasUnitTestBuilder)?.enableUnitTest = false
    }

    // Issue #18: name APK outputs chores-<versionName>-<buildType>.apk (e.g.
    // chores-1.0.0-release.apk) so every CI artifact and GitHub Release asset carries the
    // version without any workflow-side renaming — the workflows all glob *.apk. versionName
    // comes from gradle.properties (bumped by Release Please), keeping one source of truth.
    // Originally used the classic applicationVariants API (the only supported output-filename
    // hook in AGP 8.x); migrated to the new Variant API's onVariants/outputFileName here because
    // AGP 9's built-in Kotlin / new-DSL default removes applicationVariants entirely. This
    // project has no product flavors, so variant.name is just the build type ("debug"/"release").
    onVariants { variant ->
        val appVersionName = (project.findProperty("VERSION_NAME") as String).substringBefore("#").trim()
        variant.outputs.forEach { output ->
            output.outputFileName.set("chores-$appVersionName-${variant.name}.apk")
        }
    }
}

// Bounds the unit test task so a hung test fails CI within minutes instead of running until the
// job's own multi-hour timeout, and logs each test's start so the offending test name shows up
// in the console right before the freeze rather than only in the (never-written) HTML report.
tasks.withType<Test>().configureEach {
    timeout.set(Duration.ofMinutes(10))
    testLogging {
        events("started", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
    }
}

dependencies {
    // Design tokens — pinned exact version, bumped via Dependabot (rollout:
    // derekwinters/chores-web-docs#11, this repo's #13; 0.3.0 adds the component
    // tier consumed by Iteration 4, #24).
    implementation("com.derekwinters.chores:design-tokens:0.3.0")

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // Full icon set for the v1.0.0 nav shell (issue #10: Dashboard/Chores/Log/Users/Settings/
    // Preferences each need a distinct icon beyond material-icons-core's small default subset).
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel + StateFlow collection in Compose (issue #5: first ViewModel pattern).
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")

    // DI (issue #5, docs/adr/0002-network-auth-architecture.md: Hilt introduced now).
    implementation("com.google.dagger:hilt-android:2.60")
    kapt("com.google.dagger:hilt-android-compiler:2.60")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Networking (issue #5, ADR 0002: Retrofit + OkHttp + kotlinx.serialization).
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Encrypted local storage for the auth token + server URL (ADR 0002).
    implementation("androidx.security:security-crypto:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    // Issue #15: Roborazzi snapshot testing on the Robolectric NATIVE-graphics stack (see
    // ComponentSnapshotTest and docs/snapshot-testing.md). Versions match the plugin pin in the
    // root build.gradle.kts.
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.64.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.64.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.64.0")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.4.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
