package com.derekwinters.chores.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Bottom nav bar wiring Home <-> Notification screens (area: ui)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun choresApp_startsOnHomeScreen() {
        composeTestRule.setContent {
            ChoresApp(onSendTestNotification = {})
        }

        composeTestRule.onNodeWithText("Hello World").assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesToNotificationScreen() {
        composeTestRule.setContent {
            ChoresApp(onSendTestNotification = {})
        }

        composeTestRule.onNodeWithText("Notification").performClick()

        composeTestRule.onNodeWithText("Send Test Notification").assertExists()
    }

    @Test
    fun choresApp_bottomNav_navigatesBackToHomeScreen() {
        composeTestRule.setContent {
            ChoresApp(onSendTestNotification = {})
        }

        composeTestRule.onNodeWithText("Notification").performClick()
        composeTestRule.onNodeWithText("Home").performClick()

        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
