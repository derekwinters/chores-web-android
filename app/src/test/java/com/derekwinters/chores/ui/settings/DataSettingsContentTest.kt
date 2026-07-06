package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #22 behaviors: import confirmation dialog with counts + "replaces all data" warning
 * (area: ui, android). Exercises [DataSettingsContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DataSettingsContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dataSettingsContent_importPreview_confirmInvokesCallback() {
        var confirmed = false
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = ImportPreview(peopleCount = 2, choresCount = 5, settingsCount = 1, rawJson = "{}"),
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = { confirmed = true },
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("2 people, 5 chores, 1 settings", substring = true).assertExists()
        composeTestRule.onNodeWithText("This replaces all existing data and cannot be undone.").assertExists()

        composeTestRule.onNodeWithText("Import").performClick()

        assert(confirmed)
    }

    @Test
    fun dataSettingsContent_pointsLogLink_exists() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Admin Points Log").assertExists()
    }

    @Test
    fun dataSettingsContent_displaysThreeDistinctSections() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Export & Import").assertExists()
        composeTestRule.onNodeWithText("Log Retention").assertExists()
        composeTestRule.onNodeWithText("Data Management").assertExists()
    }

    /**
     * Issue #114: Export & Import, Log Retention, and Data Management each carry their own
     * descriptive copy, not just a bare section heading.
     */
    @Test
    fun dataSettingsContent_eachSection_hasDescriptionText() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText(
            "Back up your data by exporting to a file, or restore from a previous backup by importing."
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Control how long activity logs are kept. Older entries will be automatically deleted."
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Access detailed information about your activity logs."
        ).assertExists()
    }

    @Test
    fun dataSettingsContent_logRetentionSave_buttonExists() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun dataSettingsContent_selectedImportFilename_displaysConfirmation() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = "my_chores_backup.json",
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Selected: my_chores_backup.json").assertExists()
    }

    @Test
    fun dataSettingsContent_exportSuccess_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = "chores-backup.json",
                exportState = UiState.Success("{}"),
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Data exported successfully to: chores-backup.json").assertExists()
    }

    @Test
    fun dataSettingsContent_importSuccess_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Success(com.derekwinters.chores.data.repository.ImportSummary(peopleCount = 2, choresCount = 5, settingsCount = 1)),
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Imported 2 people, 5 chores, 1 settings", substring = true).assertExists()
    }

    /**
     * Issue #117: Export and Import controls each carry their own description text, not just
     * the section-level description.
     */
    @Test
    fun dataSettingsContent_exportImportButtons_haveDescriptionText() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText(
            "Download your data as a JSON file for backup or migration purposes"
        ).assertExists()
        composeTestRule.onNodeWithText(
            "Upload a previously exported JSON file to restore or migrate your data"
        ).assertExists()
    }

    /**
     * Issue #117: Export results render as a bordered/tinted [SettingsBanner], not plain text.
     */
    @Test
    fun dataSettingsContent_exportSuccess_rendersStyledBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Idle,
                selectedImportFilename = null,
                exportFilename = "chores-backup.json",
                exportState = UiState.Success("{}"),
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithTag("SuccessBanner").assertExists()
    }

    /**
     * Issue #117: Import results render as a bordered/tinted [SettingsBanner], not plain text.
     */
    @Test
    fun dataSettingsContent_importResult_rendersStyledBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Success(com.derekwinters.chores.data.repository.ImportSummary(peopleCount = 2, choresCount = 5, settingsCount = 1)),
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithTag("SuccessBanner").assertExists()
    }

    @Test
    fun dataSettingsContent_importError_displaysBanner() {
        composeTestRule.setContent {
            DataSettingsContent(
                logRetentionDays = 90,
                logRetentionInput = "90",
                logRetentionState = UiState.Idle,
                importPreview = null,
                importState = UiState.Error("Import failed"),
                selectedImportFilename = null,
                exportFilename = null,
                exportState = UiState.Idle,
                onExportClick = {},
                onImportClick = {},
                onLogRetentionChange = {},
                onSaveLogRetention = {},
                onClearLogRetentionState = {},
                onConfirmImport = {},
                onCancelImport = {},
                onDismissImportResult = {},
                onNavigateToPointsLog = {}
            )
        }

        composeTestRule.onNodeWithText("Import failed").assertExists()
    }
}
