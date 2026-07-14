package com.derekwinters.chores.ui.notifications

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #45 behaviors: the Notification Log renders read/unread rows distinctly, offers a per-row
 * acknowledge affordance on unread rows only, and handles the empty/error states. Exercises
 * [NotificationLogContent] directly (no Hilt component needed), same style as ActivityLogContentTest.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class NotificationLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun notification(id: Int, acknowledgedAt: String? = null, createdAt: String = "2026-07-14T08:00:00Z") =
        Notification(
            id = id,
            personId = 7,
            type = "chore_due",
            choreId = id,
            title = "Notification $id",
            body = "Body $id",
            createdAt = createdAt,
            deliveredAt = null,
            acknowledgedAt = acknowledgedAt,
            dismissedAt = null
        )

    @Test
    fun notificationLogContent_empty_showsEmptyState() {
        composeTestRule.setContent {
            NotificationLogContent(uiState = UiState.Success(emptyList()), onAcknowledge = {})
        }

        composeTestRule.onNodeWithTag("notificationLogEmpty").assertExists()
        composeTestRule.onNodeWithText("No notifications").assertExists()
    }

    @Test
    fun notificationLogContent_error_showsMessage() {
        composeTestRule.setContent {
            NotificationLogContent(
                uiState = UiState.Error("Unable to reach the server. Check your connection and server URL."),
                onAcknowledge = {}
            )
        }

        composeTestRule.onNodeWithText("Unable to reach the server. Check your connection and server URL.")
            .assertExists()
    }

    @Test
    fun notificationLogContent_unreadRow_showsTitleBodyAndAcknowledgeButton() {
        composeTestRule.setContent {
            NotificationLogContent(
                uiState = UiState.Success(listOf(notification(id = 1))),
                onAcknowledge = {}
            )
        }

        composeTestRule.onNodeWithText("Notification 1").assertExists()
        composeTestRule.onNodeWithText("Body 1").assertExists()
        composeTestRule.onNodeWithText("Mark as read").assertExists()
        composeTestRule.onNodeWithTag("notificationUnreadDot", useUnmergedTree = true).assertExists()
    }

    @Test
    fun notificationLogContent_readRow_hasNoAcknowledgeAffordanceOrUnreadIndicator() {
        composeTestRule.setContent {
            NotificationLogContent(
                uiState = UiState.Success(listOf(notification(id = 2, acknowledgedAt = "2026-07-14T09:00:00Z"))),
                onAcknowledge = {}
            )
        }

        composeTestRule.onNodeWithText("Notification 2").assertExists()
        composeTestRule.onNodeWithTag("acknowledgeButton_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("notificationUnreadDot", useUnmergedTree = true).assertDoesNotExist()
        composeTestRule.onNodeWithTag("notificationUnreadBar", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun notificationLogContent_acknowledgeButton_invokesCallbackWithId() {
        var acked: Int? = null
        composeTestRule.setContent {
            NotificationLogContent(
                uiState = UiState.Success(listOf(notification(id = 1))),
                onAcknowledge = { acked = it }
            )
        }

        composeTestRule.onNodeWithTag("acknowledgeButton_1").performClick()

        assert(acked == 1) { "expected onAcknowledge(1), got $acked" }
    }

    @Test
    fun notificationLogContent_row_showsRelativeTimestamp() {
        composeTestRule.setContent {
            NotificationLogContent(
                uiState = UiState.Success(listOf(notification(id = 1, createdAt = java.time.Instant.now().toString()))),
                onAcknowledge = {}
            )
        }

        composeTestRule.onNodeWithText("just now").assertIsDisplayed()
    }
}
