package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.CHORE_POINT_OPTIONS
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.theme.Space
import com.derekwinters.chores.ui.theme.pillShape
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

/**
 * Issue #100: named day-abbreviation labels for the weekly-schedule day picker, indexed to match
 * the existing `weeklyDays` data model (0=Sun ... 6=Sat) — display-only, no change to the
 * underlying weekday indices.
 */
private val WEEKDAY_ABBREVIATIONS = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

/**
 * Issue #108: fully-rounded "pill" shape applied to the Points selector and eligible-people
 * picker's [FilterChip]s. Material3's default `FilterChip` shape is a small (8dp) corner radius
 * rounded-rectangle, not a true stadium/pill shape -- this closes that styling gap so these two
 * pickers read as pill toggle buttons matching web, per issue #108's ask. Issue #24: aliases the
 * token layer's [pillShape] (9999dp corner radius, renders identically to `percent = 50`).
 */
private val PILL_SHAPE = pillShape

/**
 * Issue #16: chore create/edit form, chores-web's `ChoreForm.jsx` equivalent — the richest
 * screen in this app. Field/validation rules live in [ChoreFormState]/ChoreFormState.validate();
 * this composable is "dumb" UI over that state.
 *
 * Thin Hilt-wired wrapper around [ChoreFormContent].
 */
