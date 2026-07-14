package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.NotificationDto

/**
 * Domain model for one backend notification (issue #43), consumed by
 * [com.derekwinters.chores.notifications.NotificationPollWorker] to decide what to post as an
 * Android system notification and by the tap-to-ack flow.
 *
 * [type] is deliberately kept opaque (v1: `"chore_due"` only) — the worker renders the
 * server-provided [title]/[body] verbatim rather than switching on it. The lifecycle timestamps
 * stay as raw ISO-8601 strings (matching [LogEntry.timestamp]'s convention): the worker only ever
 * needs to know whether [acknowledgedAt]/[dismissedAt] are present, never to parse them.
 */
data class Notification(
    val id: Int,
    val personId: Int,
    val type: String,
    val choreId: Int?,
    val title: String,
    val body: String,
    val createdAt: String,
    val deliveredAt: String?,
    val acknowledgedAt: String?,
    val dismissedAt: String?
) {
    /** Acknowledged and dismissed items are never (re-)posted as system notifications. */
    val isActionable: Boolean get() = acknowledgedAt == null && dismissedAt == null
}

fun NotificationDto.toDomain(): Notification = Notification(
    id = id,
    personId = person_id,
    type = type,
    choreId = chore_id,
    title = title,
    body = body,
    createdAt = created_at,
    deliveredAt = delivered_at,
    acknowledgedAt = acknowledged_at,
    dismissedAt = dismissed_at
)
