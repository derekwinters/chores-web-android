package com.derekwinters.chores.ui.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import com.derekwinters.chores.ui.common.humanizeActionLabel
import com.derekwinters.chores.ui.theme.LocalThemeOption
import com.derekwinters.chores.ui.theme.parseHexColor
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Issue #19: chores-web's unified Activity Log — filters, pagination, and row-expand detail with
 * amendment diffs (old/new value for `updated` entries, `reassigned_to` for reassignments).
 *
 * Thin Hilt-wired wrapper around [ActivityLogContent].
 */
@Composable
fun ActivityLogScreen(
    modifier: Modifier = Modifier,
    viewModel: ActivityLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()

    ActivityLogContent(
        modifier = modifier,
        uiState = uiState,
        filters = filters,
        onFiltersChange = viewModel::updateFilters,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogContent(
    uiState: UiState<ActivityLogPageState>,
    filters: LogFilters,
    onFiltersChange: (LogFilters) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f).padding(end = 4.dp),
                value = filters.person.orEmpty(),
                onValueChange = { value -> onFiltersChange(filters.copy(person = value.ifBlank { null })) },
                label = { Text("Person") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                value = filters.chore.orEmpty(),
                onValueChange = { value -> onFiltersChange(filters.copy(chore = value.ifBlank { null })) },
                label = { Text("Chore") },
                singleLine = true
            )
        }

        // Issue #68: date-range filters, same read-only-field + trailing-icon DatePickerDialog
        // convention as the Chore form's "Next Due" date picker (issue #16) rather than inventing
        // new date-input UI.
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            DateFilterField(
                modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("startDateFilter"),
                label = "Start Date",
                value = filters.start,
                onValueChange = { date -> onFiltersChange(filters.copy(start = date)) }
            )
            DateFilterField(
                modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("endDateFilter"),
                label = "End Date",
                value = filters.end,
                onValueChange = { date -> onFiltersChange(filters.copy(end = date)) }
            )
        }

        // Issue #68: action-type filter -- a scrollable FilterChip row (same selection widget as
        // the Chore form's point-value picker, issue #16's CHORE_POINT_OPTIONS row) rather than a
        // dropdown/menu, since this app has no existing dropdown-menu precedent and FilterChip is
        // both an established pattern here and straightforward to drive in Compose UI tests.
        // Option labels reuse [humanizeActionLabel] so they match the row display text established
        // by issue #73, rather than showing raw snake_case action values.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    modifier = Modifier.padding(end = 4.dp),
                    selected = filters.action == null,
                    onClick = { onFiltersChange(filters.copy(action = null)) },
                    label = { Text("All") }
                )
                LOG_ACTION_TYPES.forEach { action ->
                    FilterChip(
                        modifier = Modifier.padding(end = 4.dp),
                        selected = filters.action == action,
                        onClick = { onFiltersChange(filters.copy(action = action)) },
                        label = { Text(humanizeActionLabel(action)) }
                    )
                }
            }

            // Issue #68: "Clear filters" resets person/chore/action/date-range together, same
            // conditional-on-isActive convention as Chores' "Clear filters" (issue #74).
            if (filters.isActive) {
                TextButton(
                    modifier = Modifier.testTag("clearFiltersButton"),
                    onClick = { onFiltersChange(LogFilters()) }
                ) { Text("Clear filters") }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (uiState) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val page = uiState.data
                    if (page.entries.isEmpty()) {
                        Text(modifier = Modifier.align(Alignment.Center), text = "No activity found")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(page.entries, key = { it.id }) { entry -> LogRow(entry) }
                        }
                    }
                }
            }
        }

        if (uiState is UiState.Success) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onPreviousPage, enabled = uiState.data.page > 1) { Text("Previous") }
                Text("Page ${uiState.data.page} of ${uiState.data.totalPages} (${uiState.data.total} total)")
                TextButton(onClick = onNextPage, enabled = uiState.data.page < uiState.data.totalPages) { Text("Next") }
            }
        }
    }
}

