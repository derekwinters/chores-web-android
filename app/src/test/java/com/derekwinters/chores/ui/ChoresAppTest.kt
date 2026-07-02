package com.derekwinters.chores.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: bottom nav bar wiring Home <-> Notification (area: ui, issue #2) and Login screen
 * gating the Scaffold (area: ui, android, issue #5). Uses [ChoresAppContent]'s injectable
 * login/home slots so this doesn't require a Hilt test component — see LoginContentTest and
 * ChoreListContentTest for the real screens' own behavior coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun choresApp_signedOut_showsLoginContent() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = false,
                onSendTestNotification = {},
                loginContent = { Text("Fake Login") },
                homeContent = { Text("Fake Home") }
            )
        }

        composeTestRule.onNodeWithText("Fake Login").assertExists()
    }

    @Test
    fun choresApp_signedIn_startsOnHomeTab() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = true,
                onSendTestNotification = {},
                loginContent = { Text("Fake Login") },
                homeContent = { Text("Fake Home") }
            )
        }

        composeTestRule.onNodeWithText("Fake Home").assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesToNotificationScreen() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = true,
                onSendTestNotification = {},
                loginContent = { Text("Fake Login") },
                homeContent = { Text("Fake Home") }
            )
        }

        composeTestRule.onNodeWithText("Notification").performClick()

        composeTestRule.onNodeWithText("Send Test Notification").assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesBackToHomeTab() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = true,
                onSendTestNotification = {},
                loginContent = { Text("Fake Login") },
                homeContent = { Text("Fake Home") }
            )
        }

        composeTestRule.onNodeWithText("Notification").performClick()
        composeTestRule.onNodeWithText("Home").performClick()

        composeTestRule.onNodeWithText("Fake Home").assertExists()
    }
}
