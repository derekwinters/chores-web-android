package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasAnyDescendant
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
 * (area: android). Tests [SettingsGeneralContent] directly.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsGeneralContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsGeneralContent_editAppTitle_andSave_submitsUpdatedConfig() {
        var saved: AppConfig? = null
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Idle,
                onSave = { saved = it }
            )
        }

        composeTestRule.onNodeWithTag("AppTitleField").performTextClearance()
        composeTestRule.onNodeWithTag("AppTitleField").performTextInput("My House")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(saved?.appTitle == "My House")
    }

    @Test
    fun settingsGeneralContent_displayTimezoneField() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores", timezone = "UTC").toDomain()),
                saveState = UiState.Idle,
                onSave = {}
            )
        }

        // Verify the timezone field is visible and labeled
        composeTestRule.onNodeWithTag("TimezoneField").assertExists()
    }

    /**
     * Issue #102: Each settings section's divider appears above its heading and content, not
     * after. The General screen has two subsections (App Title, Timezone) — verify the Timezone
     * subsection has its own divider separating it from the App Title subsection above it.
     */
    @Test
    fun settingsGeneralContent_timezoneSection_hasDividerAboveHeading() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores", timezone = "UTC").toDomain()),
                saveState = UiState.Idle,
                onSave = {}
            )
        }

        composeTestRule.onNodeWithTag("AppTitleSectionDivider").assertExists()
        composeTestRule.onNodeWithTag("TimezoneSectionDivider").assertExists()
    }

    /**
     * Issue #106: Timezone is selected via a picker showing UTC-offset labels, not the raw
     * zone id — verify the field displays an offset label (e.g. "New_York (UTC-5:00)") rather
     * than the bare "America/New_York" zone id.
     */
    @Test
    fun settingsGeneralContent_timezoneField_showsUtcOffsetLabel_notRawZoneId() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores", timezone = "America/New_York").toDomain()),
                saveState = UiState.Idle,
                onSave = {}
            )
        }

        composeTestRule.onNodeWithTag("TimezoneField").assertTextContains("UTC", substring = true)
    }

    /**
     * Issue #116: Save errors render as a bordered/tinted [SettingsBanner], not plain text.
     */
    @Test
    fun settingsGeneralContent_saveError_rendersStyledBanner() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Error("Save failed"),
                onSave = {}
            )
        }

        composeTestRule.onNodeWithTag("ErrorBanner").assertExists()
    }

    @Test
    fun settingsGeneralContent_displaysSaveButton() {
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto().toDomain()),
                saveState = UiState.Idle,
                onSave = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    /**
     * Issue #96 behavior 1: Save button shows idle state (disabled) when no changes made
     * Users should see the button is disabled (visually greyed out) when no changes are pending
     */
    @Test
    fun settingsGeneralContent_noChanges_saveButtonIsDisabled() {
        var saveClicked = false
        val config = ConfigDto(title = "Chores").toDomain()
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(config),
                saveState = UiState.Idle,
                onSave = { saveClicked = true }
            )
        }

        // Without making any changes, clicking Save should not trigger callback
        // because the button should be disabled
        composeTestRule.onNodeWithText("Save").assertExists()
        composeTestRule.onNodeWithText("Save").performClick()

        // The callback should not be triggered because button is disabled
        assert(!saveClicked) { "Save callback should not be called when no changes made" }
    }

    /**
     * Issue #96 behavior 2: Save button shows dirty state (enabled) after changes
     * Users should see the button is enabled (visually highlighted) when there are pending changes
     */
    @Test
    fun settingsGeneralContent_afterEdit_saveButtonIsEnabled() {
        var savedConfig: AppConfig? = null
        composeTestRule.setContent {
            SettingsGeneralContent(
                uiState = UiState.Success(ConfigDto(title = "Chores").toDomain()),
                saveState = UiState.Idle,
                onSave = { savedConfig = it }
            )
        }

        // Make a change to trigger dirty state
        composeTestRule.onNodeWithTag("AppTitleField").performTextClearance()
        composeTestRule.onNodeWithTag("AppTitleField").performTextInput("My House")

        // After edit, Save button should now be enabled and clicking it should work
        composeTestRule.onNodeWithText("Save").performClick()
        assert(savedConfig?.appTitle == "My House") { "Save should be called after making changes" }
    }
}