/**
 * Issue #68: known Activity Log action values offered by the action-type filter, mirroring the
 * vocabulary [actionBadgeColor] already recognizes plus "created"/"updated" (both appear on real
 * rows but aren't singled out for special badge coloring). A fixed list rather than one derived
 * from the currently-loaded page: the filter should offer every action type regardless of what
 * happens to be on the current (already-filtered/paged) result set.
 */
private val LOG_ACTION_TYPES = listOf(
    "completed",
    "created",
    "updated",
    "skipped",
    "reassigned",
    "deleted",
    "password_changed",
    "password_reset"
)

/**
 * Issue #68: a single date-range filter field -- read-only [OutlinedTextField] showing the
 * `yyyy-MM-dd` value (or blank) with a [DatePickerDialog] behind its trailing calendar icon,
 * exactly mirroring the Chore form's "Next Due" date picker (issue #16) rather than inventing new
 * date-input UI. Extracted into its own composable since the Activity Log needs two independent
 * instances (start/end) where the Chore form only ever needed one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateFilterField(
    label: String,
    value: String?,
    onValueChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedTextField(
        modifier = modifier,
        value = value.orEmpty(),
        onValueChange = {},
        label = { Text(label) },
        singleLine = true,
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Filled.DateRange, contentDescription = label)
            }
        }
    )

    if (showDatePicker) {
        val initialSelectedMillis = value?.let { raw ->
            try {
                LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                null
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValueChange(selectedDate.toString())
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    var expanded by remember { mutableStateOf(false) }
    val isPersonTarget = entry.choreId == 0
    val targetName = entry.choreName.removePrefix("Person: ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    PillBadge(
                        text = if (isPersonTarget) "User" else "Chore",
                        color = targetBadgeColor(isPersonTarget),
                        testTag = "targetTypeChip"
                    )
                    PillBadge(
                        modifier = Modifier.padding(top = 4.dp),
                        text = humanizeActionLabel(entry.action),
                        color = actionBadgeColor(entry.action),
                        testTag = "actionBadge"
                    )
                    PillBadge(
                        modifier = Modifier.padding(top = 4.dp),
                        text = targetName,
                        color = targetBadgeColor(isPersonTarget),
                        testTag = "targetBadge"
                    )
                    Text(
                        "${entry.person} · ${formatRelativeTimestamp(entry.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = agedTimestampColor(entry.timestamp),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("logRowTimestamp")
                    )
                }
                // Issue #77: chevron affordance signaling the row is expandable, matching web's
                // indicator -- Android otherwise gave no visual signal that tapping a row does
                // anything. Not wrapped in an IconButton: the enclosing Card is already the
                // clickable element (toggling `expanded` on tap anywhere in the row), so this is
                // a plain non-interactive Icon that just reflects the current state, reusing the
                // same ExpandMore/ExpandLess pair as the Chores stats panel's collapsible header
                // (issue #14) rather than introducing a new chevron icon convention.
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse entry" else "Expand entry",
                    modifier = Modifier.testTag("expandChevron")
                )
            }

            if (expanded) {
                Text("Timestamp: ${formatDateTime(entry.timestamp)}", style = MaterialTheme.typography.bodySmall)
                entry.assignee?.let { Text("Assigned to: $it", style = MaterialTheme.typography.bodySmall) }
                entry.reassignedTo?.let { Text("Reassigned to: $it", style = MaterialTheme.typography.bodySmall) }
                if (entry.isAmendment) {
                    Text(
                        "${entry.fieldName}: ${entry.oldValue.orEmpty()} -> ${entry.newValue.orEmpty()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Issue #71: pill-shaped, color-coded badge shared by the action and target values on each
 * Activity Log row, replacing the previous plain `Text`/generic uncolored `AssistChip` treatment.
 * A translucent tint of [color] as the container plus the opaque [color] as content color keeps
 * the badge legible against both light and dark surfaces without needing per-theme on-color pairs.
 *
 * [testTag] (when given) is applied to the inner `Text`, not the outer `Surface`: unlike
 * `AssistChip` (which merges its label's semantics into its own clickable node), a plain
 * non-clickable `Surface` doesn't merge descendant semantics, so tagging the `Surface` itself
 * would leave `onNodeWithTag(...).assertTextEquals(...)` unable to see the text.
 */
