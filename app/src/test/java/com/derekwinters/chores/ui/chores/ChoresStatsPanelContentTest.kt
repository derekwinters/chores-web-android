package com.derekwinters.chores.ui.chores

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Reads the requested [androidx.compose.ui.text.TextStyle.fontSize] directly off the node's
 * text layout result, rather than measuring rendered pixel bounds — Robolectric's headless text
 * measurement doesn't reliably scale reported bounds with font size, so bounds-based "is this
 * visually bigger" assertions are flaky here even when the styling is correct.
 */
private fun SemanticsNodeInteraction.textFontSizeSp(): Float {
    val results = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
    return results.first().layoutInput.style.fontSize.value
}

/** Issue #14 behavior: collapsible stats panel (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresStatsPanelContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val stats = ChoresStats(totalEnabledChores = 5, totalPoints = 27, completedLast7Days = 14, dueNext7DaysPoints = 8)

    @Test
    fun statsPanel_defaultsToCollapsed() {
        // Issue #162: collapsed by default on the chore list (previously expanded).
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats))
        }

        composeTestRule.onNodeWithText("5").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Expand stats panel").assertExists()
    }

    @Test
    fun statsPanel_expanded_showsAllFourStats() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        composeTestRule.onNodeWithText("5").assertExists()
        composeTestRule.onNodeWithText("27").assertExists()
        composeTestRule.onNodeWithText("14").assertExists()
        composeTestRule.onNodeWithText("8").assertExists()
    }

    @Test
    fun statsPanel_collapseThenExpand_togglesVisibility() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        composeTestRule.onNodeWithContentDescription("Collapse stats panel").performClick()
        composeTestRule.onNodeWithText("27").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("Expand stats panel").performClick()
        composeTestRule.onNodeWithText("27").assertExists()
    }

    @Test
    fun statsPanel_valueText_usesLargerTypographyThanLabelText() {
        composeTestRule.setContent {
            ChoresStatsPanelContent(uiState = UiState.Success(stats), initiallyExpanded = true)
        }

        val labelFontSize = composeTestRule.onNodeWithText("Total Chores").textFontSizeSp()
        val valueFontSize = composeTestRule.onNodeWithText("5").textFontSizeSp()

        assert(valueFontSize > labelFontSize)
    }
}
