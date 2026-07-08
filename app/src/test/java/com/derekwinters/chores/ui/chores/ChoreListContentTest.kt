package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
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
    fun choreListContent_addChoreFab_showsTextLabelAndInvokesCallback() {
        // Issue #70: extended FAB with an "Add Chore" text label, not icon-only.
        var added = false
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onAddChore = { added = true }
            )
        }

        // ExtendedFloatingActionButton's icon+text don't merge into a single semantics node by
        // default, so the merged-tree finder can't locate the text node -- useUnmergedTree is
        // required here.
        composeTestRule.onNodeWithText("Add Chore", useUnmergedTree = true).performClick()

        assert(added)
    }

    @Test
    fun choreListContent_filterIconRow_showsOneIconPerGroupPlusOverflow() {
        // Issue #162: the "Filters" text button is replaced by a compact row of per-group filter
        // icons (Assignee, Status, Due-within) plus an overflow "More filters" icon that opens
        // the full ChoreFiltersDialog.
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithContentDescription("Filter by assignee").assertExists()
        composeTestRule.onNodeWithContentDescription("Filter by status").assertExists()
        composeTestRule.onNodeWithContentDescription("Filter by due date").assertExists()
        composeTestRule.onNodeWithContentDescription("More filters").assertExists()
    }

    @Test
    fun choreListContent_moreFiltersIcon_opensFullFiltersDialog() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithContentDescription("More filters").performClick()

        // ChoreFiltersDialog's AlertDialog title reuses R.string.filters_title ("Filters").
        composeTestRule.onNodeWithText("Filters").assertExists()
    }

    @Test
    fun choreListContent_rendersCollapsedNameAssigneeNextDue() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("alice").assertExists()
        composeTestRule.onNodeWithText("Next due: Jul 5").assertExists()
        composeTestRule.onNodeWithText("5 points").assertDoesNotExist()
        composeTestRule.onNodeWithText("due").assertDoesNotExist()
    }

    @Test
    fun choreListContent_expandChore_showsPointsAndStatusDetail() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        // Issue #87: 2-column detail grid renders each label/value as separate Text nodes now
        // (label uppercase, e.g. "POINTS"/"STATUS"), replacing the old combined "Points: 5" text.
        composeTestRule.onNodeWithText("POINTS").assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("STATUS").assertExists()
        composeTestRule.onNodeWithText("due").assertExists()
    }

    @Test
    fun choreListContent_unassignedChore_hidesCollapsedAssigneeText() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        // Issue #162: "Completer" is a completion-time concept and must never appear as an
        // assignee value (collapsed or expanded) -- see the next test for the expanded fallback.
        composeTestRule.onNodeWithText("Completer").assertDoesNotExist()
    }

    @Test
    fun choreListContent_unassignedChore_expandedShowsAnyonePlaceholder() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(unassignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Trash").performClick()

        // Issue #162: open chores (no fixed assignee, no rotation) show "Anyone" instead of the
        // old static "Completer" placeholder -- "Completer" is a completion-time concept.
        composeTestRule.onNodeWithText("ASSIGNEE").assertExists()
        composeTestRule.onNodeWithText("Anyone").assertExists()
        composeTestRule.onNodeWithText("Completer").assertDoesNotExist()
    }

    @Test
    fun choreListContent_rotatingChoreWithNoCurrentAssignee_expandedShowsNextAssignee() {
        // Issue #162: rotating chores fall back to the server-computed next-in-rotation assignee
        // rather than "Anyone" (which is reserved for genuinely open/anyone-can-do chores).
        val rotatingChore = unassignedChore.copy(
            id = 3,
            name = "Vacuum",
            assignmentType = "rotating",
            nextAssignee = "carol"
        )
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(rotatingChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Vacuum").performClick()

        composeTestRule.onNodeWithText("carol").assertExists()
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

        composeTestRule.onNodeWithText("Dishes").performClick()
        // The expanded row's action-button Row can render taller than the viewport once the
        // Add-Chore FAB's carved-out bottom padding is applied; performScrollTo() is a safe
        // no-op when the node is already fully visible, so it's applied consistently to every
        // expanded-row button click in this file rather than only the ones observed to need it.
        composeTestRule.onNodeWithContentDescription("Complete").performScrollTo().performClick()

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

        composeTestRule.onNodeWithText("Trash").performClick()
        composeTestRule.onNodeWithContentDescription("Complete").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Who completed this?").assertExists()

        composeTestRule.onNodeWithText("bob").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(completed == (unassignedChore to "bob"))
    }

    @Test
    fun choreListContent_completingUnassignedChoreWithNoEligiblePeople_fallsBackToAvailablePeople() {
        // An "open" chore's eligiblePeople is an optional restriction (empty means anyone can
        // complete it, not that no one can), so the Completer picker must fall back to every
        // household member rather than showing no options at all.
        val openChoreWithNoRestriction = unassignedChore.copy(eligiblePeople = emptyList())
        var completed: Pair<Chore, String?>? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(openChoreWithNoRestriction)),
                completingChoreId = null,
                availablePeople = listOf("alice", "bob"),
                onComplete = { chore, completedBy -> completed = chore to completedBy }
            )
        }

        composeTestRule.onNodeWithText("Trash").performClick()
        composeTestRule.onNodeWithContentDescription("Complete").performScrollTo().performClick()
        composeTestRule.onNodeWithText("bob").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(completed == (openChoreWithNoRestriction to "bob"))
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

    @Test
    fun choreListContent_search_invokesOnQueryChange() {
        var query: String? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onQueryChange = { query = it }
            )
        }

        composeTestRule.onNodeWithText("Search chores").performTextInput("dish")

        assert(query == "dish")
    }

    @Test
    fun choreListContent_search_clearButtonHiddenWhenEmptyShownWhenNotEmpty() {
        // Issue #69: leading search icon + trailing clear ("x") button on the search field.
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                filters = ChoreFilters(query = "dish")
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear search").assertExists()
    }

    @Test
    fun choreListContent_search_clearButtonResetsQuery() {
        var query: String? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                filters = ChoreFilters(query = "dish"),
                onQueryChange = { query = it }
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear search").performClick()

        assert(query == "")
    }

    @Test
    fun choreListContent_activeFilters_showsCountAndClearAction() {
        var cleared = false
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                totalCount = 2,
                filters = ChoreFilters(query = "dish"),
                onFiltersChange = { cleared = it == ChoreFilters() }
            )
        }

        composeTestRule.onNodeWithText("Showing 1 of 2 chores").assertExists()
        composeTestRule.onNodeWithText("Clear filters").performClick()

        assert(cleared)
    }

    @Test
    fun choreListContent_noActiveFilters_stillShowsCountButHidesClearAction() {
        // Issue #74: the "Showing N of M chores" count is always visible, matching web, even
        // when no filters are active -- "Clear filters" only makes sense when they are.
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                totalCount = 1
            )
        }

        composeTestRule.onNodeWithText("Showing 1 of 1 chores").assertExists()
        composeTestRule.onNodeWithText("Clear filters").assertDoesNotExist()
    }

    @Test
    fun choreListContent_expandDueChore_showsSkipAndHistoryNotMarkDue() {
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithContentDescription("Skip").assertExists()
        composeTestRule.onNodeWithContentDescription("History").assertExists()
        composeTestRule.onNodeWithContentDescription("Mark Due Now").assertDoesNotExist()
    }

    @Test
    fun choreListContent_expandNotDueChore_showsMarkDueNotSkip() {
        val notDueChore = assignedChore.copy(state = "not_due")
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(notDueChore)),
                completingChoreId = null,
                onComplete = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithContentDescription("Mark Due Now").assertExists()
        composeTestRule.onNodeWithContentDescription("Skip").assertDoesNotExist()
    }

    @Test
    fun choreListContent_skipAction_invokesCallback() {
        var skipped: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onSkip = { skipped = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        composeTestRule.onNodeWithContentDescription("Skip").performScrollTo().performClick()

        assert(skipped == assignedChore)
    }

    @Test
    fun choreListContent_deleteAction_requiresConfirmation() {
        var deleted: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onDelete = { deleted = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        // The row's Delete button can render underneath the fixed Add-Chore FAB depending on
        // available viewport height; performScrollTo() uses the LazyColumn's contentPadding
        // headroom (see ChoreListContent) to scroll it clear of the FAB's fixed overlap zone
        // before clicking, so the FAB doesn't intercept the touch instead.
        composeTestRule.onNodeWithContentDescription("Delete").performScrollTo().performClick()
        composeTestRule.onNodeWithText("This also removes all points history for this chore and cannot be undone.").assertExists()

        // Issue #162: the row's own Delete action is now an icon (contentDescription "Delete",
        // clicked above); the confirmation dialog's "Delete" button is text, so it's the only
        // "Delete"-text node left once the dialog is open.
        composeTestRule.onNodeWithText("Delete").performClick()

        assert(deleted == assignedChore)
    }

    @Test
    fun choreListContent_historyAction_invokesCallback() {
        var history: Chore? = null
        composeTestRule.setContent {
            ChoreListContent(
                uiState = UiState.Success(listOf(assignedChore)),
                completingChoreId = null,
                onComplete = { _, _ -> },
                onHistory = { history = it }
            )
        }

        composeTestRule.onNodeWithText("Dishes").performClick()
        // See the Delete test above for why performScrollTo() is needed here.
        composeTestRule.onNodeWithContentDescription("History").performScrollTo().performClick()

        assert(history == assignedChore)
    }
}
