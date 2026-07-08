package com.derekwinters.chores.ui.chores

/**
 * Cross-screen navigation callbacks the Chores destination needs (issues #15/#16), bundled so
 * ChoresApp's composable slot signatures don't keep growing a new positional lambda parameter
 * per issue.
 *
 * Issue #180: `onNavigateToCreateChore` was removed -- Add Chore moved from this screen's own FAB
 * into the shared top app bar, which already has `navController` in scope and navigates to
 * `"chores/new"` directly, so no callback needs threading through this screen anymore.
 */
data class ChoresNavActions(
    val onNavigateToHistory: (choreName: String) -> Unit = {},
    val onNavigateToEditChore: (choreId: Int) -> Unit = {}
)