@Composable
fun ChoreFormScreen(
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ChoreFormViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val availablePeople by viewModel.availablePeople.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val loadState by viewModel.loadState.collectAsState()

    LaunchedEffect(saveState) {
        if (saveState is UiState.Success) onSaved()
    }

    if (loadState is UiState.Loading) {
        CircularProgressIndicator()
        return
    }

    ChoreFormContent(
        formState = formState,
        availablePeople = availablePeople,
        saveState = saveState,
        isEditMode = viewModel.isEditMode,
        onFormChange = viewModel::updateForm,
        onSave = viewModel::save,
        onCancel = onCancel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoreFormContent(
    formState: ChoreFormState,
    availablePeople: List<String>,
    saveState: UiState<Unit>,
    isEditMode: Boolean,
    onFormChange: ((ChoreFormState) -> ChoreFormState) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSaving = saveState is UiState.Loading

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Space.lg)
    ) {
        Text(
            text = if (isEditMode) "Edit Chore" else "New Chore",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
            value = formState.name,
            onValueChange = { name -> onFormChange { it.copy(name = name) } },
            label = { Text("Name") },
            singleLine = true,
            enabled = !isSaving
        )

        SectionLabel("Points")
        Row(modifier = Modifier.fillMaxWidth()) {
            CHORE_POINT_OPTIONS.forEach { option ->
                FilterChip(
                    modifier = Modifier.padding(end = Space.xs),
                    selected = formState.points == option,
                    onClick = { onFormChange { it.copy(points = option) } },
                    label = { Text(option.toString()) },
                    enabled = !isSaving,
                    shape = PILL_SHAPE
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enabled")
            Switch(
                checked = !formState.disabled,
                onCheckedChange = { enabled -> onFormChange { it.copy(disabled = !enabled) } },
                enabled = !isSaving
            )
        }

        if (isEditMode) {
            var showDatePicker by remember { mutableStateOf(false) }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                value = formState.nextDue.orEmpty(),
                onValueChange = {},
                label = { Text("Next Due") },
                singleLine = true,
                readOnly = true,
                enabled = !isSaving,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }, enabled = !isSaving) {
                        Icon(Icons.Filled.DateRange, contentDescription = "Next Due")
                    }
                }
            )

            if (showDatePicker) {
                val initialSelectedMillis = formState.nextDue?.let { raw ->
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
                                onFormChange { it.copy(nextDue = selectedDate.toString()) }
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                // Use the same UTC-based "today" as the OK path's interpretation
                                // of DatePickerState.selectedDateMillis (documented as a
                                // UTC-midnight instant) rather than the system-default-zone
                                // LocalDate.now(), so "Today" always matches whatever date the
                                // calendar grid itself would show as today.
                                onFormChange { it.copy(nextDue = LocalDate.now(ZoneOffset.UTC).toString()) }
                                showDatePicker = false
                            }) { Text("Today") }
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        SectionLabel("Assignment")
        RadioGrid(
            options = AssignmentType.ALL,
            selected = formState.assignmentType,
            enabled = !isSaving
        ) { type -> onFormChange { it.copy(assignmentType = type) } }
        when (formState.assignmentType) {
            AssignmentType.FIXED -> {
                SectionLabel("Assignee")
                availablePeople.forEach { person ->
                    RadioRow(person, formState.assignee == person, !isSaving) {
                        onFormChange { it.copy(assignee = person) }
                    }
                }
            }
            AssignmentType.OPEN -> {
                SectionLabel("Eligible people (optional)")
                EligiblePeoplePillRow(
                    people = availablePeople,
                    selectedPeople = formState.eligiblePeople,
                    enabled = !isSaving,
                    onTogglePerson = { person ->
                        onFormChange {
                            it.copy(
                                eligiblePeople = if (person in it.eligiblePeople) {
                                    it.eligiblePeople - person
                                } else {
                                    it.eligiblePeople + person
                                }
                            )
                        }
                    }
                )
            }
            AssignmentType.ROTATING -> {
                SectionLabel("Rotation (2+ people)")
                EligiblePeoplePillRow(
                    people = availablePeople,
                    selectedPeople = formState.eligiblePeople,
                    enabled = !isSaving,
                    onTogglePerson = { person ->
                        onFormChange {
                            it.copy(
                                eligiblePeople = if (person in it.eligiblePeople) {
                                    it.eligiblePeople - person
                                } else {
                                    it.eligiblePeople + person
                                }
                            )
                        }
                    }
                )
            }
        }

        SectionLabel("Schedule")
        RadioGrid(
            options = ScheduleType.ALL,
            selected = formState.scheduleType,
            enabled = !isSaving
        ) { type -> onFormChange { it.copy(scheduleType = type) } }
        when (formState.scheduleType) {
            ScheduleType.WEEKLY -> {
                SectionLabel("Days of week")
                WeekdayPillRow(
                    selectedDays = formState.weeklyDays,
                    enabled = !isSaving,
                    onToggleDay = { day ->
                        // Derive the toggle from the state passed into the updater (`it`),
                        // not from a boolean captured at composition time -- FilterChip's
                        // onClick (unlike Checkbox's onCheckedChange) has no built-in
                        // "new value" parameter, so we must compute membership ourselves;
                        // reading it fresh off `it.weeklyDays` here avoids ever toggling
                        // against a stale/composition-time snapshot of selection state.
                        onFormChange {
                            it.copy(weeklyDays = if (day in it.weeklyDays) it.weeklyDays - day else it.weeklyDays + day)
                        }
                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = formState.everyOtherWeek,
                        onCheckedChange = { checked -> onFormChange { it.copy(everyOtherWeek = checked) } },
                        enabled = !isSaving
                    )
                    Text("Every other week")
                }
            }
            ScheduleType.INTERVAL -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                    value = formState.intervalDays?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(intervalDays = value.toIntOrNull()) } },
                    label = { Text("Every N days") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
            ScheduleType.MONTHLY -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                    value = formState.dayOfMonth?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(dayOfMonth = value.toIntOrNull()) } },
                    label = { Text("Day of month") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
            ScheduleType.YEARLY -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                    value = formState.month?.toString().orEmpty(),
                    onValueChange = { value -> onFormChange { it.copy(month = value.toIntOrNull()) } },
                    label = { Text("Month (1-12)") },
                    singleLine = true,
                    enabled = !isSaving
                )
            }
        }

        if (formState.scheduleType != ScheduleType.YEARLY) {
            // Issue #103: collapsible section header, matching the expand/collapse chevron
            // pattern already used by ChoresStatsPanel's stats header and ActivityLogScreen's
            // row-expand affordance -- whole-row clickable (bigger tap target) rather than just
            // an icon button, matching ActivityLogScreen's LogRow convention.
            var constraintsExpanded by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Space.lg, bottom = Space.xs)
                    .clickable { constraintsExpanded = !constraintsExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Constraints", style = MaterialTheme.typography.titleSmall)
                Icon(
                    imageVector = if (constraintsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (constraintsExpanded) "Collapse constraints" else "Expand constraints"
                )
            }

            if (constraintsExpanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = formState.evenOddConstraint == "even",
                        onClick = { onFormChange { it.copy(evenOddConstraint = if (it.evenOddConstraint == "even") null else "even") } },
                        enabled = !isSaving
                    )
                    Text("Even days only")
                    RadioButton(
                        selected = formState.evenOddConstraint == "odd",
                        onClick = { onFormChange { it.copy(evenOddConstraint = if (it.evenOddConstraint == "odd") null else "odd") } },
                        enabled = !isSaving
                    )
                    Text("Odd days only")
                }

                // Issue #105: condition-not-met behavior choice (skip vs. delay), web parity.
                // Single-select RadioButton pair following the same direct-set pattern as the
                // even/odd constraint above (not a Set-membership toggle like the weekday
                // pickers) -- each button sets `constraintNotMetBehavior` to a fixed target
                // value. Labels "Skip"/"Delay" don't collide with any other label in this
                // section (or the WEEKLY schedule's separate "Days of week" picker) under any
                // schedule type.
                Text(
                    text = "If constraint isn't met",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = Space.sm, bottom = Space.xs)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = formState.constraintNotMetBehavior == ConstraintBehavior.SKIP,
                                enabled = !isSaving,
                                onClick = { onFormChange { it.copy(constraintNotMetBehavior = ConstraintBehavior.SKIP) } }
                            )
                            .padding(end = Space.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = formState.constraintNotMetBehavior == ConstraintBehavior.SKIP,
                            onClick = { onFormChange { it.copy(constraintNotMetBehavior = ConstraintBehavior.SKIP) } },
                            enabled = !isSaving
                        )
                        Text("Skip")
                    }
                    Row(
                        modifier = Modifier.selectable(
                            selected = formState.constraintNotMetBehavior == ConstraintBehavior.DELAY,
                            enabled = !isSaving,
                            onClick = { onFormChange { it.copy(constraintNotMetBehavior = ConstraintBehavior.DELAY) } }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = formState.constraintNotMetBehavior == ConstraintBehavior.DELAY,
                            onClick = { onFormChange { it.copy(constraintNotMetBehavior = ConstraintBehavior.DELAY) } },
                            enabled = !isSaving
                        )
                        Text("Delay")
                    }
                }

                // Issue #103: "weekdays only" sub-picker, web parity -- reuses the same
                // day-abbreviation-pill row as the weekly schedule's "Days of week" picker
                // (issue #100), but backed by `weekdayConstraint` instead of `weeklyDays`.
                // Hidden for WEEKLY schedules: the weekly "Days of week" picker higher up the
                // form already lets the user pick specific weekdays, so showing this sub-picker
                // too would be a redundant, overlapping "pick your weekdays" control (and would
                // render two sets of identically-labeled Mon..Sun pills on screen at once).
                if (formState.scheduleType != ScheduleType.WEEKLY) {
                    Text(
                        text = "Weekdays only",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.sm, bottom = Space.xs)
                    )
                    WeekdayPillRow(
                        selectedDays = formState.weekdayConstraint,
                        enabled = !isSaving,
                        onToggleDay = { day ->
                            // Same convention as the weekly Days-of-week pills and the #100 fix:
                            // derive membership from the state passed into the transform (`it`),
                            // not from a value captured at composition time.
                            onFormChange {
                                it.copy(
                                    weekdayConstraint = if (day in it.weekdayConstraint) {
                                        it.weekdayConstraint - day
                                    } else {
                                        it.weekdayConstraint + day
                                    }
                                )
                            }
                        }
                    )
                }
            }
        }

        if (saveState is UiState.Error) {
            Text(
                modifier = Modifier.padding(top = Space.sm),
                text = saveState.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = Space.lg), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("Cancel") }
            Button(modifier = Modifier.padding(start = Space.sm), onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = Space.sm))
                }
                Text("Save")
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Space.lg, bottom = Space.xs))
}

