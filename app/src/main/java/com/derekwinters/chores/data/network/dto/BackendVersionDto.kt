package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response body for the backend's public `GET /version` (chores-web-backend#27, issue #35's
 * "Backend version" About-screen section) — no auth required. This is unambiguously the
 * backend's own version/update status, unlike the removed `UpdateCheckStatusDto` and its
 * `/v1/config/updates/status` and `/v1/config/updates/check` endpoints, which this app
 * previously (incorrectly) used to represent its own version.
 */
@Serializable
data class BackendVersionDto(
    val version: String,
    val latest_version: String? = null,
    val update_available: Boolean = false,
    val checked_at: String? = null
)
