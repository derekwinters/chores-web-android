package com.derekwinters.chores.ui.log

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reads the resolved text color directly off the node's text layout result -- same
 * `GetTextLayoutResult`-based approach as `ChoresStatsPanelContentTest.textFontSizeSp()`, since
 * Robolectric's headless rendering doesn't support pixel-sampling (`captureToImage`) reliably.
 */
private fun SemanticsNodeInteraction.textColor(): Color {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.first().layoutInput.style.color
}

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
        // useUnmergedTree is required: LogRow's Card is clickable (mergeDescendants = true), and
        // TestTag isn't propagated upward through a merge boundary -- it stays on the exact node
        // (PillBadge's inner Text) it was set on. Same pattern as ChoreListContentTest.
        composeTestRule.onNodeWithTag("targetTypeChip", useUnmergedTree = true).assertTextEquals("Chore")
        // Not onNodeWithText("Updated") -- issue #68's action-type filter chip row also has an
        // "Updated" option with the same humanized label, so asserting by tag disambiguates.
        composeTestRule.onNodeWithTag("actionBadge", useUnmergedTree = true).assertTextEquals("Updated")
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

        composeTestRule.onNodeWithTag("targetTypeChip", useUnmergedTree = true).assertTextEquals("User")
        // Not onNodeWithText("Password Changed") -- issue #68's action-type filter chip row also
        // has a "Password Changed" option with the same humanized label.
        composeTestRule.onNodeWithTag("actionBadge", useUnmergedTree = true).assertTextEquals("Password Changed")
        composeTestRule.onNodeWithText("bob").assertExists()
    }

    /**
     * Issue #73: raw action values are mapped to humanized labels, e.g. "completed" ->
     * "Completed", mirroring chores-web and the Auth Log's (#91) humanization.
     */
    @Test
    fun activityLogContent_humanizesCompletedAction() {
        val entry = amendment.copy(action = "completed", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        // Not onNodeWithText("Completed") -- issue #68's action-type filter chip row also has a
        // "Completed" option with the same humanized label.
        composeTestRule.onNodeWithTag("actionBadge", useUnmergedTree = true).assertTextEquals("Completed")
    }

    /** Issue #73: unmapped/unknown action values fall back to a title-cased transform. */
    @Test
    fun activityLogContent_unmappedAction_fallsBackToTitleCasedTransform() {
        val entry = amendment.copy(action = "some_new_event", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Some New Event").assertExists()
    }

    /**
     * Issue #71: the action value renders inside a dedicated `actionBadge`-tagged pill (not just
     * loose row text), so it can be styled/colored distinctly from the rest of the row.
     */
    @Test
    fun activityLogContent_actionRendersAsBadge() {
        val entry = amendment.copy(action = "completed", fieldName = null, oldValue = null, newValue = null)
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(entry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("actionBadge", useUnmergedTree = true).assertTextEquals("Completed")
    }

    /**
     * Issue #71: the target value (chore name / person name) renders inside a dedicated
     * `targetBadge`-tagged pill, distinct from the existing `targetTypeChip` (User/Chore)
     * indicator.
     */
    @Test
    fun activityLogContent_targetRendersAsBadge() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetBadge", useUnmergedTree = true).assertTextEquals("Dishes")
    }

    /** Issue #71: person-target rows' target badge strips the "Person: " prefix, same as before. */
    @Test
    fun activityLogContent_personTarget_targetBadgeStripsPrefix() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(personEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("targetBadge", useUnmergedTree = true).assertTextEquals("bob")
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

    /**
     * Issue #81: entries 24h+ old get a muted-red timestamp (mirroring chores-web's stale-entry
     * treatment), while entries under 24h keep the default color. Asserted by comparing the two
     * resolved colors rather than pinning to an exact color value/alpha, since the "muted red"
     * comes from `MaterialTheme.colorScheme.error` (no `ChoresTheme` wrapper is present in this
     * test, so the M3 default light error color resolves) -- the color's red channel should
     * dominate green/blue and its alpha should be reduced below fully opaque.
     */
    @Test
    fun activityLogContent_agedTimestamp_rendersInMutedRed() {
        val agedEntry = amendment.copy(timestamp = java.time.Instant.now().minus(java.time.Duration.ofHours(48)).toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(agedEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        val color = composeTestRule.onNodeWithTag("logRowTimestamp", useUnmergedTree = true).textColor()

        assert(color.alpha < 1f) { "expected a muted (reduced-alpha) color, got alpha=${color.alpha}" }
        assert(color.red > color.green && color.red > color.blue) {
            "expected a reddish color, got r=${color.red} g=${color.green} b=${color.blue}"
        }
    }

    /**
     * Issue #81: entries under 24h old keep the default (non-error) timestamp color.
     *
     * Renders a single row rather than recent+aged together: #68's filter panel (action-type
     * chips, date-range fields, clear-filters button) added enough height above the entry list
     * that Robolectric's fixed test-window viewport no longer composes a second LazyColumn item,
     * so `onAllNodesWithTag(...)[1]` became unreachable ("There is 1 node only"). Asserting the
     * resolved color's alpha directly (mirroring [activityLogContent_agedTimestamp_rendersInMutedRed]'s
     * own alpha/redness assertions) verifies the same behavior without depending on two rows
     * being simultaneously visible.
     */
    @Test
    fun activityLogContent_recentTimestamp_keepsDefaultColor() {
        val recentEntry = amendment.copy(timestamp = java.time.Instant.now().minus(java.time.Duration.ofHours(1)).toString())
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(recentEntry), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        val color = composeTestRule.onNodeWithTag("logRowTimestamp", useUnmergedTree = true).textColor()

        assert(color.alpha == 1f) {
            "expected a recent entry to keep the default (fully opaque) color, got alpha=${color.alpha}"
        }
    }

    /**
     * Issue #77: a chevron icon signals the row is expandable (Android otherwise gave no visual
     * cue, unlike web's chevron affordance) and it flips between ExpandMore/ExpandLess as
     * `expanded` toggles. useUnmergedTree is required here too, same reason as
     * targetTypeChip/actionBadge/targetBadge above: the Icon lives inside LogRow's clickable
     * Card, and TestTag doesn't propagate upward through that merge boundary.
     */
    @Test
    fun activityLogContent_chevron_reflectsExpandedState() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("expandChevron", useUnmergedTree = true)
            .assert(hasContentDescription("Expand entry"))

        composeTestRule.onNodeWithText("Dishes").performClick()

        composeTestRule.onNodeWithTag("expandChevron", useUnmergedTree = true)
            .assert(hasContentDescription("Collapse entry"))
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

    // Issue #68 behaviors: action-type filter, start/end date-range filters, "Clear filters".

    /** Selecting an action-type chip updates [LogFilters.action] to that raw action value. */
    @Test
    fun activityLogContent_actionFilterChip_selectingUpdatesFilters() {
        var latest = LogFilters()
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = latest,
                onFiltersChange = { updated -> latest = updated },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        // "Skipped" only appears as a filter chip label here -- amendment's own action badge
        // reads "Updated", so there's no ambiguity with onNodeWithText.
        composeTestRule.onNodeWithText("Skipped").performClick()

        assert(latest.action == "skipped") { "expected action=skipped, got ${latest.action}" }
    }

    /** Re-selecting "All" clears just the action filter, leaving other filters untouched. */
    @Test
    fun activityLogContent_actionFilterChip_selectingAllClearsActionFilter() {
        var latest = LogFilters(action = "completed", person = "alice")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = latest,
                onFiltersChange = { updated -> latest = updated },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("All").performClick()

        assert(latest.action == null) { "expected action=null, got ${latest.action}" }
        assert(latest.person == "alice") { "expected person to be untouched, got ${latest.person}" }
    }

    /** Tapping the Start Date field's calendar icon opens a DatePickerDialog. */
    @Test
    fun activityLogContent_startDateField_tapIconOpensDatePicker() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(start = "2026-07-01"),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Start Date").performClick()

        composeTestRule.onNodeWithText("OK").assertExists()
    }

    /** Confirming the Start Date picker with its pre-selected value keeps the ISO date string. */
    @Test
    fun activityLogContent_startDateDatePicker_confirmingPreSelectedValue_keepsIsoDate() {
        var latest = LogFilters(start = "2026-07-01")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = latest,
                onFiltersChange = { updated -> latest = updated },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Start Date").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        assert(latest.start == "2026-07-01") { "expected start=2026-07-01, got ${latest.start}" }
    }

    /** Confirming the End Date picker updates [LogFilters.end], independent of Start Date. */
    @Test
    fun activityLogContent_endDateDatePicker_confirmingPreSelectedValue_keepsIsoDate() {
        var latest = LogFilters(end = "2026-07-05")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = latest,
                onFiltersChange = { updated -> latest = updated },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("End Date").performClick()
        composeTestRule.onNodeWithText("OK").performClick()

        assert(latest.end == "2026-07-05") { "expected end=2026-07-05, got ${latest.end}" }
    }

    /** No active filters -- "Clear filters" wouldn't make sense to offer, so it's hidden. */
    @Test
    fun activityLogContent_clearFiltersButton_hiddenWhenNoFiltersActive() {
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = LogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("clearFiltersButton").assertDoesNotExist()
    }

    /** Clicking "Clear filters" resets person/chore/action/date-range together. */
    @Test
    fun activityLogContent_clearFiltersButton_resetsAllFilters() {
        var latest = LogFilters(person = "alice", chore = "Dishes", action = "completed", start = "2026-07-01", end = "2026-07-05")
        composeTestRule.setContent {
            ActivityLogContent(
                uiState = UiState.Success(ActivityLogPageState(listOf(amendment), total = 1, page = 1)),
                filters = latest,
                onFiltersChange = { updated -> latest = updated },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithTag("clearFiltersButton").performClick()

        assert(latest == LogFilters()) { "expected all filters cleared, got $latest" }
    }
}