/**
 * Issue #103: shared row of weekday-abbreviation [FilterChip] pills, extracted from issue #100's
 * weekly-schedule "Days of week" picker so the Constraints section's "weekdays only" sub-picker
 * can reuse the same look/interaction instead of duplicating it. Callers own the backing
 * `Set<Int>` (`weeklyDays` or `weekdayConstraint`) and how toggling is applied to form state.
 */
@Composable
private fun WeekdayPillRow(selectedDays: Set<Int>, enabled: Boolean, onToggleDay: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAY_ABBREVIATIONS.forEachIndexed { day, abbreviation ->
            FilterChip(
                modifier = Modifier.padding(end = Space.xs),
                selected = day in selectedDays,
                onClick = { onToggleDay(day) },
                label = { Text(abbreviation) },
                enabled = enabled
            )
        }
    }
}

/**
 * Issue #108: shared row of per-person [FilterChip] pills backing the eligible-people picker,
 * replacing the previous `CheckboxRow`-per-person layout for both `AssignmentType.OPEN`
 * ("Eligible people") and `AssignmentType.ROTATING` ("Rotation") branches, which duplicated the
 * same checkbox-row call. Follows the same Set-membership toggle convention established by
 * [WeekdayPillRow]/issue #100's fix: membership is derived from the state passed into the
 * `onTogglePerson` caller's `onFormChange` transform, not a value captured at composition time.
 *
 * Only one of OPEN/ROTATING is ever rendered at a time (mutually exclusive `when` branches on
 * `assignmentType`), so these person-name pills never appear on screen alongside `RadioRow`'s
 * person-name rows from the FIXED branch -- no label collision.
 */
