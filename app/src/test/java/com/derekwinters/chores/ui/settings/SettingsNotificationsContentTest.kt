package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #44: exercises [SettingsNotificationsContent] directly (no Hilt), following the
 * `*ContentTest` convention. Covers rendering of interval/preferences/offline controls and that
 * each control invokes its callback.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsNotificationsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun state(
        pollIntervalMinutes: Long = 60L,
        offlineAlertEnabled: Boolean = true,
        offlineAlertThresholdDays: Int = 3,
        preferences: Map<String, Boolean> = mapOf("chore_due" to true),
        preferencesError: String? = null
    ) = NotificationSettingsUiState(
        pollIntervalMinutes = pollIntervalMinutes,
        offlineAlertEnabled = offlineAlertEnabled,
        offlineAlertThresholdDays = offlineAlertThresholdDays,
        preferences = preferences,
        preferencesError = preferencesError
    )

    @Test
    fun rendersIntervalPreferenceAndOfflineControls() {
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state()),
                saveState = UiState.Idle,
                onSelectInterval = {},
                onToggleOfflineAlert = {},
                onThresholdChange = {},
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("PollIntervalField").assertExists()
        // Default 60 minutes renders as "1 hour".
        composeTestRule.onNodeWithText("1 hour").assertExists()
        composeTestRule.onNodeWithText("Chore due").assertExists()
        composeTestRule.onNodeWithTag("PreferenceToggle_chore_due").assertExists()
        composeTestRule.onNodeWithTag("OfflineAlertToggle").assertExists()
        composeTestRule.onNodeWithTag("OfflineThresholdField").assertExists()
    }

    @Test
    fun selectingInterval_invokesCallback() {
        var selected: Long? = null
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state()),
                saveState = UiState.Idle,
                onSelectInterval = { selected = it },
                onToggleOfflineAlert = {},
                onThresholdChange = {},
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("PollIntervalField").performClick()
        composeTestRule.onNodeWithText("3 hours").performClick()

        assert(selected == 180L) { "expected 180 minutes, was $selected" }
    }

    @Test
    fun togglingOfflineAlert_invokesCallback() {
        var toggled: Boolean? = null
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state(offlineAlertEnabled = true)),
                saveState = UiState.Idle,
                onSelectInterval = {},
                onToggleOfflineAlert = { toggled = it },
                onThresholdChange = {},
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("OfflineAlertToggle").performClick()

        assert(toggled == false) { "expected toggle to false, was $toggled" }
    }

    @Test
    fun togglingPreference_invokesCallbackWithTypeAndValue() {
        var type: String? = null
        var value: Boolean? = null
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state(preferences = mapOf("chore_due" to true))),
                saveState = UiState.Idle,
                onSelectInterval = {},
                onToggleOfflineAlert = {},
                onThresholdChange = {},
                onTogglePreference = { t, v -> type = t; value = v }
            )
        }

        composeTestRule.onNodeWithTag("PreferenceToggle_chore_due").performClick()

        assert(type == "chore_due") { "expected chore_due, was $type" }
        assert(value == false) { "expected false, was $value" }
    }

    @Test
    fun thresholdInput_invokesCallbackWithPositiveInt() {
        var captured: Int? = null
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state(offlineAlertThresholdDays = 3)),
                saveState = UiState.Idle,
                onSelectInterval = {},
                onToggleOfflineAlert = {},
                onThresholdChange = { captured = it },
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("OfflineThresholdField").performTextClearance()
        composeTestRule.onNodeWithTag("OfflineThresholdField").performTextInput("7")

        assert(captured != null && captured!! > 0) { "expected a positive threshold, was $captured" }
    }

    @Test
    fun preferencesError_rendersErrorBanner() {
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state(preferences = emptyMap(), preferencesError = "offline")),
                saveState = UiState.Idle,
                onSelectInterval = {},
                onToggleOfflineAlert = {},
                onThresholdChange = {},
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("ErrorBanner").assertExists()
    }

    @Test
    fun saveError_rendersErrorBanner() {
        composeTestRule.setContent {
            SettingsNotificationsContent(
                uiState = UiState.Success(state()),
                saveState = UiState.Error("save failed"),
                onSelectInterval = {},
                onToggleOfflineAlert = {},
                onThresholdChange = {},
                onTogglePreference = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag("ErrorBanner").assertExists()
    }
}
