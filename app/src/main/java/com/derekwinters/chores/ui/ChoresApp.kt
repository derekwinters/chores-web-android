package com.derekwinters.chores.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.chores.ChoreListScreen
import com.derekwinters.chores.ui.login.LoginScreen

/**
 * Navigation destinations for the bottom nav bar (issue #2: 2 destinations, Home <-> Notification).
 */
sealed class ChoresDestination(val route: String, @StringRes val labelRes: Int) {
    data object Home : ChoresDestination("home", R.string.nav_home)
    data object Notification : ChoresDestination("notification", R.string.nav_notification)
}

private val bottomNavDestinations = listOf(ChoresDestination.Home, ChoresDestination.Notification)

/**
 * Root app composable: Login screen gates the bottom nav Scaffold wiring the Home (now: chore
 * list, issue #5) and Notification screens (issue #5 grilling: "Login screen gates the existing
 * bottom-nav Scaffold").
 *
 * @param onSendTestNotification invoked when the user taps "Send Test Notification" on the
 *   Notification screen. The caller (MainActivity) owns permission handling and posting.
 *
 * Thin Hilt-wired wrapper around [ChoresAppContent]; see ChoresAppContentTest for gating/nav
 * behavior coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoresApp(
    onSendTestNotification: () -> Unit,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isAuthenticated by sessionViewModel.isAuthenticated.collectAsState()
    ChoresAppContent(
        isAuthenticated = isAuthenticated,
        onSendTestNotification = onSendTestNotification
    )
}

/**
 * @param loginContent slot rendered while signed out; defaults to the real (Hilt-wired)
 *   [LoginScreen] but is overridable in tests to avoid needing a Hilt test component.
 * @param homeContent slot rendered on the Home tab while signed in; defaults to the real
 *   (Hilt-wired) [ChoreListScreen], overridable for the same reason.
 */
@Composable
fun ChoresAppContent(
    isAuthenticated: Boolean,
    onSendTestNotification: () -> Unit,
    modifier: Modifier = Modifier,
    loginContent: @Composable () -> Unit = { LoginScreen() },
    homeContent: @Composable () -> Unit = { ChoreListScreen() }
) {
    if (!isAuthenticated) {
        loginContent()
        return
    }

    val navController = rememberNavController()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomNavDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    ChoresDestination.Home -> Icons.Filled.Home
                                    ChoresDestination.Notification -> Icons.Filled.Notifications
                                },
                                contentDescription = label
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ChoresDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ChoresDestination.Home.route) {
                homeContent()
            }
            composable(ChoresDestination.Notification.route) {
                NotificationScreen(onSendTestNotification = onSendTestNotification)
            }
        }
    }
}
