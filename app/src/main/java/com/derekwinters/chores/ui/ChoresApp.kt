package com.derekwinters.chores.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.auth.AuthGateScreen
import com.derekwinters.chores.ui.chores.ChoreFormScreen
import com.derekwinters.chores.ui.chores.ChoreListScreen
import com.derekwinters.chores.ui.chores.ChoresNavActions
import com.derekwinters.chores.ui.common.DbReadinessGate
import com.derekwinters.chores.ui.dashboard.DashboardNavActions
import com.derekwinters.chores.ui.dashboard.DashboardScreen
import com.derekwinters.chores.ui.log.ActivityLogScreen
import com.derekwinters.chores.ui.settings.AuthLogScreen
import com.derekwinters.chores.ui.settings.DataSettingsNavActions
import com.derekwinters.chores.ui.settings.DataSettingsScreen
import com.derekwinters.chores.ui.settings.PointsLogScreen
import com.derekwinters.chores.ui.settings.SettingsAboutScreen
import com.derekwinters.chores.ui.settings.SettingsAuthScreen
import com.derekwinters.chores.ui.settings.SettingsChoresScreen
import com.derekwinters.chores.ui.settings.SettingsGeneralScreen
import com.derekwinters.chores.ui.settings.SettingsMenuContent
import com.derekwinters.chores.ui.theme.AppThemeViewModel
import com.derekwinters.chores.ui.theme.ChoresTheme
import com.derekwinters.chores.ui.theme.ThemeAdminScreen
import com.derekwinters.chores.ui.theme.ThemePreferenceScreen
import com.derekwinters.chores.ui.settings.SettingsNavActions
import com.derekwinters.chores.ui.settings.SettingsScreen
import com.derekwinters.chores.ui.users.UserDetailScreen
import com.derekwinters.chores.ui.users.UserManagementScreen

/**
 * Issue #146: Material-motion transitions replacing navigation-compose's built-in default
 * cross-fade (no enter/exitTransition means the library's implicit fade, which read as an
 * unintentional-looking flicker rather than deliberate motion).
 *
 * Top-level destination switches (Dashboard/Chores/Log/Users/Settings/Preferences) use
 * fade-through: outgoing fades out, incoming fades in with a subtle scale-up. Set as the
 * NavHost-level default.
 */
private val fadeThroughEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.92f, animationSpec = tween(300))
}
private val fadeThroughExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    fadeOut(animationSpec = tween(300))
}

/**
 * Issue #146: drill-in destinations (chore new/edit, user detail, settings sections) use
 * shared-axis horizontal motion — slide in from the right + fade, reversed on pop — set per
 * destination/graph so it overrides the NavHost-level fade-through default above.
 */
private val sharedAxisEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) +
        fadeIn(animationSpec = tween(300))
}
private val sharedAxisExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) +
        fadeOut(animationSpec = tween(300))
}
private val sharedAxisPopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) +
        fadeIn(animationSpec = tween(300))
}
private val sharedAxisPopExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) +
        fadeOut(animationSpec = tween(300))
}

/**
 * Top-level nav destinations (issue #10: "Add navigation destinations for: Dashboard, Chores,
 * Activity Log, Users, Settings, Preferences"). Chores-web's default route (`/`) is Board/
 * Dashboard, so [Dashboard] — not [Chores] — is the app's start destination now; the chore list
 * screen from issue #5 becomes the "Chores" destination unchanged.
 *
 * [adminOnly] mirrors web's `App.jsx` `adminOnly: true` nav-item gating (issue #167: only [Users]
 * still carries it at the tab level — [Settings] dropped its top-level gate since it must remain
 * reachable by non-admins for the folded-in Preferences entry; admin-only Settings content is
 * gated per-row inside `SettingsMenuContent` instead).
 *
 * Issue #167 (ADR-0004): [Preferences] is nested under the Settings sub-nav graph
 * (`"settings/preferences"`) rather than being its own top-level destination, reached only via a
 * `SettingsMenuContent` row now that the avatar dropdown no longer carries it.
 */
