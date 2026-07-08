package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.model.CurrentTheme
import com.derekwinters.chores.data.model.ThemeDefaultInfo
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.model.toColorsDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.ThemeRenameRequestDto
import com.derekwinters.chores.data.network.dto.ThemeSaveRequestDto
import com.derekwinters.chores.data.network.dto.ThemeUpdateRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Household default theme management (issue #24) and personal theme preference (issue #25). The
 * 6 built-in themes (dark, light, charcoal, paper, pink, frog) come back from [getThemes] like
 * any other row; the real API has no `is_builtin` flag, so rename/delete of a protected theme is
 * only ever rejected server-side (surfaced as an [com.derekwinters.chores.data.network.ApiException]).
 *
 * Issue #156: also owns [resolvedTheme], the single source of truth for the live app-wide theme.
 * Previously [com.derekwinters.chores.ui.theme.AppThemeViewModel] fetched the resolved theme once
 * in its own `init {}`, constructed *above* the auth gate — so on a fresh install it ran while
 * unauthenticated, the fetch failed, and the theme stayed the M3 default ("paper-like") until the
 * app was restarted with a persisted session. Centralizing the refresh here (rather than a
 * one-off fix in that one call site) also fixes the same staleness for in-session theme changes:
 * [setPersonalTheme] and [setDefaultTheme] now refresh [resolvedTheme] themselves, so
 * [com.derekwinters.chores.ui.theme.ThemePreferenceViewModel] and
 * [com.derekwinters.chores.ui.theme.ThemeAdminViewModel] don't need to know about the shared
 * state at all.
 */
@Singleton
class ThemeRepository @Inject constructor(
    private val api: ChoresApi,
    sessionManager: SessionManager
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Null means "hardcoded fallback" — [com.derekwinters.chores.ui.theme.ChoresTheme] treats
     * null as MaterialTheme's own default (also the safe fallback while unauthenticated/offline). */
    private val _resolvedTheme = MutableStateFlow<ThemeOption?>(null)
    val resolvedTheme: StateFlow<ThemeOption?> = _resolvedTheme.asStateFlow()

    init {
        // Issue #156: re-resolve whenever auth state transitions to authenticated — covers both
        // a fresh login and a restored session — rather than only once at construction time.
        sessionManager.isAuthenticated
            .onEach { authenticated ->
                if (authenticated) refreshResolvedTheme() else _resolvedTheme.value = null
            }
            .launchIn(repositoryScope)
    }

    private suspend fun refreshResolvedTheme() {
        getCurrentTheme()
            .onSuccess { current -> _resolvedTheme.value = current.theme }
            .onFailure { _resolvedTheme.value = null }
    }

    suspend fun getThemes(): Result<List<ThemeOption>> =
        safeApiCall { api.getThemes() }.map { themes -> themes.map { it.toDomain() } }

    /**
     * Issue #24: create a new custom theme named [name] by copying [sourceTheme]'s 9 colors. The
     * real backend's create endpoint (`POST /v1/theme/save`) takes a name + full color set
     * directly — there's no server-side "copy from id" — so [sourceTheme]'s colors (already
     * loaded client-side) are resent as the new theme's palette.
     */
    suspend fun createTheme(name: String, sourceTheme: ThemeOption): Result<ThemeOption> =
        safeApiCall { api.saveTheme(ThemeSaveRequestDto(name, sourceTheme.toColorsDto())) }.map { it.toDomain() }

    /** Issue #24: renames a theme via the rename-specific endpoint. */
    suspend fun renameTheme(themeId: String, name: String): Result<ThemeOption> =
        safeApiCall { api.renameTheme(themeId, ThemeRenameRequestDto(name)) }.map { it.toDomain() }

    /**
     * Issue #130: replaces [themeId]'s palette with [colors]'s full 9-color set via
     * `PATCH /v1/theme/update/{theme_id}`. Colors are all-or-nothing per the real schema (see
     * [ThemeUpdateRequestDto]); the name is left unchanged (rename stays on [renameTheme]).
     * Built-in themes are protected server-side only, same as rename/delete.
     */
    suspend fun updateColors(themeId: String, colors: ThemeOption): Result<ThemeOption> =
        safeApiCall { api.updateTheme(themeId, ThemeUpdateRequestDto(colors = colors.toColorsDto())) }.map { it.toDomain() }

    /** Issue #24: non-built-in themes only; enforced server-side too. */
    suspend fun deleteTheme(themeId: String): Result<Unit> = safeApiCall { api.deleteTheme(themeId) }

    /** Issue #24: the current site-wide default theme (admin only). Not currently used by any screen. */
    suspend fun getDefaultTheme(): Result<ThemeOption> =
        safeApiCall { api.getDefaultTheme() }.map { it.toDomain() }

    /**
     * Issue #24: sets the household default theme. Issue #156: also refreshes [resolvedTheme] on
     * success so the live app theme updates immediately for callers with no personal override
     * (including the admin who just made this change), without needing a restart.
     */
    suspend fun setDefaultTheme(themeId: String): Result<ThemeOption> =
        safeApiCall { api.setDefaultTheme(themeId) }.map { it.toDomain() }
            .also { result -> if (result.isSuccess) refreshResolvedTheme() }

    /** Issue #25: resolved theme (personal override, else household default). */
    suspend fun getCurrentTheme(): Result<CurrentTheme> =
        safeApiCall { api.getCurrentTheme() }.map { it.toDomain() }

    /**
     * Issue #25: the household default's id/name (no colors), regardless of the caller's own
     * override — see [ThemeDefaultInfo] for why this (rather than [getCurrentTheme]) is the right
     * source for the Preferences screen's "Default (name)" label.
     */
    suspend fun getDefaultThemeInfo(): Result<ThemeDefaultInfo> =
        safeApiCall { api.getDefaultThemeInfo() }.map { it.toDomain() }

    /**
     * Issue #25: sets the caller's personal override to [themeId], or clears it back to the
     * household default when [themeId] is null (the "Default (household theme name)" tile). The
     * real API models clearing as a dedicated `DELETE /v1/theme/personal` call rather than a
     * sentinel id sent to the set endpoint. Issue #156: also refreshes [resolvedTheme] on success
     * so the live app theme updates immediately, without needing a restart.
     */
    suspend fun setPersonalTheme(themeId: String?): Result<Unit> =
        (
            if (themeId == null) {
                safeApiCall { api.clearPersonalTheme() }
            } else {
                safeApiCall { api.setPersonalTheme(themeId) }.map { }
            }
        ).also { result -> if (result.isSuccess) refreshResolvedTheme() }
}
