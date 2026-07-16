package com.derekwinters.chores.ui.dashboard

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #16 behavior: Home is logged-in-user specific — it renders only the signed-in user's own
 * Board card (their trend data + Due Now/Due Soon), reusing [DashboardContent] filtered via
 * [homeCards]. Exercises [HomeContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HomeContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun card(username: String, displayName: String) = DashboardCard(
        personId = username.hashCode(),
        username = username,
        displayName = displayName,
        points7d = 10,
        goal7d = 12,
        points30d = 40,
        goal30d = 50,
        dueNowCount = 2,
        dueSoonCount = 1
    )

    @Test
    fun homeContent_showsOnlySignedInUsersCard() {
        composeTestRule.setContent {
            HomeContent(
                username = "alice",
                uiState = UiState.Success(listOf(card("alice", "Alice"), card("bob", "Bob"))),
                navActions = DashboardNavActions()
            )
        }

        composeTestRule.onNodeWithText("Alice").assertExists()
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
    }

    @Test
    fun homeContent_noMatchingCard_showsHomeEmptyState() {
        composeTestRule.setContent {
            HomeContent(
                username = "carol",
                uiState = UiState.Success(listOf(card("alice", "Alice"))),
                navActions = DashboardNavActions()
            )
        }

        composeTestRule.onNodeWithText("Nothing to show for your account yet…").assertExists()
        composeTestRule.onNodeWithText("Alice").assertDoesNotExist()
    }
}