@Composable
private fun EligiblePeoplePillRow(
    people: List<String>,
    selectedPeople: Set<String>,
    enabled: Boolean,
    onTogglePerson: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        people.forEach { person ->
            FilterChip(
                modifier = Modifier.padding(end = Space.xs),
                selected = person in selectedPeople,
                onClick = { onTogglePerson(person) },
                label = { Text(person) },
                enabled = enabled,
                shape = PILL_SHAPE
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(vertical = Space.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = Space.sm))
    }
}

/**
 * Issue #97: assignment-type/schedule-type radio options laid out in a 3-column grid
 * (matching web) instead of a vertical stack. Options wrap into rows of 3; a short final row
 * is padded with empty [Spacer]s so cells stay aligned to the 3-column grid.
 */
@Composable
private fun RadioGrid(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    labelFor: (String) -> String = { it.replaceFirstChar(Char::uppercase) },
    onSelect: (String) -> Unit
) {
    options.chunked(3).forEach { rowOptions ->
        Row(modifier = Modifier.fillMaxWidth()) {
            rowOptions.forEach { option ->
                RadioGridCell(
                    modifier = Modifier.weight(1f),
                    label = labelFor(option),
                    selected = selected == option,
                    enabled = enabled,
                    onClick = { onSelect(option) }
                )
            }
            repeat(3 - rowOptions.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RadioGridCell(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .selectable(selected = selected, enabled = enabled, onClick = onClick)
            .padding(top = Space.xxs, bottom = Space.xxs, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(text = label, modifier = Modifier.padding(start = Space.xs))
    }
}
