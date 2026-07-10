package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #13 behavior: "Filters: schedule type, assignment type, assignee (multi-select,
 * including a synthetic 'Unassigned' option), state (due/complete), 'due within' ... enabled/
 * disabled status. 'Clear filters' action."
 */
@Composable
fun ChoreFiltersDialog(
    filters: ChoreFilters,
    availableAssignees: List<String>,
    availableScheduleTypes: List<String>,
    availableAssignmentTypes: List<String>,
    onApply: (ChoreFilters) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember { mutableStateOf(filters) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filters_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (availableAssignees.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.filter_assignee))
                    availableAssignees.forEach { assignee ->
                        CheckboxRow(
                            label = assignee,
                            checked = assignee in draft.assignees,
                            onCheckedChange = { checked ->
                                draft = draft.copy(
                                    assignees = if (checked) draft.assignees + assignee else draft.assignees - assignee
                                )
                            }
                        )
                    }
                }

                SectionLabel(stringResource(R.string.filter_state))
                RadioRow(stringResource(R.string.filter_state_all), draft.state == ChoreStateFilter.ALL) {
                    draft = draft.copy(state = ChoreStateFilter.ALL)
                }
                RadioRow(stringResource(R.string.filter_state_due), draft.state == ChoreStateFilter.DUE) {
                    draft = draft.copy(state = ChoreStateFilter.DUE)
                }
                RadioRow(stringResource(R.string.filter_state_complete), draft.state == ChoreStateFilter.COMPLETE) {
                    draft = draft.copy(state = ChoreStateFilter.COMPLETE)
                }

                SectionLabel(stringResource(R.string.filter_due_within))
                listOf(
                    DueWithinFilter.ALL to stringResource(R.string.due_within_all),
                    DueWithinFilter.TODAY to stringResource(R.string.due_within_today),
                    DueWithinFilter.NEXT_3_DAYS to stringResource(R.string.due_within_3_days),
                    DueWithinFilter.NEXT_7_DAYS to stringResource(R.string.due_within_7_days),
                    DueWithinFilter.NEXT_30_DAYS to stringResource(R.string.due_within_30_days)
                ).forEach { (value, label) ->
                    RadioRow(label, draft.dueWithin == value) { draft = draft.copy(dueWithin = value) }
                }

                SectionLabel(stringResource(R.string.filter_enabled_status))
                RadioRow(stringResource(R.string.filter_state_all), draft.enabledFilter == EnabledFilter.ALL) {
                    draft = draft.copy(enabledFilter = EnabledFilter.ALL)
                }
                RadioRow(stringResource(R.string.filter_enabled), draft.enabledFilter == EnabledFilter.ENABLED) {
                    draft = draft.copy(enabledFilter = EnabledFilter.ENABLED)
                }
                RadioRow(stringResource(R.string.filter_disabled), draft.enabledFilter == EnabledFilter.DISABLED) {
                    draft = draft.copy(enabledFilter = EnabledFilter.DISABLED)
                }

                if (availableScheduleTypes.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.filter_schedule_type))
                    RadioRow(stringResource(R.string.filter_state_all), draft.scheduleType == null) {
                        draft = draft.copy(scheduleType = null)
                    }
                    availableScheduleTypes.forEach { type ->
                        RadioRow(type, draft.scheduleType == type) { draft = draft.copy(scheduleType = type) }
                    }
                }

                if (availableAssignmentTypes.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.filter_assignment_type))
                    RadioRow(stringResource(R.string.filter_state_all), draft.assignmentType == null) {
                        draft = draft.copy(assignmentType = null)
                    }
                    availableAssignmentTypes.forEach { type ->
                        RadioRow(type, draft.assignmentType == type) { draft = draft.copy(assignmentType = type) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(draft) }) { Text(stringResource(R.string.apply)) }
        },
        dismissButton = {
            TextButton(onClick = { draft = ChoreFilters(query = draft.query); onApply(draft) }) {
                Text(stringResource(R.string.clear_filters))
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, modifier = Modifier.padding(top = Space.md, bottom = Space.xs))
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = Space.xxs)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, modifier = Modifier.padding(start = Space.sm))
    }
}

@Composable
private fun CheckboxRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = checked, onClick = { onCheckedChange(!checked) })
            .padding(vertical = Space.xxs)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, modifier = Modifier.padding(start = Space.sm))
    }
}