sealed class ChoresDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val adminOnly: Boolean = false
) {
    data object Dashboard : ChoresDestination("dashboard", R.string.nav_dashboard, Icons.Filled.Home)
    data object Chores : ChoresDestination("chores", R.string.nav_chores, Icons.Filled.CheckCircle)
    data object ActivityLog : ChoresDestination("log", R.string.nav_log, Icons.Filled.History)
    data object Users : ChoresDestination("users", R.string.nav_users, Icons.Filled.People, adminOnly = true)
    data object Settings : ChoresDestination("settings", R.string.nav_settings, Icons.Filled.Settings)
    data object Preferences : ChoresDestination("settings/preferences", R.string.nav_preferences, Icons.Filled.Palette)
}

/** Issue #12: builds the Chores route + query args for a Dashboard Due Now/Due Soon deep link. */
private fun choresRouteWithArgs(assignee: String?, dueWithin: String?): String {
    val params = listOfNotNull(
        assignee?.let { "assignee=${android.net.Uri.encode(it)}" },
        dueWithin?.let { "dueWithin=${android.net.Uri.encode(it)}" }
    )
    return if (params.isEmpty()) ChoresDestination.Chores.route else "${ChoresDestination.Chores.route}?${params.joinToString("&")}"
}

/**
 * Issue #15/#17: builds the Activity Log route + query args for a "History" deep link (Chore
 * card History action, User Detail activity feed). The Log screen itself ships in issue #19.
 */
private fun logRouteWithArgs(chore: String?, person: String?): String {
    val params = listOfNotNull(
        chore?.let { "chore=${android.net.Uri.encode(it)}" },
        person?.let { "person=${android.net.Uri.encode(it)}" }
    )
    return if (params.isEmpty()) {
        ChoresDestination.ActivityLog.route
    } else {
        "${ChoresDestination.ActivityLog.route}?${params.joinToString("&")}"
    }
}

/** Issue #17: builds the "users/{personId}" route for a Dashboard card tap / User list row tap. */
private fun userDetailRoute(personId: Int, username: String): String =
    "users/$personId?username=${android.net.Uri.encode(username)}"

/**
 * Issue #167 (ADR-0004): the five fixed bottom-nav items, in web's `PAGES`-derived order (Board/
 * Home → Chores → Users(admin) → Log) plus Settings, which — unlike issues #59/#60's
 * avatar-dropdown-only placement — is now a primary tab too (see [ChoresDestination] doc).
 */
private val bottomNavDestinations = listOf(
    ChoresDestination.Dashboard,
    ChoresDestination.Chores,
    ChoresDestination.Users,
    ChoresDestination.ActivityLog,
    ChoresDestination.Settings
)

/**
 * Root app composable: Login screen gates a bottom-nav Scaffold (issue #167/ADR-0004 returns to a
 * Material3 `NavigationBar` now that the destination count is a fixed five — see the ADR for why
 * this isn't repeating issue #10's discarded bottom-bar approach). [CurrentUserViewModel] drives
 * admin-only item visibility.
 *
 * Thin Hilt-wired wrapper around [ChoresAppContent]; see ChoresAppContentTest for gating/nav
 * behavior coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoresApp(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    ChoresAppContent(
        isAuthenticated = isAuthenticated,
        onLogout = sessionViewModel::logout
    )
}

/**
 * @param loginContent slot rendered while signed out; defaults to the real (Hilt-wired)
 *   [AuthGateScreen] but is overridable in tests to avoid needing a Hilt test component.
 * @param currentUserProvider supplies the signed-in user's admin status for nav gating;
 *   overridable in tests. Defaults to the real (Hilt-wired) [CurrentUserViewModel].
 * @param dueNowCountProvider issue #167: supplies the signed-in user's own "due now" chore count
 *   for the Chores bottom-nav tab's badge, given their username. Defaults to the real (Hilt-wired)
 *   [NavBadgeViewModel] combined with [dueNowCountForUser]; overridable in tests.
 */
