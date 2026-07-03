package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.PointsLogEntry
import com.derekwinters.chores.data.repository.PointsLogPage
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Issue #23 behaviors: inline edit + delete-confirmation warning (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class PointsLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val entry = PointsLogEntry(id = 1, person = "alice", points = 5, choreId = 4, completedAt = "2026-07-01")

    @Test
    fun pointsLogContent_editEntry_savesUpdatedValues() {
        var updated: Triple<Int, String, Int>? = null
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { id, person, points -> updated = Triple(id, person, points) },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        composeTestRule.onNodeWithText("Points").performTextClearance()
        composeTestRule.onNodeWithText("Points").performTextInput("8")
        composeTestRule.onNodeWithText("Save").performClick()

        assert(updated == Triple(1, "alice", 8))
    }

    @Test
    fun pointsLogContent_deleteEntry_requiresConfirmation() {
        var deletedId: Int? = null
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = { deletedId = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice").performClick()
        composeTestRule.onNodeWithText("Delete Entry").performClick()
        composeTestRule.onNodeWithText("This will reverse the points on the person, floored at 0, and cannot be undone.").assertExists()

        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deletedId == 1)
    }

    @Test
    fun pointsLogContent_rendersFormattedTimestamp() {
        val timestampedEntry = entry.copy(completedAt = "2026-07-02T22:40:54.326377Z")
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(timestampedEntry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Chore #4 · ${formatDateTime("2026-07-02T22:40:54.326377Z")}").assertExists()
    }

    @Test
    fun pointsLogContent_malformedTimestamp_fallsBackToRawString() {
        // entry.completedAt = "2026-07-01" is date-only, so Instant.parse throws and this
        // exercises the raw-string fallback path.
        composeTestRule.setContent {
            PointsLogContent(
                uiState = UiState.Success(PointsLogPage(listOf(entry), total = 1, offset = 0, limit = 20)),
                onUpdate = { _, _, _ -> },
                onDelete = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Chore #4 · 2026-07-01").assertExists()
    }
}
