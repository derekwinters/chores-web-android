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
 * Behavior: Notification screen with "send test notification" button (area: ui)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class NotificationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notificationScreen_displaysSendTestNotificationButton() {
        composeTestRule.setContent {
            NotificationScreen(onSendTestNotification = {})
        }

        composeTestRule.onNodeWithText("Send Test Notification").assertExists()
    }

    @Test
    fun notificationScreen_tappingButton_invokesCallback() {
        var invoked = false

        composeTestRule.setContent {
            NotificationScreen(onSendTestNotification = { invoked = true })
        }

        composeTestRule.onNodeWithText("Send Test Notification").performClick()

        assert(invoked) { "Expected onSendTestNotification callback to be invoked on button tap" }
    }
}
