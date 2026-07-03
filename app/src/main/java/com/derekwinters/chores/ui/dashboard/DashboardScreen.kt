package com.derekwinters.chores.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.chores.DueWithinFilter

/**
 * Issue #12: chores-web's default screen (Board) — a grid (here, a column on phone-width
 * screens) of per-person cards with 7d/30d progress and Due Now/Due Soon deep links into Chores.
 * Issue #17: tapping a card (outside the Due Now/Due Soon buttons) opens that person's User
 * Detail screen.
 *
 * Thin Hilt-wired wrapper around [DashboardContent].
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    navActions: DashboardNavActions = DashboardNavActions(),
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    DashboardContent(modifier = modifier, uiState = uiState, navActions = navActions)
}

@Composable
fun DashboardContent(
    uiState: UiState<List<DashboardCard>>,
    navActions: DashboardNavActions,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                if (uiState.data.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.dashboard_empty)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(uiState.data, key = { it.personId }) { card ->
                            DashboardUserCard(card = card, navActions = navActions)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardUserCard(
    card: DashboardCard,
    navActions: DashboardNavActions
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { navActions.onNavigateToUserDetail(card.personId, card.username) },
        // Custom themes' surface/background colors can be very close in tone (little built-in
        // Material elevation contrast to fall back on), so use the theme's more differentiated
        // third neutral tier (surface2 -> surfaceVariant) rather than the default surface,
        // matching chores-web's own use of a more elevated tone for cards sitting on the page.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color = MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = card.initial, color = MaterialTheme.colorScheme.onPrimary)
                }
                Text(
                    text = card.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            ProgressRow(
                headerLabel = stringResource(R.string.dashboard_7_day_label),
                value = card.points7d,
                goal = card.goal7d,
                progress = progressFraction(card.points7d, card.goal7d),
                trend = card.trend7d
            )
            ProgressRow(
                headerLabel = stringResource(R.string.dashboard_30_day_label),
                value = card.points30d,
                goal = card.goal30d,
                progress = progressFraction(card.points30d, card.goal30d),
                trend = card.trend30d
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                TextButton(
                    modifier = Modifier.weight(1f).testTag("dueNowButton"),
                    onClick = { navActions.onNavigateToChores(card.username, null) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.dashboard_due_now_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = card.dueNowCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    modifier = Modifier.weight(1f).testTag("dueSoonButton"),
                    onClick = { navActions.onNavigateToChores(card.username, DueWithinFilter.NEXT_3_DAYS.name) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.dashboard_due_soon_label), style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = card.dueSoonCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(headerLabel: String, value: Int, goal: Int, progress: Float, trend: TrendStatus) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = headerLabel, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = trendColor(trend)
            )
            Text(
                text = " " + stringResource(R.string.dashboard_points_unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            color = trendColor(trend),
            trackColor = Color.Transparent
        )
        Text(
            text = stringResource(R.string.dashboard_goal_format, goal),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun trendColor(trend: TrendStatus): Color = when (trend) {
    TrendStatus.SUCCESS -> MaterialTheme.colorScheme.primary
    TrendStatus.WARNING -> Color(0xFFF9A825)
    TrendStatus.ERROR -> MaterialTheme.colorScheme.error
}

private fun progressFraction(current: Int, goal: Int): Float =
    if (goal <= 0) 0f else (current.toFloat() / goal).coerceIn(0f, 1f)
