package com.derekwinters.chores.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
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
 * Issue #167 (ADR-0004) behaviors: a Material3 `NavigationBar` in `Scaffold.bottomBar` replaces
 * the hamburger + expand-under-top-bar panel (issue #145) as primary navigation, the Dashboard/
 * Board destination is relabeled "Home", Settings becomes visible to all users (admin-only
 * content gated inside `SettingsMenuContent` instead — see SettingsMenuContentTest), Preferences
 * folds into the Settings sub-nav graph, the avatar dropdown shrinks to identity + logout, and the
 * Chores tab carries a numeric due-now badge. Uses [ChoresAppContent]'s injectable slots so this
 * doesn't require a Hilt test component — see LoginContentTest and ChoreListContentTest for the
 * real screens' own behavior coverage.
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
        dueNowCount: Int = 0,
        currentTitleProvider: @Composable () -> String? = { "Test Household" },
        // Issue #180: real ChoreFormScreen is Hilt-wired, so tests exercising the new top-bar
        // Add-Chore action's navigation need a fake here, same as every other content slot.
        choreFormContent: @Composable (onDone: () -> Unit) -> Unit = { Text("Fake Chore Form") }
    ) {
        composeTestRule.setContent {
            ChoresAppContent(
                isAuthenticated = isAuthenticated,
                onLogout = onLogout,
                loginContent = { Text("Fake Login") },
                dashboardContent = { Text("Fake Dashboard") },
                choresContent = { _, _, _ -> Text("Fake Chores") },
                choreFormContent = choreFormContent,
                userDetailContent = { Text("Fake User Detail") },
                logContent = { Text("Fake Log") },
                usersContent = { Text("Fake Users") },
                settingsContent = { Text("Fake Settings") },
                preferencesContent = { Text("Fake Preferences") },
                currentUserProvider = { UiState.Success(CurrentUser(username, isAdmin)) },
                isDatabaseReadyProvider = { true },
                currentThemeProvider = { null },
                currentTitleProvider = currentTitleProvider,
                dueNowCountProvider = { dueNowCount }
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
    fun choresApp_bottomNav_navigatesToChoresAndBackToHome() {
        setContent()

        composeTestRule.onNodeWithTag("navItem_chores").performClick()
        composeTestRule.onNodeWithText("Fake Chores").assertExists()

        composeTestRule.onNodeWithTag("navItem_dashboard").performClick()
        composeTestRule.onNodeWithText("Fake Dashboard").assertExists()
    }

    @Test
    fun choresApp_hamburgerMenu_noLongerExists() {
        // Issue #167: the hamburger toggle + expand-under-top-bar panel (issue #145) are removed
        // entirely in favor of the bottom NavigationBar, not left dormant.
        setContent()

        composeTestRule.onNodeWithContentDescription("Open navigation menu").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close navigation menu").assertDoesNotExist()
    }

    @Test
    fun choresApp_bottomNav_dashboardLabelIsHome() {
        // Issue #167: "Board" -> "Home" (route unchanged), an intentional divergence from web's copy.
        setContent()

        composeTestRule.onNodeWithTag("navItem_dashboard").assertTextEquals("Home")
    }

    @Test
    fun choresApp_bottomNav_admin_showsAllFiveDestinationsWithLabels() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithTag("navItem_dashboard").assertTextEquals("Home")
        composeTestRule.onNodeWithTag("navItem_chores").assertTextEquals("Chores")
        composeTestRule.onNodeWithTag("navItem_users").assertTextEquals("Users")
        composeTestRule.onNodeWithTag("navItem_log").assertTextEquals("Log")
        composeTestRule.onNodeWithTag("navItem_settings").assertTextEquals("Settings")
    }

    @Test
    fun choresApp_bottomNav_nonAdmin_hidesUsersButKeepsSettingsVisible() {
        // Issue #167: Settings loses its admin-only gate (now reachable by everyone for the
        // folded-in Preferences entry); Users remains admin-only and hidden entirely -- non-admins
        // see 3 of 5 tabs (Home, Chores, Log), which is intentional, not a bug.
        setContent(isAdmin = false)

        composeTestRule.onNodeWithTag("navItem_users").assertDoesNotExist()
        composeTestRule.onNodeWithTag("navItem_settings").assertExists()
        composeTestRule.onNodeWithTag("navItem_dashboard").assertExists()
        composeTestRule.onNodeWithTag("navItem_chores").assertExists()
        composeTestRule.onNodeWithTag("navItem_log").assertExists()
    }

    @Test
    fun choresApp_bottomNav_admin_showsUsersTab() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithTag("navItem_users").assertExists()
    }

    @Test
    fun choresApp_bottomNav_homeTabIsSelectedOnStart() {
        setContent()

        composeTestRule.onNodeWithTag("navItem_dashboard").assertIsSelected()
    }

    @Test
    fun choresApp_choresTab_noDueChores_showsNoBadge() {
        setContent(dueNowCount = 0)

        composeTestRule.onNodeWithTag("choresDueNowBadge").assertDoesNotExist()
    }

    @Test
    fun choresApp_choresTab_dueChores_showsBadgeWithCount() {
        // Issue #167: numeric Badge on the Chores tab shows the signed-in user's own due-now count.
        setContent(dueNowCount = 3)

        composeTestRule.onNodeWithTag("choresDueNowBadge", useUnmergedTree = true).assertExists()
        // useUnmergedTree disables all merging, so the Badge's own text lives on its Text child
        // rather than on the tagged node itself.
        composeTestRule.onNodeWithTag("choresDueNowBadge", useUnmergedTree = true)
            .onChildren()[0]
            .assertTextEquals("3")
    }

    @Test
    fun choresApp_settingsTab_nonAdmin_showsPreferencesButNotAdminOnlyRows() {
        // Issue #167: navigating to the Settings tab renders the real SettingsMenuContent (not an
        // injectable fake), so this exercises the ChoresApp <-> SettingsMenuContent isAdmin wiring
        // end-to-end; row-level gating detail itself is covered by SettingsMenuContentTest.
        setContent(isAdmin = false)

        composeTestRule.onNodeWithTag("navItem_settings").performClick()

        composeTestRule.onNodeWithText("Preferences").assertExists()
        composeTestRule.onNodeWithText("General").assertDoesNotExist()
    }

    @Test
    fun choresApp_settingsTab_admin_showsAdminOnlyRows() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithTag("navItem_settings").performClick()

        composeTestRule.onNodeWithText("General").assertExists()
    }

    @Test
    fun choresApp_preferencesReachableViaSettingsMenu_keepsSettingsTabHighlighted() {
        // Issue #167 behavior 4/10: Preferences is folded into the Settings menu (not the avatar
        // dropdown), and the existing isCurrent() hierarchy-prefix match keeps the Settings tab
        // highlighted while drilled into "settings/preferences".
        setContent()

        composeTestRule.onNodeWithTag("navItem_settings").performClick()
        composeTestRule.onNodeWithText("Preferences").performClick()

        composeTestRule.onNodeWithText("Fake Preferences").assertExists()
        composeTestRule.onNodeWithTag("navItem_settings").assertIsSelected()
        composeTestRule.onNodeWithText("Preferences").assertExists()
    }

    @Test
    fun choresApp_userMenu_offersSignedInAsHeaderAndLogoutOnly() {
        // Issue #167: the avatar dropdown shrinks to identity + logout only -- Preferences and
        // Settings are reachable via the bottom nav now. "Settings" text still exists once (the
        // bottom-nav tab label), so this asserts the dropdown doesn't add a second occurrence
        // rather than asserting global absence. Issue #180: identity is now the "Signed in as"
        // header (the username moved out of the always-visible top bar into here).
        setContent(isAdmin = true, username = "alice")

        composeTestRule.onNodeWithTag("userMenuTrigger").performClick()

        composeTestRule.onNodeWithText("Signed in as alice").assertExists()
        composeTestRule.onNodeWithText("Logout").assertExists()
        composeTestRule.onNodeWithText("Preferences").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Settings").assertCountEquals(1)
    }

    @Test
    fun choresApp_topBar_showsAvatarInitialOnly_usernameMovedIntoDropdown() {
        // Issue #180: navigationIcon is sized for a single compact icon, not icon+text -- the
        // avatar circle alone is now always visible in the top bar; the username itself only
        // appears once the dropdown is opened (see choresApp_userMenu_offersSignedInAsHeaderAndLogoutOnly).
        setContent(username = "alice")

        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("alice").assertDoesNotExist()
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

    @Test
    fun choresApp_topBar_addChoreAction_hiddenOnDashboard() {
        // Issue #180: the top bar is shared across every screen, so the Add-Chore action must not
        // render on the Dashboard start destination.
        setContent()

        composeTestRule.onNodeWithContentDescription("Add Chore").assertDoesNotExist()
    }

    @Test
    fun choresApp_topBar_addChoreAction_visibleOnChoresScreen() {
        setContent()

        composeTestRule.onNodeWithTag("navItem_chores").performClick()

        composeTestRule.onNodeWithContentDescription("Add Chore").assertExists()
    }

    @Test
    fun choresApp_topBar_addChoreAction_navigatesDirectlyToChoreForm() {
        // Issue #180: navController is already in scope in ChoresAuthenticatedScaffold, so the
        // top-bar plus icon calls navController.navigate("chores/new") directly -- no callback
        // threading through ChoresNavActions.
        setContent()

        composeTestRule.onNodeWithTag("navItem_chores").performClick()
        composeTestRule.onNodeWithContentDescription("Add Chore").performClick()

        composeTestRule.onNodeWithText("Fake Chore Form").assertExists()
    }

    @Test
    fun choresApp_topBar_addChoreAction_hiddenOnChoreFormScreenItself() {
        // Regression test for the visibility bug caught during grilling: the existing isCurrent()
        // prefix-match helper (used for bottom-nav highlighting) would incorrectly also match
        // "chores/new" since it starts with "chores" -- the Add-Chore action must use an exact
        // route match instead, so it disappears once already on the create-chore screen.
        setContent()

        composeTestRule.onNodeWithTag("navItem_chores").performClick()
        composeTestRule.onNodeWithContentDescription("Add Chore").performClick()

        composeTestRule.onNodeWithContentDescription("Add Chore").assertDoesNotExist()
    }

    @Test
    fun choresApp_topBar_addChoreAction_hiddenOnOtherTopLevelScreens() {
        setContent(isAdmin = true)

        composeTestRule.onNodeWithTag("navItem_users").performClick()
        composeTestRule.onNodeWithContentDescription("Add Chore").assertDoesNotExist()

        composeTestRule.onNodeWithTag("navItem_settings").performClick()
        composeTestRule.onNodeWithContentDescription("Add Chore").assertDoesNotExist()
    }
}
