package com.derekwinters.chores.ui.snapshots

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.CurrentTheme
import com.derekwinters.chores.data.model.ThemeDefaultInfo
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.chores.ChoreListContent
import com.derekwinters.chores.ui.settings.SettingsAboutContent
import com.derekwinters.chores.ui.theme.ChoresTheme
import com.derekwinters.chores.ui.theme.PillBadgeTokens
import com.derekwinters.chores.ui.theme.Space
import com.derekwinters.chores.ui.theme.ThemePreferenceContent
import com.derekwinters.chores.ui.theme.ThemePreferenceData
import com.derekwinters.chores.ui.theme.parseHexColor
import com.derekwinters.chores.ui.theme.pillShape
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Issue #15 (design-token rollout iteration 5, derekwinters/chores-web-docs#11): Roborazzi
 * snapshot catalog of the mapping-matrix components, rendered under a dark and a light ("paper")
 * [ThemeOption] fixture so any regression in how tokens/theme colors reach the components shows
 * up as a pixel diff.
 *
 * Roborazzi, not Paparazzi: Paparazzi does not support com.android.application modules
 * (cashapp/paparazzi#107) and these composables live in the single :app application module.
 * See docs/snapshot-testing.md for the record/verify workflow.
 *
 * Goldens live in `app/src/test/snapshots/` (the path is passed per-capture below — the
 * version-stable mechanism — rather than through the plugin's gradle DSL). Naming:
 * `<component>_<variant>_<dark|paper>.png`.
 *
 * Notes on catalog composition:
 *  - `PillBadge` (ActivityLogScreen) is private; the badge here is an equivalent inline
 *    reconstruction using the exact same tokens ([pillShape], [PillBadgeTokens]) and the same
 *    theme-color mapping (success/warning/error for actions, primary/secondary for targets).
 *    Production visibility was deliberately not widened for tests.
 *  - `ChoreRow` is private; the closest public self-contained piece is [ChoreListContent], so
 *    the chore-row snapshots capture the list content with one row expanded via a click and one
 *    left collapsed (this also covers the filter icon row and the due-status accent bar).
 *  - The theme-preference tile (`ThemeOptionCard`) is private; [ThemePreferenceContent] is the
 *    public wrapper and is captured whole (selected + unselected tiles).
 *  - Buttons/OutlinedTextField/AlertDialog are plain M3 samples under [ChoresTheme], mirroring
 *    their real call sites (ChoreForm's field, the chore delete confirmation dialog).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ComponentSnapshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Backend "dark" built-in palette (9-color wire format; see data/model/ThemeOption.kt).
    private val darkTheme = ThemeOption(
        id = "snapshot-dark",
        name = "Dark",
        background = "#080c14",
        surface = "#16202e",
        surface2 = "#1e2d40",
        accent = "#73B1DD",
        primary = "#3574B3",
        secondary = "#4a5568",
        success = "#3db87a",
        warning = "#e8a930",
        error = "#e05c6a"
    )

    // Backend "paper" built-in palette — the light-background case (ChoresTheme picks its
    // light/dark base scheme from background luminance).
    private val paperTheme = ThemeOption(
        id = "snapshot-paper",
        name = "Paper",
        background = "#f0ede6",
        surface = "#faf8f3",
        surface2 = "#f5f0e9",
        accent = "#b8860b",
        primary = "#8b6914",
        secondary = "#7a7a6a",
        success = "#558b2f",
        warning = "#e0860b",
        error = "#d32f2f"
    )

    // Minimal Chore fixtures (same shape as ChoreListContentTest's): one due chore with an
    // assignee and next-due date (red accent bar) and one completed chore (muted accent bar).
    private val dueChore = Chore(
        id = 1,
        name = "Dishes",
        points = 5,
        state = "due",
        nextDue = "2026-07-05",
        currentAssignee = "alice",
        eligiblePeople = listOf("alice", "bob"),
        scheduleSummary = "Every 3 days"
    )
    private val completeChore = Chore(
        id = 2,
        name = "Trash",
        points = 3,
        state = "complete",
        nextDue = "2026-07-12",
        currentAssignee = null,
        eligiblePeople = listOf("alice", "bob")
    )

    private fun goldenPath(fileName: String) = "src/test/snapshots/$fileName"

    /** Themed full-screen host so every snapshot carries the theme's real background color. */
    private fun captureThemedContent(
        theme: ThemeOption,
        fileName: String,
        content: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            ChoresTheme(themeOption = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    content()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(goldenPath(fileName))
    }

    // --- PillBadge (Activity Log action/target badges; inline reconstruction, see class doc) ---

    /**
     * Same construction as ActivityLogScreen's private `PillBadge`: [pillShape], translucent
     * container ([PillBadgeTokens.fillAlpha] tint of the badge color), opaque content color,
     * labelMedium text, [PillBadgeTokens] padding. Variants mirror `actionBadgeColor` (success/
     * warning/error/neutral) and `targetBadgeColor` (primary=Chore, secondary=User).
     */
    @Composable
    private fun PillBadgeReconstruction(text: String, color: Color) {
        Surface(
            shape = pillShape,
            color = color.copy(alpha = PillBadgeTokens.fillAlpha),
            contentColor = color
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(
                    horizontal = PillBadgeTokens.paddingX,
                    vertical = PillBadgeTokens.paddingY
                )
            )
        }
    }

    @Composable
    private fun PillBadgeCatalog(theme: ThemeOption) {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            // Action badge variants (actionBadgeColor's mapping).
            PillBadgeReconstruction("Completed", parseHexColor(theme.success))
            PillBadgeReconstruction("Skipped", parseHexColor(theme.warning))
            PillBadgeReconstruction("Deleted", parseHexColor(theme.error))
            PillBadgeReconstruction("Updated", MaterialTheme.colorScheme.onSurfaceVariant)
            // Target badge variants (targetBadgeColor's mapping).
            PillBadgeReconstruction("Chore", parseHexColor(theme.primary))
            PillBadgeReconstruction("User", parseHexColor(theme.secondary))
        }
    }

    @Test
    fun pillBadge_variants_dark() {
        captureThemedContent(darkTheme, "pillbadge_variants_dark.png") {
            PillBadgeCatalog(darkTheme)
        }
    }

    @Test
    fun pillBadge_variants_paper() {
        captureThemedContent(paperTheme, "pillbadge_variants_paper.png") {
            PillBadgeCatalog(paperTheme)
        }
    }

    // --- ChoreRow via ChoreListContent (one expanded row, one collapsed; see class doc) --------

    private fun captureChoreList(theme: ThemeOption, fileName: String) {
        composeTestRule.setContent {
            ChoresTheme(themeOption = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChoreListContent(
                        uiState = UiState.Success(listOf(dueChore, completeChore)),
                        completingChoreId = null,
                        onComplete = { _, _ -> }
                    )
                }
            }
        }
        // Expand the first (due) row so the snapshot covers the expanded detail grid + action
        // icon row as well as the collapsed second row.
        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onRoot().captureRoboImage(goldenPath(fileName))
    }

    @Test
    fun choreRow_listExpandedAndCollapsed_dark() {
        captureChoreList(darkTheme, "chorerow_list_dark.png")
    }

    @Test
    fun choreRow_listExpandedAndCollapsed_paper() {
        captureChoreList(paperTheme, "chorerow_list_paper.png")
    }

    // --- Buttons (primary filled Button + secondary TextButton, as used across forms) ---------

    @Composable
    private fun ButtonCatalog() {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Button(onClick = {}) { Text("Save Chore") }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                TextButton(onClick = {}) { Text("Cancel") }
                TextButton(onClick = {}) { Text("Clear filters") }
            }
        }
    }

    @Test
    fun buttons_primarySecondary_dark() {
        captureThemedContent(darkTheme, "buttons_primarysecondary_dark.png") { ButtonCatalog() }
    }

    @Test
    fun buttons_primarySecondary_paper() {
        captureThemedContent(paperTheme, "buttons_primarysecondary_paper.png") { ButtonCatalog() }
    }

    // --- OutlinedTextField (as used by ChoreForm / login / chores search) ----------------------

    @Composable
    private fun TextFieldCatalog() {
        Column(
            modifier = Modifier.padding(Space.lg),
            verticalArrangement = Arrangement.spacedBy(Space.md)
        ) {
            OutlinedTextField(
                value = "Vacuum the stairs",
                onValueChange = {},
                label = { Text("Chore name") },
                singleLine = true
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Search chores") },
                singleLine = true
            )
        }
    }

    @Test
    fun textField_outlined_dark() {
        captureThemedContent(darkTheme, "textfield_outlined_dark.png") { TextFieldCatalog() }
    }

    @Test
    fun textField_outlined_paper() {
        captureThemedContent(paperTheme, "textfield_outlined_paper.png") { TextFieldCatalog() }
    }

    // --- Theme preference tiles (ThemePreferenceContent; selected + unselected) ----------------

    private fun captureThemePreference(theme: ThemeOption, fileName: String) {
        val data = ThemePreferenceData(
            themes = listOf(darkTheme, paperTheme),
            // Personal override selected on the capture theme, so the snapshot shows one
            // selected (elevated/shadowed) tile and unselected tiles side by side.
            current = CurrentTheme(theme = theme, isPersonalOverride = true),
            defaultInfo = ThemeDefaultInfo(id = darkTheme.id, name = darkTheme.name)
        )
        composeTestRule.setContent {
            ChoresTheme(themeOption = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ThemePreferenceContent(uiState = UiState.Success(data), onSelectTheme = {})
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(goldenPath(fileName))
    }

    @Test
    fun themePreference_tiles_dark() {
        captureThemePreference(darkTheme, "themepreference_tiles_dark.png")
    }

    @Test
    fun themePreference_tiles_paper() {
        captureThemePreference(paperTheme, "themepreference_tiles_paper.png")
    }

    // --- AlertDialog (the chore delete confirmation shape) -------------------------------------

    private fun captureAlertDialog(theme: ThemeOption, fileName: String) {
        composeTestRule.setContent {
            ChoresTheme(themeOption = theme) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Delete chore?") },
                    text = { Text("This will permanently delete the chore and cannot be undone.") },
                    confirmButton = { TextButton(onClick = {}) { Text("Delete") } },
                    dismissButton = { TextButton(onClick = {}) { Text("Cancel") } }
                )
            }
        }
        // Dialogs compose into their own window/root, so capture the dialog node rather than
        // onRoot() (which would match multiple roots).
        composeTestRule.onNode(isDialog()).captureRoboImage(goldenPath(fileName))
    }

    @Test
    fun alertDialog_deleteConfirm_dark() {
        captureAlertDialog(darkTheme, "alertdialog_deleteconfirm_dark.png")
    }

    @Test
    fun alertDialog_deleteConfirm_paper() {
        captureAlertDialog(paperTheme, "alertdialog_deleteconfirm_paper.png")
    }

    // --- Settings About screen (issue #35: own-app version + backend version + repo links) -----

    private fun captureSettingsAbout(theme: ThemeOption, fileName: String) {
        composeTestRule.setContent {
            ChoresTheme(themeOption = theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsAboutContent(
                        uiState = UiState.Success(ConfigDto().toDomain()),
                        saveState = UiState.Idle,
                        appVersionState = AppVersionUiState.Checked(
                            currentVersion = "1.1.0",
                            latestVersion = "1.2.0",
                            updateAvailable = true,
                            lastCheckedAtMillis = 0L
                        ),
                        backendVersionState = BackendVersionUiState.Available(
                            version = "2.2.0",
                            latestVersion = "2.3.0",
                            updateAvailable = true,
                            checkedAt = "2026-07-02T22:40:54.326377Z"
                        ),
                        onSave = {},
                        onCheckAppVersionNow = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(goldenPath(fileName))
    }

    @Test
    fun settingsAbout_sections_dark() {
        captureSettingsAbout(darkTheme, "settingsabout_sections_dark.png")
    }

    @Test
    fun settingsAbout_sections_paper() {
        captureSettingsAbout(paperTheme, "settingsabout_sections_paper.png")
    }
}
