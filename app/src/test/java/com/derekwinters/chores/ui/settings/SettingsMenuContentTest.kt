package com.derekwinters.chores.ui.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Issue #88 behavior 1: Settings destination shows a menu of rows (General, Auth, Chores, Theme,
 * Data, About) instead of one scrolling form (area: ui). Exercises [SettingsMenuContent] directly.
 *
 * Issue #167: Settings' top-level `adminOnly` gate is removed (it's now a fixed bottom-nav tab
 * visible to everyone) so admin-only content is gated at the row level instead — General, Auth,
 * Chores, Theme, and Data stay admin-only, but Preferences (the folded-in personal theme picker)
 * and About are visible to all users.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsMenuContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isAdmin: Boolean = true,
        onNavigateToGeneral: () -> Unit = {},
        onNavigateToAuth: () -> Unit = {},
        onNavigateToChores: () -> Unit = {},
        onNavigateToTheme: () -> Unit = {},
        onNavigateToData: () -> Unit = {},
        onNavigateToAbout: () -> Unit = {},
        onNavigateToPreferences: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SettingsMenuContent(
                isAdmin = isAdmin,
                onNavigateToGeneral = onNavigateToGeneral,
                onNavigateToAuth = onNavigateToAuth,
                onNavigateToChores = onNavigateToChores,
                onNavigateToTheme = onNavigateToTheme,
                onNavigateToData = onNavigateToData,
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToPreferences = onNavigateToPreferences
            )
        }
    }

    @Test
    fun settingsMenuContent_admin_displaysAllMenuRows() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithText("Preferences").assertExists()
        composeTestRule.onNodeWithText("General").assertExists()
        composeTestRule.onNodeWithText("Auth").assertExists()
        composeTestRule.onNodeWithText("Chores").assertExists()
        composeTestRule.onNodeWithText("Theme").assertExists()
        composeTestRule.onNodeWithText("Data").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun settingsMenuContent_nonAdmin_hidesAdminOnlyRowsButKeepsPreferencesAndAbout() {
        // Issue #167: Settings.adminOnly is now false (visible to all), so a non-admin can reach
        // this menu at all — but the config-mutating rows stay admin-gated at the row level.
        setContent(isAdmin = false)

        composeTestRule.onNodeWithText("Preferences").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
        composeTestRule.onNodeWithText("General").assertDoesNotExist()
        composeTestRule.onNodeWithText("Auth").assertDoesNotExist()
        composeTestRule.onNodeWithText("Chores").assertDoesNotExist()
        composeTestRule.onNodeWithText("Theme").assertDoesNotExist()
        composeTestRule.onNodeWithText("Data").assertDoesNotExist()
    }

    @Test
    fun settingsMenuContent_preferencesRowClick_invokesNavigateToPreferences() {
        var navigated = false
        setContent(isAdmin = false, onNavigateToPreferences = { navigated = true })

        composeTestRule.onNodeWithText("Preferences").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_generalRowClick_invokesNavigateToGeneral() {
        var navigated = false
        setContent(onNavigateToGeneral = { navigated = true })

        composeTestRule.onNodeWithText("General").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_authRowClick_invokesNavigateToAuth() {
        var navigated = false
        setContent(onNavigateToAuth = { navigated = true })

        composeTestRule.onNodeWithText("Auth").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_choresRowClick_invokesNavigateToChores() {
        var navigated = false
        setContent(onNavigateToChores = { navigated = true })

        composeTestRule.onNodeWithText("Chores").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_themeRowClick_invokesNavigateToTheme() {
        var navigated = false
        setContent(onNavigateToTheme = { navigated = true })

        composeTestRule.onNodeWithText("Theme").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_dataRowClick_invokesNavigateToData() {
        var navigated = false
        setContent(onNavigateToData = { navigated = true })

        composeTestRule.onNodeWithText("Data").performClick()

        assert(navigated)
    }

    @Test
    fun settingsMenuContent_aboutRowClick_invokesNavigateToAbout() {
        var navigated = false
        setContent(isAdmin = false, onNavigateToAbout = { navigated = true })

        composeTestRule.onNodeWithText("About").performClick()

        assert(navigated)
    }
}
