package com.derekwinters.chores.ui.users

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.model.PersonStats
import com.derekwinters.chores.data.model.Redemption
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #17 behaviors: Redeem action is admin-only, and the redeem dialog shows a before/after
 * balance (area: ui, android). Exercises [UserDetailContent] directly (no Hilt component needed).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class UserDetailContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val data = UserDetailData(
        stats = PersonStats(availablePoints = 20, points7d = 10, points30d = 40, totalPoints = 45, completedCount = 8),
        redemptions = emptyList(),
        activity = emptyList()
    )

    @Test
    fun userDetailContent_nonAdmin_hidesRedeemButton() {
        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(data),
                redeemState = UiState.Idle,
                isAdmin = false,
                onValidateAmount = { null },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Redeem Points").assertDoesNotExist()
    }

    @Test
    fun userDetailContent_admin_redeemFlow_showsBeforeAfterBalance() {
        var redeemed: Int? = null
        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(data),
                redeemState = UiState.Idle,
                isAdmin = true,
                onValidateAmount = { text -> if (text.toIntOrNull() in 1..20) null else "invalid" },
                onRedeem = { redeemed = it },
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Redeem Points").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("5")
        composeTestRule.onNodeWithText("Before: 20 -> After: 15").assertExists()

        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(redeemed == 5)
    }

    @Test
    fun userDetailContent_activityAndRedemptions_renderAsCardsWithSeparateFields() {
        val activityEntry = LogEntry(
            id = 1,
            choreId = 4,
            choreName = "Dishes",
            person = "alice",
            action = "completed",
            timestamp = "2026-07-02T22:40:54.326377Z",
            reassignedTo = null,
            assignee = null,
            fieldName = null,
            oldValue = null,
            newValue = null
        )
        val redemption = Redemption(id = 1, amount = 10, redeemedBy = "alice", timestamp = "2026-07-02T22:40:54.326377Z")
        val dataWithHistory = data.copy(activity = listOf(activityEntry), redemptions = listOf(redemption))

        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(dataWithHistory),
                redeemState = UiState.Idle,
                isAdmin = false,
                onValidateAmount = { null },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        // Chore Activity row: chore name -> capitalized action -> date-only formatted timestamp.
        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("Completed").assertExists()

        // Redemption row: amount -> redeemedBy -> date-only formatted timestamp.
        composeTestRule.onNodeWithText("10 pts").assertExists()
        composeTestRule.onNodeWithText("by alice").assertExists()

        composeTestRule.onAllNodesWithText(formatDate("2026-07-02T22:40:54.326377Z"))[0].assertExists()
    }

    @Test
    fun userDetailContent_malformedTimestamp_fallsBackToRawStringInActivityRow() {
        val activityEntry = LogEntry(
            id = 1,
            choreId = 4,
            choreName = "Dishes",
            person = "alice",
            action = "completed",
            timestamp = "not-a-timestamp",
            reassignedTo = null,
            assignee = null,
            fieldName = null,
            oldValue = null,
            newValue = null
        )
        val dataWithHistory = data.copy(activity = listOf(activityEntry))

        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(dataWithHistory),
                redeemState = UiState.Idle,
                isAdmin = false,
                onValidateAmount = { null },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("not-a-timestamp").assertExists()
    }
}