@Composable
fun ChoresAppContent(
    isAuthenticated: Boolean,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
    loginContent: @Composable () -> Unit = { AuthGateScreen() },
    dashboardContent: @Composable (DashboardNavActions) -> Unit = { navActions ->
        DashboardScreen(navActions = navActions)
    },
    choresContent: @Composable (assignee: String?, dueWithin: String?, navActions: ChoresNavActions) -> Unit = { assignee, dueWithin, navActions ->
        ChoreListScreen(initialAssignee = assignee, initialDueWithin = dueWithin, navActions = navActions)
    },
    choreFormContent: @Composable (onDone: () -> Unit) -> Unit = { onDone ->
        ChoreFormScreen(onSaved = onDone, onCancel = onDone)
    },
    userDetailContent: @Composable (onNavigateToHistory: () -> Unit) -> Unit = { onNavigateToHistory ->
        UserDetailScreen(onNavigateToHistory = onNavigateToHistory)
    },
    logContent: @Composable () -> Unit = { ActivityLogScreen() },
    usersContent: @Composable (onHistoryClick: (String) -> Unit) -> Unit = { onHistoryClick ->
        UserManagementScreen(onHistoryClick = onHistoryClick)
    },
    settingsContent: @Composable (SettingsNavActions) -> Unit = { navActions -> SettingsScreen(navActions = navActions) },
    authLogContent: @Composable () -> Unit = { AuthLogScreen() },
    dataSettingsContent: @Composable (DataSettingsNavActions) -> Unit = { navActions ->
        DataSettingsScreen(navActions = navActions)
    },
    pointsLogContent: @Composable () -> Unit = { PointsLogScreen() },
    preferencesContent: @Composable () -> Unit = { ThemePreferenceScreen() },
    themeAdminContent: @Composable () -> Unit = { ThemeAdminScreen() },
    currentUserProvider: @Composable () -> UiState<CurrentUser> = {
        val viewModel: CurrentUserViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsState()
        state
    },
    isDatabaseReadyProvider: @Composable () -> Boolean = {
        val viewModel: DbReadinessViewModel = hiltViewModel()
        val ready by viewModel.isReady.collectAsState()
        ready
    },
    currentThemeProvider: @Composable () -> ThemeOption? = {
        val viewModel: AppThemeViewModel = hiltViewModel()
        val theme by viewModel.currentTheme.collectAsState()
        theme
    },
    /**
     * Issue #58: the household/app title (`config.title`) shown as serif-branded nav-shell
     * chrome, matching web's `.app-title`/`.topnav-title`. Null means "not yet loaded / fetch
     * failed" — [ChoresAuthenticatedScaffold] falls back to `R.string.app_name` in that case.
     */
    currentTitleProvider: @Composable () -> String? = {
        val viewModel: AppTitleViewModel = hiltViewModel()
        val title by viewModel.appTitle.collectAsState()
        title
    },
    dueNowCountProvider: @Composable (username: String?) -> Int = { username ->
        val viewModel: NavBadgeViewModel = hiltViewModel()
        val chores by viewModel.chores.collectAsState()
        dueNowCountForUser(chores, username)
    }
) {
    // Issue #62: ChoresTheme now wraps the unauthenticated screen graph (AuthGateScreen's
    // server-check/Login/Setup states) too, not just the post-auth scaffold, so Login and Setup
    // pick up the household's branded colors/typography instead of Compose's default M3 scheme.
    // currentThemeProvider's AppThemeViewModel already treats a failed/unauthenticated theme
    // fetch as null (ChoresTheme's hardcoded-fallback case), so this is a safe no-regression
    // superset of the previous post-auth-only wrapping.
    val currentTheme = currentThemeProvider()

    ChoresTheme(themeOption = currentTheme) {
        if (!isAuthenticated) {
            loginContent()
            return@ChoresTheme
        }

        val isDatabaseReady = isDatabaseReadyProvider()

        DbReadinessGate(isReady = isDatabaseReady, modifier = modifier) {
            val currentUserState = currentUserProvider()
            val isAdmin = (currentUserState as? UiState.Success)?.data?.isAdmin == true
            val username = (currentUserState as? UiState.Success)?.data?.username
            val appTitle = currentTitleProvider()
            val dueNowCount = dueNowCountProvider(username)

            ChoresAuthenticatedScaffold(
                isAdmin = isAdmin,
                username = username,
                onLogout = onLogout,
                appTitle = appTitle,
                dueNowCount = dueNowCount,
                dashboardContent = dashboardContent,
                choresContent = choresContent,
                choreFormContent = choreFormContent,
                userDetailContent = userDetailContent,
                logContent = logContent,
                usersContent = usersContent,
                settingsContent = settingsContent,
                authLogContent = authLogContent,
                dataSettingsContent = dataSettingsContent,
                pointsLogContent = pointsLogContent,
                preferencesContent = preferencesContent,
                themeAdminContent = themeAdminContent
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoresAuthenticatedScaffold(
    isAdmin: Boolean,
    /** Issue #59: shown as the avatar initial + name in the top bar's user menu. */
    username: String?,
    onLogout: () -> Unit,
    /** Issue #58: household/app title branding; null falls back to `R.string.app_name`. */
    appTitle: String?,
    /** Issue #167: the signed-in user's own "due now" chore count, shown as a badge on the Chores tab. */
    dueNowCount: Int,
    modifier: Modifier = Modifier,
    dashboardContent: @Composable (DashboardNavActions) -> Unit,
    choresContent: @Composable (assignee: String?, dueWithin: String?, navActions: ChoresNavActions) -> Unit,
    choreFormContent: @Composable (onDone: () -> Unit) -> Unit,
    userDetailContent: @Composable (onNavigateToHistory: () -> Unit) -> Unit,
    logContent: @Composable () -> Unit,
    usersContent: @Composable (onHistoryClick: (String) -> Unit) -> Unit,
    settingsContent: @Composable (SettingsNavActions) -> Unit,
    authLogContent: @Composable () -> Unit,
    dataSettingsContent: @Composable (DataSettingsNavActions) -> Unit,
    pointsLogContent: @Composable () -> Unit,
    preferencesContent: @Composable () -> Unit,
    themeAdminContent: @Composable () -> Unit
) {
    val navController = rememberNavController()
    var userMenuExpanded by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    // Issue #167: Settings.adminOnly is now false, so it's always included; Users stays gated.
    val visibleDestinations = bottomNavDestinations.filter { !it.adminOnly || isAdmin }
    // Chores' actual registered route carries query-arg placeholders ("chores?assignee=...");
    // matching by prefix keeps bottom-nav highlighting/title working whether or not args are
    // present, and (issue #167) also keeps the Settings tab highlighted while drilled into any
    // "settings/*" sub-route, including the folded-in "settings/preferences" screen.
    fun isCurrent(dest: ChoresDestination) =
        currentDestination?.hierarchy?.any { it.route?.startsWith(dest.route) == true } == true
    val currentLabel = when {
        // Issue #98: the page title reads "Manage Users" (web parity) while the bottom-nav item
        // keeps the shorter "Users" label — these are intentionally decoupled.
        isCurrent(ChoresDestination.Users) -> stringResource(R.string.user_management_screen_title)
        // Issue #167: Preferences now lives under Settings ("settings/preferences"), which also
        // satisfies isCurrent(Settings) since it starts with "settings" -- check the more specific
        // Preferences match first so the subtitle reads "Preferences" rather than "Settings" there.
        isCurrent(ChoresDestination.Preferences) -> stringResource(R.string.nav_preferences)
        else -> visibleDestinations.firstOrNull(::isCurrent)
            ?.labelRes
            ?.let { stringResource(it) }
            ?: stringResource(R.string.app_name)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Issue #58: household/app title branding, matching web's
                        // `.app-title`/`.topnav-title` (Playfair Display serif, 1.3rem/700/-0.5px).
                        Text(
                            text = appTitle ?: stringResource(R.string.app_name),
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.8.sp,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.testTag("appTitleBranding")
                        )
                        Text(currentLabel, style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    // Issue #59: restores web's user identity treatment (`UserAvatarMenu.jsx`
                    // lines 45-51) — a colored circle with the user's initial plus their name —
                    // in place of the generic MoreVert icon. Issue #167: the dropdown itself now
                    // only offers identity + logout, since Preferences/Settings moved to the
                    // bottom nav.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("userMenuTrigger")
                            .clickable { userMenuExpanded = true }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(
                                text = username?.take(1)?.uppercase().orEmpty(),
                                color = MaterialTheme.colorScheme.onTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (username != null) {
                            Text(
                                text = username,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    DropdownMenu(expanded = userMenuExpanded, onDismissRequest = { userMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout)) },
                            leadingIcon = { Icon(Icons.Filled.Logout, contentDescription = null) },
                            onClick = {
                                userMenuExpanded = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            // Issue #167 (ADR-0004): Material3 NavigationBar replaces the hamburger + expand-
            // under-the-top-bar panel (issue #145) as primary navigation.
            NavigationBar {
                visibleDestinations.forEach { destination ->
                    val selected = isCurrent(destination)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(ChoresDestination.Dashboard.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (destination == ChoresDestination.Chores && dueNowCount > 0) {
                                // stringResource is a @Composable call, so it must be resolved here
                                // (composable scope) rather than inside the Modifier.semantics{}
                                // lambda below, which is a plain (non-composable) lambda.
                                val badgeDescription = stringResource(R.string.chores_due_now_badge_description, dueNowCount)
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            modifier = Modifier
                                                .testTag("choresDueNowBadge")
                                                .semantics { contentDescription = badgeDescription }
                                        ) {
                                            Text(dueNowCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(destination.icon, contentDescription = null)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        modifier = Modifier.testTag("navItem_${destination.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ChoresDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = fadeThroughEnter,
            exitTransition = fadeThroughExit,
            popEnterTransition = fadeThroughEnter,
            popExitTransition = fadeThroughExit
        ) {
            composable(ChoresDestination.Dashboard.route) {
                dashboardContent(
                    DashboardNavActions(
                        onNavigateToChores = { assignee, dueWithin ->
                            navController.navigate(choresRouteWithArgs(assignee, dueWithin))
                        },
                        onNavigateToUserDetail = { personId, username ->
                            navController.navigate(userDetailRoute(personId, username))
                        }
                    )
                )
            }
            composable(
                route = "${ChoresDestination.Chores.route}?assignee={assignee}&dueWithin={dueWithin}",
                arguments = listOf(
                    navArgument("assignee") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("dueWithin") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                choresContent(
                    backStackEntry.arguments?.getString("assignee"),
                    backStackEntry.arguments?.getString("dueWithin"),
                    ChoresNavActions(
                        onNavigateToHistory = { choreName ->
                            navController.navigate(logRouteWithArgs(chore = choreName, person = null))
                        },
                        onNavigateToCreateChore = { navController.navigate("chores/new") },
                        onNavigateToEditChore = { choreId -> navController.navigate("chores/$choreId/edit") }
                    )
                )
            }
            composable(
                route = "chores/new",
                enterTransition = sharedAxisEnter,
                exitTransition = sharedAxisExit,
                popEnterTransition = sharedAxisPopEnter,
                popExitTransition = sharedAxisPopExit
            ) {
                choreFormContent { navController.popBackStack() }
            }
            composable(
                route = "chores/{choreId}/edit",
                arguments = listOf(navArgument("choreId") { type = NavType.IntType }),
                enterTransition = sharedAxisEnter,
                exitTransition = sharedAxisExit,
                popEnterTransition = sharedAxisPopEnter,
                popExitTransition = sharedAxisPopExit
            ) {
                choreFormContent { navController.popBackStack() }
            }
            composable(
                route = "${ChoresDestination.ActivityLog.route}?chore={chore}&person={person}",
                arguments = listOf(
                    navArgument("chore") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("person") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { logContent() }
            composable(
                route = "users/{personId}?username={username}",
                arguments = listOf(
                    navArgument("personId") { type = NavType.IntType },
                    navArgument("username") { type = NavType.StringType; nullable = true; defaultValue = null }
                ),
                enterTransition = sharedAxisEnter,
                exitTransition = sharedAxisExit,
                popEnterTransition = sharedAxisPopEnter,
                popExitTransition = sharedAxisPopExit
            ) { backStackEntry ->
                val username = backStackEntry.arguments?.getString("username")
                userDetailContent {
                    navController.navigate(logRouteWithArgs(chore = null, person = username))
                }
            }
            composable(ChoresDestination.Users.route) {
                usersContent { username -> navController.navigate(logRouteWithArgs(chore = null, person = username)) }
            }
            // Issue #146: navigation-compose 2.7.7's `navigation(...)` graph builder doesn't
            // accept enter/exitTransition params (added in a later library version), so the
            // shared-axis drill-in transition is set per-composable within this graph instead
            // of once on the graph itself.
            navigation(startDestination = "settings/menu", route = ChoresDestination.Settings.route) {
                composable(
                    route = "settings/menu",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    SettingsMenuContent(
                        isAdmin = isAdmin,
                        onNavigateToGeneral = { navController.navigate("settings/general") },
                        onNavigateToAuth = { navController.navigate("settings/auth") },
                        onNavigateToChores = { navController.navigate("settings/chores") },
                        onNavigateToTheme = { navController.navigate("settings/theme") },
                        onNavigateToData = { navController.navigate("settings/data") },
                        onNavigateToAbout = { navController.navigate("settings/about") },
                        onNavigateToPreferences = { navController.navigate(ChoresDestination.Preferences.route) }
                    )
                }
                composable(
                    route = "settings/general",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    SettingsGeneralScreen(navController = navController)
                }
                composable(
                    route = "settings/auth",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    SettingsAuthScreen(
                        navController = navController,
                        onNavigateToAuthLog = { navController.navigate("settings/authLog") }
                    )
                }
                composable(
                    route = "settings/chores",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    SettingsChoresScreen(
                        navController = navController,
                        onNavigateToData = { navController.navigate("settings/data") }
                    )
                }
                composable(
                    route = "settings/about",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    SettingsAboutScreen(navController = navController)
                }
                composable(
                    route = "settings/authLog",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) { authLogContent() }
                composable(
                    route = "settings/theme",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) { themeAdminContent() }
                composable(
                    route = "settings/data",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) {
                    dataSettingsContent(DataSettingsNavActions(onNavigateToPointsLog = { navController.navigate("settings/data/pointsLog") }))
                }
                composable(
                    route = "settings/data/pointsLog",
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) { pointsLogContent() }
                // Issue #167: Preferences (the personal theme picker) folds into the Settings
                // sub-nav graph instead of being reachable only via the avatar dropdown.
                composable(
                    route = ChoresDestination.Preferences.route,
                    enterTransition = sharedAxisEnter,
                    exitTransition = sharedAxisExit,
                    popEnterTransition = sharedAxisPopEnter,
                    popExitTransition = sharedAxisPopExit
                ) { preferencesContent() }
            }
        }
    }
}
