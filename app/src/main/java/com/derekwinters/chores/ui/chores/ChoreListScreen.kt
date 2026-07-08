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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.theme.LocalThemeOption
import com.derekwinters.chores.ui.theme.parseHexColor
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
 * adds tap-to-expand row detail plus Skip/Mark Due Now/Delete/History actions and per-row Edit
 * action via [navActions]. Issue #180 removes the Add-chore FAB issue #16/#70 added -- Add Chore
 * now lives in the shared top app bar (see `ChoresApp.kt`'s `ChoresAuthenticatedScaffold`)
 * instead, which also removes the FAB/list-content overlap problem investigated under #177.
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
    onQueryChange: (String) -> Unit = {},
    onFiltersChange: (ChoreFilters) -> Unit = {},
    statsPanel: @Composable () -> Unit = {}
) {
    var choreAwaitingCompleter by remember { mutableStateOf<Chore?>(null) }
    // Issue #74 CI fix: hoisted up from ChoreRow (a LazyColumn item composable) to match every
    // other delete-confirmation dialog in this codebase (PointsLog, ThemeAdmin) and this same
    // file's own CompleterPickerDialog below -- all of which keep their "awaiting confirmation"
    // state as a sibling of the list, not owned by a list item itself. Owning it inside the
    // LazyColumn item was the actual root cause of choreListContent_deleteAction_requiresConfirmation
    // intermittently failing to find the dialog after the list's layout changed (issue #74):
    // list-item-owned state is subject to that item's own composition lifecycle, which isn't
    // guaranteed stable in the same way a parent-level remember is.
    var choreAwaitingDelete by remember { mutableStateOf<Chore?>(null) }
    var showFiltersDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Issue #180: search is now a collapsible icon inside ChoreFilterIconRow itself (carried
        // forward from #177's validated design) instead of an always-visible field above it -- see
        // ChoreFilterIconRow for the expand/collapse behavior.
        // Issue #162: compact row of flat filter icon buttons (Assignee, Status, Due-within) plus
        // an overflow "Tune" icon for the remaining groups (schedule type, assignment type,
        // enabled status) -- replaces the single "Filters" text button, matching mobile web's top
        // filter icon row.
        ChoreFilterIconRow(
            filters = filters,
            availableAssignees = availableAssignees,
            onFiltersChange = onFiltersChange,
            onQueryChange = onQueryChange,
            onMoreFiltersClick = { showFiltersDialog = true }
        )

        // Issue #162: stats moved below the filter row and collapsed by default (see
        // ChoresStatsPanelContent's initiallyExpanded default), matching mobile web's layout.
        statsPanel()

        // Issue #74: "Showing N of M chores" is now always visible (previously hidden unless
        // filters were active), matching web's always-visible count. "Clear filters" remains
        // conditional -- it wouldn't make sense to offer clearing filters that aren't active.
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
            if (filters.isActive) {
                TextButton(onClick = { onFiltersChange(ChoreFilters()) }) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
        }

        // Issue #180: Add Chore moved to the top app bar and the FAB was removed entirely, which
        // also removes the FAB/list-content overlap problem investigated under #177 by
        // construction -- this Box no longer needs the fixed-footprint bottom padding issue #74
        // added to keep the last row clear of the FAB.
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
                                    onDeleteClick = { choreAwaitingDelete = chore },
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

    choreAwaitingDelete?.let { chore ->
        AlertDialog(
            onDismissRequest = { choreAwaitingDelete = null },
            title = { Text(stringResource(R.string.chore_delete_confirm_title)) },
            text = { Text(stringResource(R.string.chore_delete_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    choreAwaitingDelete = null
                    onDelete(chore)
                }) { Text(stringResource(R.string.chore_delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { choreAwaitingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
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
    onDeleteClick: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expanded = !expanded }
    ) {
        // Issue #67: colored left accent bar + due-date color coding on the chore row, matching
        // ChoreCard.css's `.accent-bar` rules (red while due, muted gray once complete, no bar
        // otherwise). Uses drawBehind on this Column (not a sibling Box/IntrinsicSize.Min) since
        // that's a draw-time-only effect against the already-resolved layout size, with no
        // interaction with layout/semantics/click routing.
        val accentColor = statusAccentColor(chore)
        Column(
            modifier = Modifier
                .drawBehind {
                    if (accentColor != null) {
                        drawRect(color = accentColor, size = Size(4.dp.toPx(), size.height))
                    }
                }
                .padding(
                    start = if (accentColor != null) 20.dp else 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp
                )
        ) {
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

                // Issue #162: simple flat icon buttons instead of text chips, matching mobile
                // web's icon-button treatment -- not full-color emoji icons, plain Material
                // symbols with a contentDescription each (existing show/hide logic unchanged).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCompleting || isPendingAction) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                    } else {
                        if (chore.isDue) {
                            // Issue #93/#162: success/green tint, sourced from the theme's
                            // success color (no first-class Material3 ColorScheme slot -- same
                            // LocalThemeOption pattern as Dashboard's trend coloring, issue #120),
                            // falling back to colorScheme.primary if no theme has resolved yet.
                            val themeOption = LocalThemeOption.current
                            ChoreActionIconButton(
                                onClick = onCompleteClick,
                                icon = Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.complete_chore_button),
                                tint = themeOption?.success?.let(::parseHexColor) ?: MaterialTheme.colorScheme.primary
                            )
                            ChoreActionIconButton(
                                onClick = onSkip,
                                icon = Icons.Filled.SkipNext,
                                contentDescription = stringResource(R.string.chore_skip_action)
                            )
                        } else {
                            ChoreActionIconButton(
                                onClick = onMarkDue,
                                icon = Icons.Filled.NotificationsActive,
                                contentDescription = stringResource(R.string.chore_mark_due_action)
                            )
                        }
                        ChoreActionIconButton(
                            onClick = onEdit,
                            icon = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.chore_edit_action)
                        )
                        ChoreActionIconButton(
                            onClick = onHistory,
                            icon = Icons.Filled.History,
                            contentDescription = stringResource(R.string.chore_history_action)
                        )
                        // Issue #93/#162: red/error tint, same LocalThemeOption pattern as
                        // Complete. This is the one button whose onClick mutates ChoreRow's own
                        // local `remember` state to conditionally render an AlertDialog -- the
                        // exact shape that broke every prior attempt at this file, so it stays
                        // isolated per the issue #93 rollout plan.
                        val deleteThemeOption = LocalThemeOption.current
                        ChoreActionIconButton(
                            onClick = onDeleteClick,
                            icon = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.chore_delete_action),
                            tint = deleteThemeOption?.error?.let(::parseHexColor) ?: MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Issue #162: simple flat icon button for the expanded row's action row -- matches
 * `ChoreCard.css`'s `.action-btn` in spirit (equal-weight action affordances) but as a plain
 * Material icon rather than a text chip. Default tint is onSurfaceVariant; Complete/Delete pass
 * their semantic success/error [tint]. Every icon carries a [contentDescription] for
 * accessibility -- there is no visible text.
 */
@Composable
private fun ChoreActionIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * Issue #67/#80: matches `ChoreCard.css`'s `.accent-bar` rules exactly -- red while due, muted
 * gray once complete (a distinct `state` value from "due"/"not_due"), no bar otherwise.
 */
@Composable
private fun statusAccentColor(chore: Chore): Color? = when {
    chore.isDue -> MaterialTheme.colorScheme.error
    chore.state == "complete" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> null
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

/**
 * Issue #87: 2-column label/value grid (Status/Frequency, then Points/Assignee) framed by
 * dividers, matching `ChoreCard.css`'s `.expanded-meta`/`.meta-item`/`.meta-label`/`.meta-value`
 * -- replaces the single-column "Label: value" text stack. Deliberately uses
 * `Modifier.fillMaxWidth(0.5f)` per cell rather than `Modifier.weight(1f)` (see this file's/
 * issue #93's notes on this Row's history of unrelated Compose-testing fragility) -- same visual
 * result, without engaging `Row`'s weight-distribution machinery.
 */
@Composable
private fun ChoreDetailSection(chore: Chore) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        HorizontalDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_status_label),
                value = chore.state
            )
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_frequency_label),
                value = chore.scheduleSummary ?: "—"
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_points_label),
                value = chore.points.toString()
            )
            ChoreMetaItem(
                modifier = Modifier.fillMaxWidth(0.5f),
                label = stringResource(R.string.chore_detail_assignee_label),
                // Issue #162: the mobile app was showing the static "Completer" label for every
                // open/rotating chore -- "Completer" is a completion-time concept, not an
                // assignee value. Now: currentAssignee when set; else next-in-rotation for
                // rotating chores; else "Anyone" for open chores.
                value = chore.currentAssignee
                    ?: chore.nextAssignee?.takeIf { chore.assignmentType == "rotating" }
                    ?: stringResource(R.string.chore_assignee_anyone_label)
            )
        }
        HorizontalDivider()
    }
}

