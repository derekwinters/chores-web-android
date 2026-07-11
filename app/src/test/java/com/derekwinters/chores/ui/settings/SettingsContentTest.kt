package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #20 behaviors: General/Auth/Chores/About sections and their nav entries (area: ui,
 * android). Exercises [SettingsContent] directly (no Hilt component needed).
 *
 * Issue #35: [SettingsContent]'s About section now takes [AppVersionUiState]/
 * [BackendVersionUiState] instead of the removed backend-sourced `UpdateCheckStatus`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsContent_editAppTitle_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                navActions = SettingsNavActions(),
                onSave = { saved = it },
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("App Title").performTextClearance()
        composeTestRule.onNodeWithText("App Title").performTextInput("My House")
        composeTestRule.onNodeWithText("Save Settings").performScrollTo().performClick()

        assert(saved?.appTitle == "My House")
    }

    @Test
    fun settingsContent_authEventLogLink_invokesNavCallback() {
        var navigated = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                navActions = SettingsNavActions(onNavigateToAuthLog = { navigated = true }),
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Auth Event Log").performClick()

        assert(navigated)
    }

    @Test
    fun settingsContent_checkNow_invokesCallback() {
        var checked = false
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Loading,
                navActions = SettingsNavActions(),
                onSave = {},
                onCheckForUpdates = { checked = true }
            )
        }

        composeTestRule.onNodeWithText("Check Now").performScrollTo().performClick()

        assert(checked)
    }

    @Test
    fun settingsContent_appVersion_displaysCheckedState() {
        val status = AppVersionUiState.Checked(
            currentVersion = "1.0.0",
            latestVersion = "1.0.0",
            updateAvailable = false,
            lastCheckedAtMillis = 0L
        )
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = status,
                backendVersionState = BackendVersionUiState.Loading,
                navActions = SettingsNavActions(),
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Current version: 1.0.0").performScrollTo().assertExists()
    }

    @Test
    fun settingsContent_backendVersionUnsupported_rendersUnknownFallback() {
        composeTestRule.setContent {
            SettingsContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                appVersionState = AppVersionUiState.Loading,
                backendVersionState = BackendVersionUiState.Unsupported,
                navActions = SettingsNavActions(),
                onSave = {},
                onCheckForUpdates = {}
            )
        }

        composeTestRule.onNodeWithText("Backend version: unknown").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("Backend update status: unsupported check").performScrollTo().assertExists()
    }
}
