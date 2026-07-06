package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 4: Each section screen has its own local draft and its own Save action
 * (area: android). Tests [SettingsAuthContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsAuthContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsAuthContent_toggleAuthEnabled_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsAuthContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = { saved = it },
                onNavigateToAuthLog = {}
            )
        }

        composeTestRule.onNodeWithTag("authEnabledSwitch").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        assert(saved?.authEnabled == true)
    }

    @Test
    fun settingsAuthContent_authLogLink_invokesNavCallback() {
        var navigated = false
        composeTestRule.setContent {
            SettingsAuthContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {},
                onNavigateToAuthLog = { navigated = true }
            )
        }

        composeTestRule.onNodeWithText("View Event Log").performClick()

        assert(navigated)
    }

    @Test
    fun settingsAuthContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsAuthContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {},
                onNavigateToAuthLog = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    /**
     * Issue #116: Save errors render as a bordered/tinted [SettingsBanner], not plain text.
     */
    @Test
    fun settingsAuthContent_saveError_rendersStyledBanner() {
        composeTestRule.setContent {
            SettingsAuthContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Error("Save failed"),
                onSave = {},
                onNavigateToAuthLog = {}
            )
        }

        composeTestRule.onNodeWithTag("ErrorBanner").assertExists()
    }
}
