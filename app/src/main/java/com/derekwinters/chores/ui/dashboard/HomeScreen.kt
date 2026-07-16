package com.derekwinters.chores.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState

/**
 * Issue #16: the Home destination is logged-in-user specific — it shows only the signed-in user's
 * own Board card (their 7d/30d trend data plus their Due Now/Due Soon counts), where the [Boards]
 * destination keeps the full per-person grid.
 *
 * Deliberately reuses [DashboardViewModel] (same people + points-summary + chores fetch and 60s
 * auto-refresh) and [DashboardContent] (same card rendering), filtering to the signed-in user at
 * this call site via [homeCards] — the same "raw data in the ViewModel, username combined in the
 * composable tree" decoupling [com.derekwinters.chores.ui.NavBadgeViewModel] uses, since the
 * signed-in username is known only to the composable tree (via CurrentUserViewModel).
 *
 * Thin Hilt-wired wrapper around [HomeContent].
 */
@Composable
fun HomeScreen(
    username: String?,
    modifier: Modifier = Modifier,
    navActions: DashboardNavActions = DashboardNavActions(),
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    HomeContent(username = username, uiState = uiState, navActions = navActions, modifier = modifier)
}

@Composable
fun HomeContent(
    username: String?,
    uiState: UiState<List<DashboardCard>>,
    navActions: DashboardNavActions,
    modifier: Modifier = Modifier
) {
    // Filter the full Board card set down to the signed-in user's single card; loading/error
    // states pass straight through to DashboardContent unchanged.
    val homeState: UiState<List<DashboardCard>> = when (uiState) {
        is UiState.Success -> UiState.Success(homeCards(uiState.data, username))
        else -> uiState
    }
    DashboardContent(
        uiState = homeState,
        navActions = navActions,
        modifier = modifier,
        emptyText = stringResource(R.string.home_empty)
    )
}
