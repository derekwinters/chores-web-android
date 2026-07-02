package com.derekwinters.chores.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behavior: Home screen showing "Hello World" (area: ui)
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysHelloWorldGreeting() {
        composeTestRule.setContent {
            HomeScreen()
        }

        composeTestRule.onNodeWithText("Hello World").assertExists()
    }
}
