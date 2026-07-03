package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Issue #21 behaviors: username filter and row rendering (area: ui, android). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class AuthLogContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun authLogContent_filterByUsername_invokesCallback() {
        var filters: AuthLogFilters? = null
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(emptyList(), total = 0, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = { filters = it },
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("Username").performTextInput("alice")

        assert(filters?.username == "alice")
    }

    @Test
    fun authLogContent_rendersEntryWithChangedBy() {
        val entry = AuthLogEntry(
            id = 1,
            timestamp = "2026-07-02T22:40:54.326377Z",
            username = "bob",
            action = "password_reset",
            changedBy = "admin"
        )
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(listOf(entry), total = 1, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("password_reset").assertExists()
        composeTestRule.onNodeWithText("bob").assertExists()
        composeTestRule.onNodeWithText(formatDateTime("2026-07-02T22:40:54.326377Z")).assertExists()
        composeTestRule.onNodeWithText("Changed by: admin").assertExists()
    }

    @Test
    fun authLogContent_malformedTimestamp_fallsBackToRawString() {
        val entry = AuthLogEntry(id = 2, timestamp = "not-a-timestamp", username = "carol", action = "login_failed", changedBy = null)
        composeTestRule.setContent {
            AuthLogContent(
                uiState = UiState.Success(AuthLogPageState(listOf(entry), total = 1, page = 1)),
                filters = AuthLogFilters(),
                onFiltersChange = {},
                onNextPage = {},
                onPreviousPage = {}
            )
        }

        composeTestRule.onNodeWithText("not-a-timestamp").assertExists()
    }
}
