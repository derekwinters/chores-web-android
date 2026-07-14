package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/notifications` and body of `POST /v1/notifications/{id}/ack`
 * (chores-web-backend#39). Matches the backend's `NotificationOut` schema exactly (see
 * `backend/app/schemas.py`).
 *
 * Following the same wire-mapping idiom as every other DTO in this package (`ChoreDto`,
 * `LogEntryDto`, …): the snake_case backend field names are the Kotlin property names directly,
 * so no `@SerialName` annotations are needed — kotlinx.serialization matches on property name,
 * and `ignoreUnknownKeys = true` (see [com.derekwinters.chores.di.NetworkModule]) drops any
 * further backend fields.
 *
 * `type` is opaque to the client (v1 emits only `"chore_due"`); the worker renders whatever
 * server-sent [title]/[body] it carries. The three `*_at` lifecycle timestamps are nullable and
 * default to null so partial fixtures deserialize the same way: `delivered_at` is set server-side
 * on the first list return (the server owns delivery state), `acknowledged_at` on ack, and
 * `dismissed_at` when the user dismisses it elsewhere.
 */
@Serializable
data class NotificationDto(
    val id: Int,
    val person_id: Int,
    val type: String,
    val chore_id: Int? = null,
    val title: String,
    val body: String,
    val created_at: String,
    val delivered_at: String? = null,
    val acknowledged_at: String? = null,
    val dismissed_at: String? = null
)
