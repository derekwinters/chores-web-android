package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.BackendVersionDto

/**
 * Issue #35: the backend's own version/update status, from its public `GET /version` endpoint
 * (chores-web-backend#27). Distinct from [AppVersionUiState], which is this Android app's own
 * client-side GitHub-releases check — the backend has no way to know what app version is actually
 * deployed, so the two are intentionally decoupled and rendered as separate About-screen
 * sections.
 */
sealed interface BackendVersionUiState {
    data object Loading : BackendVersionUiState

    data class Available(
        val version: String,
        val latestVersion: String?,
        val updateAvailable: Boolean,
        val checkedAt: String?
    ) : BackendVersionUiState

    /**
     * The request failed, timed out, or 404'd (e.g. talking to a backend that predates this
     * endpoint). Required fallback (issue #35): version renders as the literal string "unknown"
     * and status as "unsupported check" — never a crash, never blocking the rest of the screen.
     */
    data object Unsupported : BackendVersionUiState
}

fun BackendVersionDto.toDomain(): BackendVersionUiState.Available = BackendVersionUiState.Available(
    version = version,
    latestVersion = latest_version,
    updateAvailable = update_available,
    checkedAt = checked_at
)
