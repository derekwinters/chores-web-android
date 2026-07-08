package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.ChoreCreateRequestDto
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.ChoreUpdateRequestDto
import com.derekwinters.chores.data.network.dto.ScheduleConfig

/**
 * Domain model for a chore (issue #5, extended by #13/#14/#15/#16 with the schedule/assignment/
 * constraint fields the Chores screen's filters, stats panel, card detail, and create/edit form
 * need).
 *
 * `currentAssignee == null` means the chore has no fixed assignee — the chore list's Assignee
 * cell shows [nextAssignee] for rotating chores, else "Anyone" (issue #162: never the static
 * "Completer" placeholder, which is a completion-time concept — see CompleterPickerDialog).
 * Completing such a chore requires picking a Completer from [eligiblePeople].
 *
 * There is no separate "rotation list" concept in the real backend: [eligiblePeople] doubles as
 * both the "open" assignment type's eligible-to-complete pool and the "rotating" type's rotation
 * membership, and [rotationIndex] tracks whose turn it is within that same list.
 *
 * `disabled` is kept matching the wire format directly (rather than inverted to `enabled`) to
 * avoid maintaining an inverted mapping at the serialization boundary; [enabled] is a computed
 * convenience getter for call sites (filters, stats) that read more naturally as a positive
 * condition.
 *
 * [scheduleConfig]'s real inner shape is unconfirmed — see [ScheduleConfig]'s KDoc.
 */
data class Chore(
    val id: Int,
    val name: String,
    val points: Int,
    val state: String,
    val nextDue: String?,
    val currentAssignee: String?,
    val eligiblePeople: List<String>,
    val disabled: Boolean = false,
    val assignmentType: String? = null,
    val scheduleType: String? = null,
    val scheduleSummary: String? = null,
    val scheduleConfig: ScheduleConfig = ScheduleConfig(),
    /** The configured "fixed" assignee; distinct from [currentAssignee] per the real `ChoreOut` schema. */
    val assignee: String? = null,
    val rotationIndex: Int = 0,
    /** Server-computed, read-only "next up in the rotation" — not user-editable. */
    val nextAssignee: String? = null,
    val age: Int? = null,
    val lastChangedAt: String? = null,
    val lastChangedBy: String? = null,
    val lastChangeType: String? = null,
    val lastCompletedAt: String? = null,
    val lastCompletedBy: String? = null
) {
    val enabled: Boolean get() = !disabled
    val needsCompleterSelection: Boolean get() = currentAssignee == null
    val isDue: Boolean get() = state == "due"
}

fun ChoreDto.toDomain(): Chore = Chore(
    id = id,
    name = name,
    points = points,
    state = state,
    nextDue = next_due,
    currentAssignee = current_assignee,
    eligiblePeople = eligible_people,
    disabled = disabled,
    assignmentType = assignment_type,
    scheduleType = schedule_type,
    scheduleSummary = schedule_summary,
    scheduleConfig = schedule_config,
    assignee = assignee,
    rotationIndex = rotation_index,
    nextAssignee = next_assignee,
    age = age,
    lastChangedAt = last_changed_at,
    lastChangedBy = last_changed_by,
    lastChangeType = last_change_type,
    lastCompletedAt = last_completed_at,
    lastCompletedBy = last_completed_by
)

/**
 * Issue #16: create/edit form submission payload. Only the fields relevant to [assignmentType]/
 * [scheduleType] are meaningful; the form is responsible for validating those combinations before
 * building a draft (see ChoreFormValidation). [currentAssignee] is only meaningful for the update
 * path (`ChoreCreate` has no such field); see [toCreateRequestDto]/[toUpdateRequestDto].
 */
data class ChoreDraft(
    val name: String,
    val points: Int,
    val disabled: Boolean,
    val nextDue: String?,
    val assignmentType: String,
    val assignee: String?,
    val eligiblePeople: List<String>,
    val currentAssignee: String?,
    val scheduleType: String,
    val scheduleConfig: ScheduleConfig
)

/** `POST /v1/chores` body — `ChoreCreate` has no `current_assignee`/`next_due` fields. */
fun ChoreDraft.toCreateRequestDto(): ChoreCreateRequestDto = ChoreCreateRequestDto(
    name = name,
    schedule_type = scheduleType,
    schedule_config = scheduleConfig,
    assignment_type = assignmentType,
    eligible_people = eligiblePeople,
    assignee = assignee,
    points = points,
    disabled = disabled
)

/** `PUT /v1/chores/{id}` body — matches `ChoreUpdate`'s superset of `ChoreCreate`'s fields. */
fun ChoreDraft.toUpdateRequestDto(): ChoreUpdateRequestDto = ChoreUpdateRequestDto(
    name = name,
    schedule_type = scheduleType,
    schedule_config = scheduleConfig,
    assignment_type = assignmentType,
    eligible_people = eligiblePeople,
    assignee = assignee,
    current_assignee = currentAssignee,
    points = points,
    disabled = disabled,
    next_due = nextDue
)

/** Fibonacci point values chores-web restricts the Points dropdown to (issue #16). */
val CHORE_POINT_OPTIONS = listOf(1, 2, 3, 5, 8, 13)
