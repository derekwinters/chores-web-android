package com.derekwinters.chores.ui.log

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #19 behaviors: row expand shows amendment diffs, pagination controls (area: ui, android).
 * Exercises [ActivityLogContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ActivityLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val amendment = LogEntry(
        id = 1,
        choreId = 1,
        choreName = "Dishes",
        person = "alice",
        action = "updated",
        timestamp = "2026-07-01",
        reassignedTo = null,
        assignee = null,
        fieldName = "points",
        oldValue = "5",
        newValue = "8"
    )

    private val personEntry = LogEntry(
        id = 2,
        choreId = 0,
        choreName = "Person: bob",
        person = "alice",
        action = "password_changed",
        timestamp = "2026-07-01",
        reassignedTo = null,
        assignee = null,
        fieldName = null,
        oldValue = null,
        newValue = null
    )

    @Test
    fun activityLogContent_expandAmendment_showsFieldDiff() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("points: 5 -> 8").assertExists()
    }

    @Test
    fun activityLogContent_choreEntry_showsChoreChipAndSeparatedActionTarget() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        // Not onNodeWithText("Chore") -- the filters row also has a "Chore" text-field label.
        composeTestRule.onNodeWithTag("targetTypeChip").assertTextEquals("Chore")
        composeTestRule.onNodeWithText("updated").assertExists()
        composeTestRule.onNodeWithText("Dishes").assertExists()
    }

    @Test
    fun activityLogContent_personEntry_showsUserChipAndStripsPrefix() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(personEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetTypeChip").assertTextEquals("User")
        composeTestRule.onNodeWithText("password_changed").assertExists()
        composeTestRule.onNodeWithText("bob").assertExists()
    }

    @Test
    fun activityLogContent_expandEntry_showsFullTimestamp() {
        val timestampedEntry = amendment.copy(timestamp = "2026-07-02T22:40:54.326377Z")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(timestampedEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithText("Timestamp: ${formatDateTime("2026-07-02T22:40:54.326377Z")}").assertExists()
    }

    @Test
    fun activityLogContent_relativeTimestamp_justNow() {
        val justNowEntry = amendment.copy(timestamp = java.time.Instant.now().toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(justNowEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("alice · just now").assertExists()
    }

    @Test
    fun activityLogContent_previousPageDisabledOnFirstPage() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(emptyList(), total = 0, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Previous").assertIsNotEnabled()
    }
}
