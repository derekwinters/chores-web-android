package com.derekwinters.chores.ui.dashboard

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
 * Issue #12/#17 behaviors: Due Now/Due Soon deep links and tapping a card to open User Detail
 * (area: ui, android). Exercises [DashboardContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DashboardContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val card = DashboardCard(
        personId = 1,
        username = "alice",
        displayName = "Alice",
        points7d = 10,
        goal7d = 12,
        points30d = 40,
        goal30d = 50,
        dueNowCount = 2,
        dueSoonCount = 1
    )

    @Test
    fun dashboardContent_tapCard_navigatesToUserDetail() {
        var navigated: Pair<Int, String>? = null
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions(onNavigateToUserDetail = { id, username -> navigated = id to username })
            )
        }

        composeTestRule.onNodeWithText("Alice").performClick()

        assert(navigated == (1 to "alice"))
    }

    @Test
    fun dashboardContent_tapDueNow_navigatesToChoresWithAssignee() {
        var navigated: Pair<String?, String?>? = null
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions(onNavigateToChores = { assignee, dueWithin -> navigated = assignee to dueWithin })
            )
        }

        composeTestRule.onNodeWithTag("dueNowButton").performClick()

        assert(navigated == ("alice" to null))
    }

    @Test
    fun dashboardContent_progressRow_showsLargeBoldValueAndGoalCaption() {
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions()
            )
        }

        composeTestRule.onNodeWithText("Last 7 Days").assertExists()
        composeTestRule.onNodeWithText("10").assertExists()
        composeTestRule.onNodeWithText("Goal: 12 pts").assertExists()
        composeTestRule.onNodeWithText("Last 30 Days").assertExists()
        composeTestRule.onNodeWithText("40").assertExists()
        composeTestRule.onNodeWithText("Goal: 50 pts").assertExists()
    }

    @Test
    fun dashboardContent_dueColumns_showLabelsAndCounts() {
        composeTestRule.setContent {
            DashboardContent(
                uiState = UiState.Success(listOf(card)),
                navActions = DashboardNavActions()
            )
        }

        composeTestRule.onNodeWithText("Due Now").assertExists()
        composeTestRule.onNodeWithText("Due Soon").assertExists()
        composeTestRule.onNodeWithTag("dueNowButton").assertExists()
        composeTestRule.onNodeWithTag("dueSoonButton").assertExists()
    }
}
