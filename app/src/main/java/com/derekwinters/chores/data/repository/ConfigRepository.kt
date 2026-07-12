package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.model.toDto
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Admin-configurable settings backing the Settings screens (issue #20) and the Dashboard/Chores
 * due-soon window (issue #12).
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getConfig(): Result<AppConfig> = safeApiCall { api.getConfig() }.map { it.toDomain() }

    /** No partial-update endpoint is used by this client, so the full config is round-tripped. */
    suspend fun updateConfig(config: AppConfig): Result<AppConfig> =
        safeApiCall { api.updateConfig(config.toDto()) }.map { it.toDomain() }

    /**
     * Issue #35: "Backend version" About-screen section, sourced from the backend's public
     * `GET /version` (chores-web-backend#27). Callers should fall back to
     * [BackendVersionUiState.Unsupported] on failure (see [SettingsViewModel]) rather than
     * surfacing an error — a backend predating this endpoint (404) is an expected, not
     * exceptional, case.
     */
    suspend fun getBackendVersion(): Result<BackendVersionUiState.Available> =
        safeApiCall { api.getBackendVersion() }.map { it.toDomain() }
}
