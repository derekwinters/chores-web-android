package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null". Issue #13 adds live search, filters, and sorting; issue #14 adds
 * the stats panel above this (see ChoresStatsPanel); issue #12 adds [initialAssignee]/
 * [initialDueWithin] so Dashboard's Due Now/Due Soon links land here pre-filtered; issue #15
 * adds tap-to-expand row detail plus Skip/Mark Due Now/Delete/History actions; issue #16 adds
 * the Add-chore FAB and per-row Edit action via [navActions].
 *
 * Thin Hilt-wired wrapper around [ChoreListContent]; see ChoreListContentTest for behavior
 * coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoreListScreen(
    modifier: Modifier = Modifier,
    initialAssignee: String? = null,
    initialDueWithin: String? = null,
    navActions: ChoresNavActions = ChoresNavActions(),
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val visibleState by viewModel.visibleChores.collectAsState()
    val allChoresState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val completingChoreId by viewModel.completingChoreId.collectAsState()
    val pendingActionChoreId by viewModel.pendingActionChoreId.collectAsState()
    val availablePeople by viewModel.availablePeople.collectAsState()

    LaunchedEffect(initialAssignee, initialDueWithin) {
        if (initialAssignee != null || initialDueWithin != null) {
            val dueWithin = initialDueWithin?.let { name -> runCatching { DueWithinFilter.valueOf(name) }.getOrNull() }
            viewModel.applyInitialFilters(initialAssignee, dueWithin)
        }
    }

    val allChores = (allChoresState as? UiState.Success)?.data ?: emptyList()

    ChoreListContent(
        modifier = modifier,
        statsPanel = { ChoresStatsPanel() },
        uiState = visibleState,
        totalCount = allChores.size,
        filters = filters,
        availableAssignees = allChores.availableAssigneeOptions(),
        availableScheduleTypes = allChores.availableScheduleTypes(),
        availableAssignmentTypes = allChores.availableAssignmentTypes(),
        availablePeople = availablePeople,
        completingChoreId = completingChoreId,
        pendingActionChoreId = pendingActionChoreId,
        onComplete = viewModel::completeChore,
        onSkip = viewModel::skipChore,
        onMarkDue = viewModel::markChoreDue,
        onDelete = viewModel::deleteChore,
        onHistory = { chore -> navActions.onNavigateToHistory(chore.name) },
        onEdit = { chore -> navActions.onNavigateToEditChore(chore.id) },
        onAddChore = navActions.onNavigateToCreateChore,
        onQueryChange = viewModel::updateQuery,
        onFiltersChange = viewModel::updateFilters
    )
}

