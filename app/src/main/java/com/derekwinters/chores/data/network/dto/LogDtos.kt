package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/log`, chores-web's unified Activity Log (issue #19), also reused
 * (filtered) for the User Detail chore-activity feed (issue #17) and the Chore card History link
 * (issue #15). Matches the backend's `ChoreLogOut` schema exactly (see the real OpenAPI spec):
 * there is no literal `target_type`/`target_name` JSON field, but target type *is* encoded on the
 * wire via a documented sentinel (issue #33): person-log rows are merged into this unified
 * response with `chore_id == 0` (the backend's own "UserLog sentinel") and
 * `chore_name == "Person: {name}"`. Prefer checking `chore_id == 0` over string-matching
 * `chore_name` when deriving target type client-side — same condition, more robust.
 */
@Serializable
data class LogEntryDto(
    val id: Int,
    val chore_id: Int,
    val chore_name: String,
    val person: String,
    val action: String,
    val timestamp: String,
    val reassigned_to: String? = null,
    val assignee: String? = null,
    val field_name: String? = null,
    val old_value: String? = null,
    val new_value: String? = null
)

/**
 * `GET /v1/log/retention` and `POST /v1/log/retention` share this request/response shape — matches
 * the backend's `RetentionSettings` schema exactly (issue #22's log-retention setting; a real,
 * separate backend resource, not part of `ConfigOut`/`ConfigUpdate`).
 */
@Serializable
data class RetentionSettingsDto(
    val retention_days: Int
)

/**
 * Response element for `GET /v1/auth/log`, issue #21's separate admin auth audit trail. The real
 * endpoint returns a bare array (no `{items, total}` wrapper), matching `AuthLogOut` exactly.
 */
@Serializable
data class AuthLogEntryDto(
    val id: Int,
    val timestamp: String,
    val username: String,
    val action: String,
    val changed_by: String? = null
)
