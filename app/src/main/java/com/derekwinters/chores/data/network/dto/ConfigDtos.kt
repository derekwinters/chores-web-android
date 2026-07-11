package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response/request body for `GET/PUT /v1/config` — matches the backend's `ConfigOut`/
 * `ConfigUpdate` schemas (see the real OpenAPI spec) exactly. An earlier pass guessed this shape
 * from GitHub issue text alone (fields like `app_title`, `notify_days_before`, `due_hour`,
 * `log_retention_days`, and bundled-in version/update-availability fields) and got every field
 * name wrong; this backs the General/Auth/Chores sections of Settings (issue #20) and the
 * Dashboard/Chores due-soon window (issue #12, `due_soon_days`). Sent back in full on `PUT` (this
 * client doesn't use `ConfigUpdate`'s partial-update support), so every field the client doesn't
 * render still round-trips via `ignoreUnknownKeys` + these defaults.
 */
@Serializable
data class ConfigDto(
    val title: String = "Chores",
    val auth_enabled: Boolean = false,
    val timezone: String = "UTC",
    val due_soon_days: Int = 3,
    val due_time_hour: Int = 0,
    val update_check_enabled: Boolean = true,
    val update_check_interval: Int = 24
)