@Composable
fun ChoreListContent(
    uiState: UiState<List<Chore>>,
    completingChoreId: Int?,
    onComplete: (Chore, String?) -> Unit,
    modifier: Modifier = Modifier,
    totalCount: Int = (uiState as? UiState.Success)?.data?.size ?: 0,
    filters: ChoreFilters = ChoreFilters(),
    availableAssignees: List<String> = emptyList(),
    availableScheduleTypes: List<String> = emptyList(),
    availableAssignmentTypes: List<String> = emptyList(),
    availablePeople: List<String> = emptyList(),
    pendingActionChoreId: Int? = null,
    onSkip: (Chore) -> Unit = {},
    onMarkDue: (Chore) -> Unit = {},
    onDelete: (Chore) -> Unit = {},
    onHistory: (Chore) -> Unit = {},
    onEdit: (Chore) -> Unit = {},
    onAddChore: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onFiltersChange: (ChoreFilters) -> Unit = {},
    statsPanel: @Composable () -> Unit = {}
) {
    var choreAwaitingCompleter by remember { mutableStateOf<Chore?>(null) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            statsPanel()

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = filters.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_chores_label)) },
                singleLine = true
            )
            IconButton(onClick = { showFiltersDialog = true }) {
                Icon(Icons.Filled.FilterList, contentDescription = stringResource(R.string.filters_title))
            }
        }

        if (filters.isActive) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val visibleCount = (uiState as? UiState.Success)?.data?.size ?: 0
                Text(
                    text = stringResource(R.string.showing_n_of_m_chores, visibleCount, totalCount),
                    style = MaterialTheme.typography.bodySmall
                )
                TextButton(onClick = { onFiltersChange(ChoreFilters()) }) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    Text(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = stringResource(R.string.chore_list_empty)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.data, key = { it.id }) { chore ->
                                ChoreRow(
                                    chore = chore,
                                    isCompleting = completingChoreId == chore.id,
                                    isPendingAction = pendingActionChoreId == chore.id,
                                    onCompleteClick = {
                                        if (chore.needsCompleterSelection) {
                                            choreAwaitingCompleter = chore
                                        } else {
                                            onComplete(chore, null)
                                        }
                                    },
                                    onSkip = { onSkip(chore) },
                                    onMarkDue = { onMarkDue(chore) },
                                    onDelete = { onDelete(chore) },
                                    onHistory = { onHistory(chore) },
                                    onEdit = { onEdit(chore) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        FloatingActionButton(
            onClick = onAddChore,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_chore))
        }
    }

    if (showFiltersDialog) {
        ChoreFiltersDialog(
            filters = filters,
            availableAssignees = availableAssignees,
            availableScheduleTypes = availableScheduleTypes,
            availableAssignmentTypes = availableAssignmentTypes,
            onApply = { updated ->
                onFiltersChange(updated)
                showFiltersDialog = false
            },
            onDismiss = { showFiltersDialog = false }
        )
    }

    choreAwaitingCompleter?.let { chore ->
        // An "open" chore's eligiblePeople is an optional restriction (see ChoreFormScreen's
        // "Eligible people (optional)" section) -- empty means anyone can complete it, not that
        // no one can -- so fall back to every household member when it's unset.
        CompleterPickerDialog(
            people = chore.eligiblePeople.ifEmpty { availablePeople },
            onConfirm = { completedBy ->
                onComplete(chore, completedBy)
                choreAwaitingCompleter = null
            },
            onDismiss = { choreAwaitingCompleter = null }
        )
    }
}

@Composable
private fun ChoreRow(
    chore: Chore,
    isCompleting: Boolean,
    isPendingAction: Boolean,
    onCompleteClick: () -> Unit,
    onSkip: () -> Unit,
    onMarkDue: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chore.name, style = MaterialTheme.typography.titleMedium)

            chore.currentAssignee?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }

            chore.nextDue?.let {
                Text(
                    text = stringResource(R.string.chore_next_due_format, formatNextDue(it)),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (expanded) {
                ChoreDetailSection(chore = chore)

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Start) {
                    if (isCompleting || isPendingAction) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    } else {
                        if (chore.isDue) {
                            Button(onClick = onCompleteClick) { Text(stringResource(R.string.complete_chore_button)) }
                            TextButton(onClick = onSkip) { Text(stringResource(R.string.chore_skip_action)) }
                        } else {
                            TextButton(onClick = onMarkDue) { Text(stringResource(R.string.chore_mark_due_action)) }
                        }
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.chore_edit_action)) }
                        TextButton(onClick = onHistory) { Text(stringResource(R.string.chore_history_action)) }
                        TextButton(onClick = { showDeleteConfirm = true }) { Text(stringResource(R.string.chore_delete_action)) }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.chore_delete_confirm_title)) },
            text = { Text(stringResource(R.string.chore_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text(stringResource(R.string.chore_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/**
 * Issue #31: formats a raw ISO `yyyy-MM-dd` `next_due` string as e.g. "Jul 2" (matching web's
 * `toLocaleDateString(undefined, { month: "short", day: "numeric" })`), falling back to the raw
 * string unchanged if it can't be parsed.
 */
private fun formatNextDue(raw: String): String {
    return try {
        LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
    } catch (e: DateTimeParseException) {
        raw
    }
}

@Composable
private fun ChoreDetailSection(chore: Chore) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = stringResource(R.string.chore_detail_status_format, chore.state),
            style = MaterialTheme.typography.bodySmall
        )
        chore.scheduleSummary?.let {
            Text(text = stringResource(R.string.chore_detail_frequency_format, it), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = stringResource(R.string.chore_detail_points_format, chore.points),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = stringResource(
                R.string.chore_detail_assignee_format,
                chore.currentAssignee ?: stringResource(R.string.chore_completer_label)
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
