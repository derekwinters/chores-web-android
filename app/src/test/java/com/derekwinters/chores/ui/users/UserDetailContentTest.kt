package com.derekwinters.chores.ui.users

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        stats = PersonStats(availablePoints = 20, points7d = 10, points30d = 40, totalPoints = 45, completedCount = 8, redeemed = 25),
        redemptions = emptyList(),
        activity = emptyList()
    )

    /**
     * Issue #101's hero-sized Available Points stat makes the card taller, which can push
     * later list rows (redemption/activity history) outside Robolectric's test viewport —
     * LazyColumn only composes items actually in view, so a plain onNodeWithText can miss an
     * item that's offscreen. Scroll the list to the node first so these assertions hold
     * regardless of how much vertical space the stat card takes up.
     */
    private fun scrollToText(text: String) {
        composeTestRule.onNodeWithTag("userDetailList").performScrollToNode(hasText(text))
    }

    /** Issue #104: User Detail's stat set should match web's (Available, 7d, 30d, Redeemed, Completed). */
    @Test
    fun userDetailContent_showsRedeemedStat_andHidesExtraStatsNotOnWeb() {
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

        composeTestRule.onNodeWithText("Redeemed").assertExists()
        composeTestRule.onNodeWithText("25").assertExists()
        composeTestRule.onNodeWithText("Total points earned").assertDoesNotExist()
        composeTestRule.onNodeWithText("Skipped count").assertDoesNotExist()
    }

    /**
     * Issue #101: Available Points gets a hero/elevated treatment, distinct from every other
     * equal-weight stat. Marked as a semantic heading (see [HeroStat]), so we assert on that
     * rather than color/font-size, which aren't inspectable through Compose UI test semantics.
     */
    @Test
    fun userDetailContent_availablePoints_rendersAsHeroHeading_distinctFromOtherStats() {
        val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

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

        // The hero value ("20") is marked as a heading; the label and every other stat's value
        // (e.g. "10" for 7-day total) are not. onNode() fails if more than one match exists, so
        // this also confirms exactly one heading node is present, and it's the hero figure.
        composeTestRule.onNodeWithText("Available points").assertExists()
        composeTestRule.onNode(isHeading).assertTextEquals("20")
    }

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

    /** Issue #107: Redeem button should only render when the user has available points to redeem. */
    @Test
    fun userDetailContent_admin_hidesRedeemButton_whenAvailablePointsIsZero() {
        val dataWithNoPoints = data.copy(stats = data.stats.copy(availablePoints = 0))

        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(dataWithNoPoints),
                redeemState = UiState.Idle,
                isAdmin = true,
                onValidateAmount = { null },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Redeem Points").assertDoesNotExist()
    }

    /** Issue #107: admins with a positive balance should still see the Redeem button. */
    @Test
    fun userDetailContent_admin_showsRedeemButton_whenAvailablePointsIsPositive() {
        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(data),
                redeemState = UiState.Idle,
                isAdmin = true,
                onValidateAmount = { null },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Redeem Points").assertExists()
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

        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(redeemed == 5)
    }

    /**
     * Issue #110: redeem is a two-step confirm — tapping the first step's action ("Next") must
     * only advance to the confirm step, and the redeem call must not fire until the second,
     * explicit "Confirm" tap.
     */
    @Test
    fun userDetailContent_admin_redeemFlow_doesNotSubmit_untilSecondConfirmStep() {
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
        composeTestRule.onNodeWithText("Next").performClick()

        // First step's action only advances to the confirm step; nothing submitted yet.
        assert(redeemed == null)
        composeTestRule.onNodeWithText("Confirm Redemption").assertExists()

        composeTestRule.onNodeWithText("Confirm").performClick()
        assert(redeemed == 5)
    }

    /** Issue #110: the confirm step must show the available-points amount in color-coded text. */
    @Test
    fun userDetailContent_admin_redeemConfirmStep_showsAvailablePoints_inColorCodedText() {
        composeTestRule.setContent {
            UserDetailContent(
                uiState = UiState.Success(data),
                redeemState = UiState.Idle,
                isAdmin = true,
                onValidateAmount = { text -> if (text.toIntOrNull() in 1..20) null else "invalid" },
                onRedeem = {},
                onDismissRedeemResult = {},
                onHistoryClick = {}
            )
        }

        composeTestRule.onNodeWithText("Redeem Points").performClick()
        composeTestRule.onNodeWithText("Amount").performTextInput("5")
        composeTestRule.onNodeWithText("Next").performClick()

        // Marked with a dedicated tag (rather than semantics heading, see HeroStat) since the
        // hero stat already owns the sole heading node on this screen; actual color isn't
        // inspectable through Compose UI test semantics, so this asserts presence/content of the
        // tertiary-colored node the implementation renders (see RedeemDialog).
        composeTestRule.onNodeWithTag("redeemConfirmAvailablePoints").assertTextEquals("Available points: 20")
    }

    /** Issue #110: cancelling/backing out at the confirm step must never submit the redemption. */
    @Test
    fun userDetailContent_admin_redeemConfirmStep_back_doesNotSubmit() {
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
        composeTestRule.onNodeWithText("Next").performClick()
        composeTestRule.onNodeWithText("Back").performClick()

        // Back rewinds to the amount step (its "Next" action reappears) without submitting.
        // Note: "Redeem Points" itself is ambiguous here — it matches both the dialog's title
        // and the always-rendered button underneath it once the dialog is back on this step.
        composeTestRule.onNodeWithText("Next").assertExists()
        assert(redeemed == null)
    }

    /**
     * Issue #113: the redemption amount in each history row is emphasized (bold, accent-colored)
     * relative to the row's other fields ("by X", the date), matching web's treatment. Actual
     * font weight/color aren't inspectable through Compose UI test semantics (see HeroStat and
     * RedeemDialog's confirm-step tests for the same limitation), so this asserts on the
     * dedicated tag the implementation renders for the amount node and confirms its content,
     * while the sibling fields in the same row remain untagged/plain.
     */
    @Test
    fun userDetailContent_redemptionHistoryRow_emphasizesAmount_distinctFromOtherRowFields() {
        val redemption = Redemption(id = 1, amount = 10, redeemedBy = "alice", timestamp = "2026-07-02T22:40:54.326377Z")
        val dataWithHistory = data.copy(redemptions = listOf(redemption))

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

        scrollToText("10 pts")
        composeTestRule.onNodeWithTag("redemptionAmountEmphasis").assertTextEquals("10 pts")
        composeTestRule.onNodeWithText("by alice").assertExists()
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

        // Redemption row (appears above Chore Activity): amount -> redeemedBy -> date-only
        // formatted timestamp.
        scrollToText("10 pts")
        composeTestRule.onNodeWithText("10 pts").assertExists()
        composeTestRule.onNodeWithText("by alice").assertExists()

        // Chore Activity row: chore name -> capitalized action -> date-only formatted timestamp.
        scrollToText("Dishes")
        composeTestRule.onNodeWithText("Dishes").assertExists()
        composeTestRule.onNodeWithText("Completed").assertExists()

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

        scrollToText("not-a-timestamp")
        composeTestRule.onNodeWithText("not-a-timestamp").assertExists()
    }
}
