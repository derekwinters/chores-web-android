package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/points/state/
 * next_due" and "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null" (area: ui, android, network). Exercises [ChoreListContent] directly
 * (no Hilt component needed) — see ChoreListViewModelTest for the ViewModel-level state machine.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoreListContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val assignedChore = Chore(
        id = 1,
        name = "Dishes",
        points = 5,
        state = "due",
        nextDue = "2026-07-05",
        currentAssignee = "alice",
        eligiblePeople = listOf("alice", "bob")
    )
    private val unassignedChore = Chore(
        id = 2,
        name = "Trash",
        points = 3,
        state = "due",
        nextDue = null,
        currentAssignee = null,
        eligiblePeople = listOf("alice", "bob")
    )

    @Test
    fun choreListContent_rendersNameAssigneePointsStateNextDue() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("alice").assertExists()
        composeTestRule.onNodeWithText("5 points").assertExists()
        composeTestRule.onNodeWithText("due").assertExists()
        composeTestRule.onNodeWithText("Next due: 2026-07-05").assertExists()
    }

    @Test
    fun choreListContent_unassignedChore_showsCompleterPlaceholder() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Completer").assertExists()
    }

    @Test
    fun choreListContent_completingAssignedChore_completesWithoutDialog() {
        var completed: Pair<Chore, String?>? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { chore, completedBy -> completed = chore to completedBy }
            )
        }

        composeTestRule.onNodeWithText("Complete").performClick()

        assert(completed == (assignedChore to null))
    }

    @Test
    fun choreListContent_completingUnassignedChore_opensCompleterPickerAndConfirms() {
        var completed: Pair<Chore, String?>? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { chore, completedBy -> completed = chore to completedBy }
            )
        }

        composeTestRule.onNodeWithText("Complete").performClick()
        composeTestRule.onNodeWithText("Who completed this?").assertExists()

        composeTestRule.onNodeWithText("bob").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(completed == (unassignedChore to "bob"))
    }

    @Test
    fun choreListContent_errorState_showsErrorMessage() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Error("Something went wrong. Please try again."),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Something went wrong. Please try again.").assertExists()
    }
}
