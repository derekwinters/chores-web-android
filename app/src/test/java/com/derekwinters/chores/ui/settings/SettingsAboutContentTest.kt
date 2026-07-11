package com.derekwinters.chores.ui.settings

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 4: Each section screen has its own local draft and its own Save action
 * (area: android). Tests [SettingsAboutContent] directly.
 *
 * Issue #35: [SettingsAboutContent] now takes [AppVersionUiState] (client-side GitHub-releases
 * check) and [BackendVersionUiState] (backend's own `GET /version`) instead of the single
 * backend-sourced `UpdateCheckStatus`, plus renders the three outbound repo links.
 *
 * Uses [createAndroidComposeRule] (rather than the plain `createComposeRule()`) so the link tests
 * can inspect the [Intent] fired by the hosting [ComponentActivity] via Robolectric's
 * [shadowOf] — everything the older tests did with `createComposeRule()` still works the same way
 * through this rule.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsAboutContentTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val checkedAppVersion = AppVersionUiState.Checked(
        currentVersion = "1.0.0",
        latestVersion = "1.0.1",
        updateAvailable = true,
        lastCheckedAtMillis = 0L
    )

    private val availableBackendVersion = BackendVersionUiState.Available(
        version = "2.2.0",
        latestVersion = "2.3.0",
        updateAvailable = true,
        checkedAt = "2026-07-02T22:40:54.326377Z"
    )

    @Test
    fun settingsAboutContent_checkNow_invokesCallback() {
        var checked = false
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = { checked = true }
            )
        }

        composeTestRule.onNodeWithText("Check Now").performClick()

        assert(checked)
    }

    @Test
    fun settingsAboutContent_displaysAppVersionInfo() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = checkedAppVersion,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("Current version: 1.0.0").assertExists()
        composeTestRule.onNodeWithText("Latest version: 1.0.1").assertExists()
        composeTestRule.onNodeWithText("Update available!").assertExists()
    }

    @Test
    fun settingsAboutContent_appVersionUnavailable_showsCurrentVersionWithoutCrashing() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Unavailable(currentVersion = "1.0.0"),
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("Current version: 1.0.0").assertExists()
        composeTestRule.onNodeWithText("Latest version: unknown").assertExists()
    }

    @Test
    fun settingsAboutContent_backendVersionAvailable_displaysBackendSection() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = availableBackendVersion,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("Backend version: 2.2.0").assertExists()
        composeTestRule.onNodeWithText("Backend update status: update available").assertExists()
    }

    /**
     * Issue #35's required fallback: a failed/404/timed-out `GET /version` must render the
     * literal strings "unknown" and "unsupported check", never crash, and never block the rest of
     * the screen (the app-version section and Save button below still render).
     */
    @Test
    fun settingsAboutContent_backendVersionUnsupported_rendersUnknownFallback_doesNotCrash() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = checkedAppVersion,
                backendVersionState = BackendVersionUiState.Unsupported,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("Backend version: unknown").assertExists()
        composeTestRule.onNodeWithText("Backend update status: unsupported check").assertExists()
        // The rest of the screen (unrelated to the failed backend call) still renders.
        composeTestRule.onNodeWithText("Current version: 1.0.0").assertExists()
        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun settingsAboutContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    /**
     * Issue #116: Save errors render as a bordered/tinted [com.derekwinters.chores.ui.common.SettingsBanner], not plain text.
     */
    @Test
    fun settingsAboutContent_saveError_rendersStyledBanner() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Error("Save failed"),
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithTag("ErrorBanner").assertExists()
    }

    // --- Issue #35: repo links ---------------------------------------------------------------

    @Test
    fun settingsAboutContent_repoLinks_arePresent() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("chores-web-docs").assertExists()
        composeTestRule.onNodeWithText("chores-web-frontend releases").assertExists()
        composeTestRule.onNodeWithText("chores-web-backend releases").assertExists()
    }

    @Test
    fun settingsAboutContent_docsLink_opensCorrectUri() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("chores-web-docs").performScrollTo().performClick()

        val shadowActivity = shadowOf(composeTestRule.activity)
        val fired = shadowActivity.nextStartedActivity
        assert(fired.action == Intent.ACTION_VIEW)
        assert(fired.data.toString() == RepoLinks.DOCS)
    }

    @Test
    fun settingsAboutContent_frontendReleasesLink_opensCorrectUri() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("chores-web-frontend releases").performScrollTo().performClick()

        val fired = shadowOf(composeTestRule.activity).nextStartedActivity
        assert(fired.action == Intent.ACTION_VIEW)
        assert(fired.data.toString() == RepoLinks.FRONTEND_RELEASES)
    }

    @Test
    fun settingsAboutContent_backendReleasesLink_opensCorrectUri() {
        composeTestRule.setContent {
            SettingsAboutContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                onSave = {},
                onCheckAppVersionNow = {}
            )
        }

        composeTestRule.onNodeWithText("chores-web-backend releases").performScrollTo().performClick()

        val fired = shadowOf(composeTestRule.activity).nextStartedActivity
        assert(fired.action == Intent.ACTION_VIEW)
        assert(fired.data.toString() == RepoLinks.BACKEND_RELEASES)
    }
}
