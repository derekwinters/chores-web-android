package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/chores`, trimmed to the fields issue #5 needs (name,
 * assignee-or-Completer, points, state, next_due) plus `eligible_people`, which chores-web's
 * `ChoreOut` schema already includes per chore and which we reuse to populate the
 * Completer-picker dialog instead of adding a separate `/v1/people` call (out of scope for #5).
 *
 * The JSON deserializer is configured with `ignoreUnknownKeys = true` (see NetworkModule) so the
 * many other `ChoreOut` fields (schedule_type, disabled, rotation_index, ...) are simply dropped.
 */
@Serializable
data class ChoreDto(
    val id: Int,
    val name: String,
    val points: Int,
    val state: String,
    val next_due: String? = null,
    val current_assignee: String? = null,
    val eligible_people: List<String> = emptyList()
)

/**
 * Request body for `POST /v1/chores/{id}/complete`, matching chores-web's `CompleteBody` schema.
 * `completed_by` is null for already-assigned chores (server defaults to `current_assignee`) and
 * set to the chosen username when the Completer-picker dialog is used.
 */
@Serializable
data class CompleteChoreRequestDto(
    val completed_by: String? = null
)
