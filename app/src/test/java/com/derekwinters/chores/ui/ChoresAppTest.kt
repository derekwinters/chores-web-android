package com.derekwinters.chores.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.derekwinters.chores.data.model.CurrentUser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Behaviors: drawer nav wiring the 5 primary destinations, admin-only Users visibility, the
 * Logout action (issue #10, area: ui), the TopAppBar's household/app title branding (issue #58,
 * area: ui), and the avatar/name identity + Preferences/Settings/Logout dropdown that replaces
 * Settings/Preferences as drawer destinations (issue #59, area: ui). Uses [ChoresAppContent]'s
 * injectable slots so this doesn't require a Hilt test component — see LoginContentTest and
 * ChoreListContentTest for the real screens' own behavior coverage.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class ChoresAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isAuthenticated: Boolean = true,
        isAdmin: Boolean = false,
        username: String = "alice",
        onLogout: () -> Unit = {},
        currentTitleProvider: @Composable () -> String? = { "Test Household" }
    ) {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = isAuthenticated,
                onLogout = onLogout,
                loginContent = { Text("Fake Login") },
                dashboardContent = { Text("Fake Dashboard") },
                choresContent = { _, _, _ -> Text("Fake Chores") },
                userDetailContent = { Text("Fake User Detail") },
                logContent = { Text("Fake Log") },
                usersContent = { Text("Fake Users") },
                settingsContent = { Text("Fake Settings") },
                preferencesContent = { Text("Fake Preferences") },
                currentUserProvider = { UiState.Success(CurrentUser(username, isAdmin)) },
                isDatabaseReadyProvider = { true },
                currentThemeProvider = { null },
                currentTitleProvider = currentTitleProvider
            )
        }
    }

    @Test
    fun choresApp_signedOut_showsLoginContent() {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = false,
                loginContent = { Text("Fake Login") },
                // Issue #62: currentThemeProvider is now invoked before the auth check too (it
                // wraps loginContent in ChoresTheme), so it needs a non-Hilt fake here same as the
                // authenticated setContent() helper below.
                currentThemeProvider = { null }
            )
        }

        composeTestRule.onNodeWithText("Fake Login").assertExists()
    }

    @Test
    fun choresApp_signedOut_loginContentIsWrappedInHouseholdTheme() {
        // Issue #62: Login (and Setup, via the same AuthGateScreen loginContent slot) now render
        // inside ChoresTheme, so they pick up the household's branded colors instead of falling
        // back to Compose's default M3 scheme even while unauthenticated.
        val theme = com.derekwinters.chores.data.model.ThemeOption(
            id = "1",
            name = "Test",
            background = "#000000",
            surface = "#111111",
            surface2 = "#222222",
            accent = "#333333",
            primary = "#4287f5",
            secondary = "#555555",
            success = "#00ff00",
            warning = "#ffff00",
            error = "#ff0000"
        )
        var observedPrimary: androidx.compose.ui.graphics.Color? = null

        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = false,
                loginContent = { observedPrimary = MaterialTheme.colorScheme.primary },
                currentThemeProvider = { theme }
            )
        }

        assertEquals(com.derekwinters.chores.ui.theme.parseHexColor("#4287f5"), observedPrimary)
    }

    @Test
    fun choresApp_signedIn_startsOnDashboard() {
        setContent()

        composeTestRule.onNodeWithText("Fake Dashboard").assertExists()
    }

    @Test
    fun choresApp_drawer_navigatesToChoresAndBackToDashboard() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Chores").performClick()
        composeTestRule.onNodeWithText("Fake Chores").assertExists()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        // Issue #60: the Dashboard destination's drawer label is "Board", matching web's PAGES copy.
        composeTestRule.onNodeWithText("Board").performClick()
        composeTestRule.onNodeWithText("Fake Dashboard").assertExists()
    }

    @Test
    fun choresApp_navPanel_hamburgerTogglesOpenAndClosedContentDescription() {
        // Issue #145: tapping the hamburger a second time (without navigating anywhere) closes
        // the panel again, and the content description reflects the open/closed state.
        setContent()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithTag("navItem_chores").assertExists()

        composeTestRule.onNodeWithContentDescription("Close navigation menu").performClick()
        composeTestRule.onNodeWithTag("navItem_chores").assertDoesNotExist()
    }

    @Test
    fun choresApp_drawer_showsAllPrimaryDestinationsWithWebLabels() {
        // Issue #60: web's PAGES order is Board -> Chores -> Users(admin) -> Log; verifies the
        // renamed "Board"/"Log" labels are present (order itself is covered by drawerDestinations'
        // declaration order, which drives ModalDrawerSheet's forEach rendering). Scoped by the
        // navItem_<route> testTag (added alongside this test) rather than plain text, since the
        // TopAppBar subtitle can legitimately show the same label text as the current drawer item
        // (e.g. both read "Board" when Dashboard/Board is the start destination).
        setContent(isAdmin = true)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithTag("navItem_dashboard").assertTextEquals("Board")
        composeTestRule.onNodeWithTag("navItem_chores").assertTextEquals("Chores")
        composeTestRule.onNodeWithTag("navItem_users").assertTextEquals("Users")
        composeTestRule.onNodeWithTag("navItem_log").assertTextEquals("Log")
    }

    @Test
    fun choresApp_nonAdmin_hidesAdminOnlyDestinations() {
        setContent(isAdmin = false)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Users").assertDoesNotExist()
    }

    @Test
    fun choresApp_admin_showsAdminOnlyDestinations() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Users").assertExists()
    }

    @Test
    fun choresApp_drawer_noLongerListsSettingsOrPreferences() {
        // Issue #59: matches web's PAGES list, which keeps Settings/Preferences out of primary
        // nav — they're reachable only via the avatar dropdown now.
        setContent(isAdmin = true)

        composeTestRule.onNodeWithContentDescription("Open navigation menu").performClick()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
        composeTestRule.onNodeWithText("Preferences").assertDoesNotExist()
    }

    @Test
    fun choresApp_topBar_showsAvatarInitialAndUserName() {
        setContent(username = "alice")

        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("alice").assertExists()
    }

    @Test
    fun choresApp_userMenu_nonAdmin_offersPreferencesAndLogoutButNotSettings() {
        setContent(isAdmin = false)

        composeTestRule.onNodeWithTag("userMenuTrigger").performClick()
        composeTestRule.onNodeWithText("Preferences").assertExists()
        composeTestRule.onNodeWithText("Logout").assertExists()
        composeTestRule.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun choresApp_userMenu_admin_offersSettings() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithTag("userMenuTrigger").performClick()
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun choresApp_userMenu_preferencesNavigatesToPreferencesScreen() {
        setContent()

        composeTestRule.onNodeWithTag("userMenuTrigger").performClick()
        composeTestRule.onNodeWithText("Preferences").performClick()
        composeTestRule.onNodeWithText("Fake Preferences").assertExists()
    }

    @Test
    fun choresApp_logout_invokesCallback() {
        var loggedOut = false
        setContent(onLogout = { loggedOut = true })

        composeTestRule.onNodeWithTag("userMenuTrigger").performClick()
        composeTestRule.onNodeWithText("Logout").performClick()

        assert(loggedOut)
    }

    @Test
    fun choresApp_topBar_showsHouseholdTitleBranding() {
        setContent(currentTitleProvider = { "The Winters House" })

        composeTestRule.onNodeWithTag("appTitleBranding").assertTextEquals("The Winters House")
    }

    @Test
    fun choresApp_topBar_fallsBackToAppNameWhenTitleNotLoaded() {
        setContent(currentTitleProvider = { null })

        composeTestRule.onNodeWithTag("appTitleBranding").assertTextEquals("Chores")
    }
}
