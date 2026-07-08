package com.derekwinters.chores.ui.theme

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #24 behaviors: tap a theme row to set household default; the edit dialog renames a theme
 * (area: ui, android). Exercises [ThemeAdminContent] directly (no Hilt needed).
 *
 * The real API exposes no `is_builtin` flag, so unlike a prior version of this test, there's no
 * client-side "built-in themes can't be renamed/deleted" UI gating to exercise here — that
 * protection is enforced server-side only.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ThemeAdminContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val theme = ThemeOption(
        id = "1", name = "Dark", background = "#000000", surface = "#111111",
        surface2 = "#222222", accent = "#333333", primary = "#444444", secondary = "#555555",
        success = "#00FF00", warning = "#FFFF00", error = "#FF0000"
    )

    @Test
    fun themeAdminContent_tapRow_setsDefault() {
        var defaultId: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = { defaultId = it },
                onCreate = { _, _ -> },
                onRename = { _, _ -> },
                onUpdateColors = { _, _ -> },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Dark").performClick()

        assert(defaultId == "1")
    }

    @Test
    fun themeAdminContent_editTheme_saveRenamesTheme() {
        var renamedId: String? = null
        var renamedTo: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { id, newName -> renamedId = id; renamedTo = newName },
                onUpdateColors = { _, _ -> },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        // The edit dialog's pre-filled name field and the underlying theme row both display
        // "Dark" simultaneously, so onNodeWithText alone is ambiguous — the field is the second
        // ("Dark") node composed, since the dialog opens on top of the row.
        composeTestRule.onAllNodesWithText("Dark")[1].performTextReplacement("Midnight")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(renamedId == "1")
        assert(renamedTo == "Midnight")
    }

    @Test
    fun themeAdminContent_editTheme_deleteConfirmDeletes() {
        var deletedId: String? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { _, _ -> },
                onUpdateColors = { _, _ -> },
                onDelete = { deletedId = it }
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("Delete Theme").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deletedId == "1")
    }

    /**
     * Issue #131: the row's "Copy" action opens the create dialog pre-named "<name> Copy" and
     * creates the new theme from the tapped row's colors (not the list's first theme).
     */
    @Test
    fun themeAdminContent_copyTheme_createsFromTappedRowsColors() {
        val frog = theme.copy(id = "2", name = "Frog", primary = "#777777")
        var createdName: String? = null
        var createdSource: ThemeOption? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme, frog)),
                onSetDefault = {},
                onCreate = { name, source -> createdName = name; createdSource = source },
                onRename = { _, _ -> },
                onUpdateColors = { _, _ -> },
                onDelete = {}
            )
        }

        // Rows render in list order, so the second "Copy" button belongs to the Frog row.
        composeTestRule.onAllNodesWithText("Copy")[1].performClick()
        composeTestRule.onNodeWithText("Frog Copy").assertExists() // pre-filled name field
        composeTestRule.onNodeWithText("Create").performClick()

        assert(createdName == "Frog Copy")
        assert(createdSource == frog)
    }

    /** Issue #130: editing a hex field and saving sends the full updated palette (no rename). */
    @Test
    fun themeAdminContent_editTheme_changedColorSavesUpdatedColors() {
        var renamed = false
        var updatedId: String? = null
        var updatedColors: ThemeOption? = null
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { _, _ -> renamed = true },
                onUpdateColors = { id, colors -> updatedId = id; updatedColors = colors },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("#444444").performScrollTo().performTextReplacement("#ABCDEF")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(updatedId == "1")
        assert(updatedColors == theme.copy(primary = "#ABCDEF"))
        // Name untouched, so no rename call should fire.
        assert(!renamed)
    }

    /** Issue #130: any invalid hex field disables Save. */
    @Test
    fun themeAdminContent_editTheme_invalidHexDisablesSave() {
        composeTestRule.setContent {
            ThemeAdminContent(
                uiState = UiState.Success(listOf(theme)),
                onSetDefault = {},
                onCreate = { _, _ -> },
                onRename = { _, _ -> },
                onUpdateColors = { _, _ -> },
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Edit").performClick()
        composeTestRule.onNodeWithText("#444444").performScrollTo().performTextReplacement("nope")
        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }
}