/**
 * Issue #87: one 2-column grid cell -- an uppercase muted label over its value, matching
 * `ChoreCard.css`'s `.meta-label`/`.meta-value`.
 */
@Composable
private fun ChoreMetaItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Issue #162: compact row of flat icon buttons at the top of the chore list content, one per
 * filter group (Assignee, State, Due-within) plus an overflow "Tune" icon opening the existing
 * [ChoreFiltersDialog] for the remaining groups (enabled status, schedule type, assignment type).
 * A group's icon is tinted primary when that group holds a non-default value, so active filters
 * are visible at a glance without opening any menu. The underlying [ChoreFilters] model and
 * [onFiltersChange] contract are unchanged -- this only changes how filters are edited.
 *
 * Issue #180 (carried forward from #177's validated design): search is a collapsible icon --
 * the first entry in this row -- rather than an always-visible field above it. Tapping it morphs
 * the whole row into a full-width text field + back/collapse icon, hiding the other filter icons
 * while expanded; collapsing preserves the query (the filter stays active, surfaced via the
 * search icon's own active badge once collapsed again); the field starts expanded already if a
 * query is active on first composition (e.g. returning to the screen with a filter carried over).
 */
@Composable
private fun ChoreFilterIconRow(
    filters: ChoreFilters,
    availableAssignees: List<String>,
    onFiltersChange: (ChoreFilters) -> Unit,
    onQueryChange: (String) -> Unit,
    onMoreFiltersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var assigneeMenuExpanded by remember { mutableStateOf(false) }
    var stateMenuExpanded by remember { mutableStateOf(false) }
    var dueWithinMenuExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(filters.query.isNotEmpty()) }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchExpanded) {
            IconButton(onClick = { searchExpanded = false }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.collapse_search))
            }
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = filters.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_chores_label)) },
                singleLine = true,
                // Issue #69: trailing clear ("x") button, matching web's search field.
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                }
            )
        } else {
            FilterGroupIconButton(
                icon = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_chores_label),
                active = filters.query.isNotEmpty(),
                onClick = { searchExpanded = true }
            )

            Box {
                FilterGroupIconButton(
                    icon = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.filter_assignee_label),
                    active = filters.assignees.isNotEmpty(),
                    onClick = { assigneeMenuExpanded = true }
                )
                DropdownMenu(expanded = assigneeMenuExpanded, onDismissRequest = { assigneeMenuExpanded = false }) {
                    if (availableAssignees.isEmpty()) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.filter_assignee)) }, onClick = {})
                    }
                    // Multi-select: stays open across taps (only closes via outside-tap/dismiss) so
                    // several assignees can be toggled in one go, matching a filter checklist.
                    availableAssignees.forEach { assignee ->
                        val checked = assignee in filters.assignees
                        DropdownMenuItem(
                            text = { Text(assignee) },
                            leadingIcon = if (checked) {
                                { Icon(Icons.Filled.CheckCircle, contentDescription = null) }
                            } else {
                                null
                            },
                            onClick = {
                                onFiltersChange(
                                    filters.copy(
                                        assignees = if (checked) filters.assignees - assignee else filters.assignees + assignee
                                    )
                                )
                            }
                        )
                    }
                }
            }

            Box {
                FilterGroupIconButton(
                    icon = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.filter_state_label),
                    active = filters.state != ChoreStateFilter.ALL,
                    onClick = { stateMenuExpanded = true }
                )
                DropdownMenu(expanded = stateMenuExpanded, onDismissRequest = { stateMenuExpanded = false }) {
                    val options = listOf(
                        ChoreStateFilter.ALL to stringResource(R.string.filter_state_all),
                        ChoreStateFilter.DUE to stringResource(R.string.filter_state_due),
                        ChoreStateFilter.COMPLETE to stringResource(R.string.filter_state_complete)
                    )
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onFiltersChange(filters.copy(state = value))
                                stateMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Box {
                FilterGroupIconButton(
                    icon = Icons.Filled.Schedule,
                    contentDescription = stringResource(R.string.filter_due_within_label),
                    active = filters.dueWithin != DueWithinFilter.ALL,
                    onClick = { dueWithinMenuExpanded = true }
                )
                DropdownMenu(expanded = dueWithinMenuExpanded, onDismissRequest = { dueWithinMenuExpanded = false }) {
                    val options = listOf(
                        DueWithinFilter.ALL to stringResource(R.string.due_within_all),
                        DueWithinFilter.TODAY to stringResource(R.string.due_within_today),
                        DueWithinFilter.NEXT_3_DAYS to stringResource(R.string.due_within_3_days),
                        DueWithinFilter.NEXT_7_DAYS to stringResource(R.string.due_within_7_days),
                        DueWithinFilter.NEXT_30_DAYS to stringResource(R.string.due_within_30_days)
                    )
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onFiltersChange(filters.copy(dueWithin = value))
                                dueWithinMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Overflow: schedule type, assignment type, and enabled status don't get their own
            // top-level icon -- they stay behind the full ChoreFiltersDialog, same as this icon
            // also duplicating the three groups above for anyone who prefers the full dialog.
            FilterGroupIconButton(
                icon = Icons.Filled.Tune,
                contentDescription = stringResource(R.string.filter_more_label),
                active = filters.scheduleType != null || filters.assignmentType != null || filters.enabledFilter != EnabledFilter.ALL,
                onClick = onMoreFiltersClick
            )
        }
    }
}

@Composable
private fun FilterGroupIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        if (active) {
            BadgedBox(badge = { Badge() }) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}
