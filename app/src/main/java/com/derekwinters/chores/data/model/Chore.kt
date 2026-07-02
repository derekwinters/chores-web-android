package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.ChoreDto

/**
 * Domain model for a chore list row (issue #5, behavior: "Chore list screen").
 *
 * `currentAssignee == null` means the chore has no fixed assignee — the chore list shows the
 * "Completer" placeholder for it, and completing it requires picking a Completer from
 * [eligiblePeople] (see CompleterPickerDialog).
 */
data class Chore(
    val id: Int,
    val name: String,
    val points: Int,
    val state: String,
    val nextDue: String?,
    val currentAssignee: String?,
    val eligiblePeople: List<String>
) {
    val needsCompleterSelection: Boolean get() = currentAssignee == null
}

fun ChoreDto.toDomain(): Chore = Chore(
    id = id,
    name = name,
    points = points,
    state = state,
    nextDue = next_due,
    currentAssignee = current_assignee,
    eligiblePeople = eligible_people
)
