package com.derekwinters.chores.ui.chores

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null" (area: ui, android, network) — matches CompleteWithActorModal.jsx
 * and the "Completer" term from chores-web's CONTEXT.md.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class CompleterPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun listsAllEligiblePeople() {
        composeTestRule.setContent {
            CompleterPickerDialog(people = listOf("alice", "bob", "carol"), onConfirm = {}, onDismiss = {})
        }

        composeTestRule.onNodeWithText("alice").assertExists()
        composeTestRule.onNodeWithText("bob").assertExists()
        composeTestRule.onNodeWithText("carol").assertExists()
    }

    @Test
    fun cancel_invokesOnDismiss_notOnConfirm() {
        var confirmed = false
        var dismissed = false
        composeTestRule.setContent {
            CompleterPickerDialog(
                people = listOf("alice", "bob"),
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assert(dismissed)
        assert(!confirmed)
    }

    @Test
    fun confirm_withoutChangingSelection_usesFirstPersonAsDefault() {
        var selected: String? = null
        composeTestRule.setContent {
            CompleterPickerDialog(
                people = listOf("alice", "bob"),
                onConfirm = { selected = it },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Confirm").performClick()

        assert(selected == "alice")
    }
}
