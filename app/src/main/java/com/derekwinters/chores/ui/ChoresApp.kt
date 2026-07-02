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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.derekwinters.chores.R

/**
 * Navigation destinations for the bottom nav bar (issue #2: 2 destinations, Home <-> Notification).
 */
sealed class ChoresDestination(val route: String, @StringRes val labelRes: Int) {
    data object Home : ChoresDestination("home", R.string.nav_home)
    data object Notification : ChoresDestination("notification", R.string.nav_notification)
}

private val bottomNavDestinations = listOf(ChoresDestination.Home, ChoresDestination.Notification)

/**
 * Root app composable: bottom navigation bar wiring the Home and Notification screens.
 *
 * Stateless / no ViewModel-Hilt for this bootstrap issue (see issue #2 grilling notes).
 *
 * @param onSendTestNotification invoked when the user taps "Send Test Notification" on the
 *   Notification screen. The caller (MainActivity) owns permission handling and posting.
 */
@Composable
fun ChoresApp(onSendTestNotification: () -> Unit) {
    val navController = rememberNavController()

    Scaffold(
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
                HomeScreen()
            }
            composable(ChoresDestination.Notification.route) {
                NotificationScreen(onSendTestNotification = onSendTestNotification)
            }
        }
    }
}
