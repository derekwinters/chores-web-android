package com.derekwinters.chores.ui.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.ui.UiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: "Login screen: server URL + username/password fields, calls POST /auth/login,
 * persists token + URL" (area: android, ui, network). Exercises [LoginContent] directly (no
 * Hilt component needed) — see LoginViewModelTest for the ViewModel-level state machine.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class LoginContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginContent_displaysAllFieldsAndTitle() {
        composeTestRule.setContent {
            LoginContent(uiState = UiState.Idle, onLogin = { _, _, _ -> })
        }

        composeTestRule.onNodeWithText("Log in to Chores").assertExists()
        composeTestRule.onNodeWithText("Server URL").assertExists()
        composeTestRule.onNodeWithText("Username").assertExists()
        composeTestRule.onNodeWithText("Password").assertExists()
        composeTestRule.onNodeWithText("Log In").assertExists()
    }

    @Test
    fun loginContent_submitsEnteredValues() {
        var captured: Triple<String, String, String>? = null
        composeTestRule.setContent {
            LoginContent(
                uiState = UiState.Idle,
                onLogin = { serverUrl, username, password -> captured = Triple(serverUrl, username, password) }
            )
        }

        composeTestRule.onNodeWithText("Server URL").performTextInput("https://chores.example.com")
        composeTestRule.onNodeWithText("Username").performTextInput("alice")
        composeTestRule.onNodeWithText("Password").performTextInput("secret")
        composeTestRule.onNodeWithText("Log In").performClick()

        assert(captured == Triple("https://chores.example.com", "alice", "secret"))
    }

    @Test
    fun loginContent_showsErrorMessage_whenUiStateIsError() {
        composeTestRule.setContent {
            LoginContent(uiState = UiState.Error("Invalid username or password"), onLogin = { _, _, _ -> })
        }

        composeTestRule.onNodeWithText("Invalid username or password").assertExists()
    }
}
