package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState

/**
 * Issue #14: collapsible stats panel above the Chores list. Defaults to expanded; the
 * expand/collapse state lives in this composable's own `remember` (not persisted across process
 * restarts, unlike chores-web's `localStorage["chores-stats-expanded"]" — a reasonable
 * simplification since Android doesn't have an equivalent lightweight per-screen-UI-state store
 * already wired into this app).
 *
 * Thin Hilt-wired wrapper around [ChoresStatsPanelContent].
 */
@Composable
fun ChoresStatsPanel(
    modifier: Modifier = Modifier,
    viewModel: ChoresStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    ChoresStatsPanelContent(modifier = modifier, uiState = uiState)
}

@Composable
fun ChoresStatsPanelContent(
    uiState: UiState<ChoresStats>,
    modifier: Modifier = Modifier,
    // Issue #162: collapsed by default on the chore list (previously expanded), matching the
    // mobile-web layout where stats sit below the filter row and start collapsed.
    initiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        // See DashboardScreen's per-person Card for why: custom themes' surface/background
        // colors can be too close in tone to rely on Material's default elevation contrast.
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.stats_panel_title), style = MaterialTheme.typography.titleSmall)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(
                            if (expanded) R.string.collapse_stats_panel else R.string.expand_stats_panel
                        )
                    )
                }
            }

            if (expanded) {
                when (uiState) {
                    is UiState.Success -> {
                        val stats = uiState.data
                        // Issue #75: 2x2 grid of accent-colored stat tiles, matching web's
                        // `.chore-stat-card` grid layout (previously stacked label/value rows).
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.stat_total_chores),
                                    value = stats.totalEnabledChores.toString()
                                )
                                StatTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.stat_total_points),
                                    value = stats.totalPoints.toString()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.stat_completed_7_days),
                                    value = stats.completedLast7Days.toString()
                                )
                                StatTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.stat_due_7_days),
                                    value = stats.dueNext7DaysPoints.toString()
                                )
                            }
                        }
                    }
                    is UiState.Error -> Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                    else -> Text(text = stringResource(R.string.loading))
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