@Composable
private fun PillBadge(text: String, color: Color, modifier: Modifier = Modifier, testTag: String? = null) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .let { base -> if (testTag != null) base.testTag(testTag) else base }
        )
    }
}

/**
 * Issue #71: per-action-type color scheme for the action badge, mirroring chores-web's
 * success/warning/error-toned treatment. Reuses the same [LocalThemeOption] success/warning/error
 * lookup (falling back to hardcoded Material equivalents) established by Dashboard's trend
 * coloring (issue #120) and the Chore row's Complete/Delete actions (issue #93), rather than
 * inventing new colors -- `completed`/`created` land on the positive/success color, `skipped`/
 * `reassigned`/password-related events land on the warning (attention-needed) color, `deleted`
 * lands on error, and anything else (e.g. `updated` amendments, unmapped future actions) falls
 * back to a neutral onSurfaceVariant so unknown actions never silently render as red/green.
 */
@Composable
private fun actionBadgeColor(action: String): Color {
    val themeOption = LocalThemeOption.current
    return when (action) {
        "completed", "created" -> themeOption?.success?.let(::parseHexColor) ?: MaterialTheme.colorScheme.primary
        "skipped", "reassigned", "password_changed", "password_reset" ->
            themeOption?.warning?.let(::parseHexColor) ?: Color(0xFFF9A825)
        "deleted" -> themeOption?.error?.let(::parseHexColor) ?: MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

/**
 * Issue #71: color-codes the target badge (both the User/Chore type indicator and the target
 * name itself) by target type, reusing [ThemeOption]'s primary/secondary split that already
 * distinguishes the two elsewhere in the theme (see [com.derekwinters.chores.ui.theme.ChoresTheme]).
 */
@Composable
private fun targetBadgeColor(isPersonTarget: Boolean): Color {
    val themeOption = LocalThemeOption.current
    return if (isPersonTarget) {
        themeOption?.secondary?.let(::parseHexColor) ?: MaterialTheme.colorScheme.secondary
    } else {
        themeOption?.primary?.let(::parseHexColor) ?: MaterialTheme.colorScheme.primary
    }
}

/**
 * Issue #33: relative-timestamp thresholds/strings mirror chores-web's `formatRelativeTimestamp`
 * exactly. Falls back to the raw string unchanged if it can't be parsed, same defensive pattern
 * as #31's `formatNextDue`.
 */
private fun formatRelativeTimestamp(raw: String): String {
    return try {
        val instant = Instant.parse(raw)
        val duration = Duration.between(instant, Instant.now())
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()
        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    } catch (e: DateTimeParseException) {
        raw
    }
}

/**
 * Issue #81: mirrors chores-web's Activity Log behavior of flagging stale entries by coloring
 * timestamps 24h+ old in a muted-red tone; unparsable timestamps are treated as not-aged (same
 * fail-safe stance as [formatRelativeTimestamp] falling back to the raw string) rather than
 * risking a false "stale" flag on bad data.
 */
private fun isAgedTimestamp(raw: String): Boolean {
    return try {
        Duration.between(Instant.parse(raw), Instant.now()).toHours() >= 24
    } catch (e: DateTimeParseException) {
        false
    }
}

/**
 * Issue #81: returns the muted-red color for timestamps [isAgedTimestamp], or [Color.Unspecified]
 * otherwise so the `Text` falls back to its normal default color. Reuses the same
 * [LocalThemeOption]/`error`/`parseHexColor` lookup (falling back to
 * `MaterialTheme.colorScheme.error`) already established by #71's `actionBadgeColor` for the
 * "deleted" action, rather than inventing a new red -- a reduced-alpha (0.7f) variant of that
 * color gives the "muted" (lower-emphasis) treatment web uses instead of a full-strength,
 * alarm-toned red.
 */
@Composable
private fun agedTimestampColor(raw: String): Color {
    if (!isAgedTimestamp(raw)) return Color.Unspecified
    val themeOption = LocalThemeOption.current
    val errorColor = themeOption?.error?.let(::parseHexColor) ?: MaterialTheme.colorScheme.error
    return errorColor.copy(alpha = 0.7f)
}
