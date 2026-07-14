package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.AuthLogEntryDto
import com.derekwinters.chores.data.network.dto.BackendVersionDto
import com.derekwinters.chores.data.network.dto.ChoreCreateRequestDto
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.network.dto.ChoreUpdateRequestDto
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.network.dto.CreatePersonRequestDto
import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.DbStatusDto
import com.derekwinters.chores.data.network.dto.ImportResultDto
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.LogEntryDto
import com.derekwinters.chores.data.network.dto.NotificationDto
import com.derekwinters.chores.data.network.dto.NotificationPreferencesDto
import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.network.dto.UserStatsDto
import com.derekwinters.chores.data.network.dto.PointsLogEntryDto
import com.derekwinters.chores.data.network.dto.PointsLogPageDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.network.dto.ReassignRequestDto
import com.derekwinters.chores.data.network.dto.RedeemRequestDto
import com.derekwinters.chores.data.network.dto.RedemptionDto
import com.derekwinters.chores.data.network.dto.ResetPasswordRequestDto
import com.derekwinters.chores.data.network.dto.RetentionSettingsDto
import com.derekwinters.chores.data.network.dto.SetupRequestDto
import com.derekwinters.chores.data.network.dto.SetupStatusDto
import com.derekwinters.chores.data.network.dto.ThemeDefaultInfoDto
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.network.dto.ThemeRenameRequestDto
import com.derekwinters.chores.data.network.dto.ThemeSaveRequestDto
import com.derekwinters.chores.data.network.dto.ThemeUpdateRequestDto
import com.derekwinters.chores.data.network.dto.UpdatePersonRequestDto
import com.derekwinters.chores.data.network.dto.UpdatePointsLogRequestDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * chores-web backend API surface. Paths are relative to the placeholder Retrofit base URL and
 * are rewritten per-request by [BaseUrlInterceptor]; the `v1/` segment matches the backend's
 * `V1_PREFIX` (backend/app/main.py) applied to all routers except status/metrics.
 *
 * Started in issue #5 (login/chores/complete) and grown by the v1.0.0 feature-parity milestone
 * (issues #10-#25) as each screen's Impact Areas table calls for new endpoints; see each
 * function's KDoc for which issue introduced it.
 */
interface ChoresApi {

    // --- Auth (issue #5, extended by #10/#11) ---

    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): LoginResponseDto

    @GET("v1/auth/me")
    suspend fun getCurrentUser(): UserInfoDto

    /** Issue #10: manual Logout action in the user menu. */
    @POST("v1/auth/logout")
    suspend fun logout()

    /** Issue #11: first-run setup gate. */
    @GET("v1/auth/setup-status")
    suspend fun getSetupStatus(): SetupStatusDto

    /** Issue #11: creates the first (admin) user; response shape matches login's. */
    @POST("v1/auth/setup")
    suspend fun setup(@Body request: SetupRequestDto): LoginResponseDto

    /**
     * Issue #11: forced-password-reset flow. [authorization] carries the one-time `reset_token`
     * (e.g. "Bearer <token>") rather than the normal session token, since login hasn't
     * succeeded yet.
     */
    @PUT("v1/auth/password/reset")
    suspend fun resetPassword(
        @Header("Authorization") authorization: String,
        @Body request: ResetPasswordRequestDto
    )

    /** Issue #11: DB-readiness gate polled before rendering the authenticated app shell. */
    @GET("status/db-status")
    suspend fun getDbStatus(): DbStatusDto

    // --- Chores (issue #5, extended by #13/#14/#15/#16) ---

    @GET("v1/chores")
    suspend fun getChores(): List<ChoreDto>

    @POST("v1/chores/{id}/complete")
    suspend fun completeChore(
        @Path("id") choreId: Int,
        @Body request: CompleteChoreRequestDto
    ): ChoreDto

    /** Issue #15: moves to next cycle without awarding points. */
    @POST("v1/chores/{id}/skip")
    suspend fun skipChore(@Path("id") choreId: Int): ChoreDto

    /** Issue #15: marks a not-yet-due chore due now. */
    @POST("v1/chores/{id}/mark-due")
    suspend fun markChoreDue(@Path("id") choreId: Int): ChoreDto

    /** Issue #15: also removes all points history for this chore server-side. */
    @DELETE("v1/chores/{id}")
    suspend fun deleteChore(@Path("id") choreId: Int)

    /** Issue #16. */
    @POST("v1/chores")
    suspend fun createChore(@Body request: ChoreCreateRequestDto): ChoreDto

    /** Issue #16. */
    @PUT("v1/chores/{id}")
    suspend fun updateChore(@Path("id") choreId: Int, @Body request: ChoreUpdateRequestDto): ChoreDto

    /** Issue #16: editing an `open` chore's assignee field IS reassignment. */
    @POST("v1/chores/{id}/reassign")
    suspend fun reassignChore(@Path("id") choreId: Int, @Body request: ReassignRequestDto): ChoreDto

    // --- People (issues #12, #17, #18) ---

    @GET("v1/people")
    suspend fun getPeople(): List<PersonDto>

    /**
     * Issue #12: per-person rolling point totals for the Dashboard progress bars. Each entry is
     * keyed by `person` (username string), not a person id — see [PointsSummaryDto].
     */
    @GET("v1/points/summary")
    suspend fun getPointsSummary(): List<PointsSummaryDto>

    /**
     * Issue #17. Keyed by the person's **username** (a string path segment), not their numeric
     * person id — matches chores-web's `GET /v1/points/stats/{person}`.
     */
    @GET("v1/points/stats/{person}")
    suspend fun getPersonStats(@Path("person") username: String): UserStatsDto

    /** Issue #18. */
    @POST("v1/people")
    suspend fun createPerson(@Body request: CreatePersonRequestDto): PersonDto

    /** Issue #18: chores-web's `PersonUpdate` route is a PUT (partial update), not a PATCH. */
    @PUT("v1/people/{person_id}")
    suspend fun updatePerson(@Path("person_id") personId: Int, @Body request: UpdatePersonRequestDto): PersonDto

    /** Issue #18. */
    @DELETE("v1/people/{person_id}")
    suspend fun deletePerson(@Path("person_id") personId: Int)

    /**
     * Issue #17. Returns the updated `PersonOut`, not a stats payload — callers should re-fetch
     * [getPersonStats] for the display balance rather than trusting this response body.
     */
    @POST("v1/people/{person_id}/redeem")
    suspend fun redeemPoints(@Path("person_id") personId: Int, @Body request: RedeemRequestDto): PersonDto

    /** Issue #17. */
    @GET("v1/people/{person_id}/redemptions")
    suspend fun getRedemptions(@Path("person_id") personId: Int): List<RedemptionDto>

    // --- Activity Log (issues #15, #17, #19, #22) ---

    /**
     * Issue #19, reused (with a subset of filters) by issues #15/#17's deep-linked views. Returns
     * a bare array (no server-side pagination envelope) — see the real OpenAPI spec's
     * `get_log_v1_log_get` response, `type: array` of `ChoreLogOut`; any "N results"/paging UI is
     * entirely client-side over this full filtered list. Note [choreId] is typed as a `string`
     * query param in the real API despite being conceptually an integer id.
     */
    @GET("v1/log")
    suspend fun getLog(
        @Query("person") person: String? = null,
        @Query("chore_id") choreId: String? = null,
        @Query("action") action: String? = null,
        @Query("actions") actions: List<String>? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<LogEntryDto>

    /** Issue #22: current log-retention setting, a real standalone backend resource. */
    @GET("v1/log/retention")
    suspend fun getRetention(): RetentionSettingsDto

    /** Issue #22: sets the log-retention setting; the backend returns the (presumably same) new value. */
    @POST("v1/log/retention")
    suspend fun setRetention(@Body request: RetentionSettingsDto): RetentionSettingsDto

    // --- Auth Event Log (issue #21) ---

    @GET("v1/auth/log")
    suspend fun getAuthLog(
        @Query("username") username: String? = null,
        @Query("action") action: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<AuthLogEntryDto>

    // --- Notifications (issue #43, chores-web-backend#39) ---

    /**
     * Issue #43: the caller's notifications, newest first. The server owns delivery state — the
     * first time a notification is returned here it sets `delivered_at` server-side, which is why
     * the once-per-item posting guarantee relies on a local posted-ids record rather than this
     * flag (see [com.derekwinters.chores.notifications.NotificationPollWorker]).
     *
     * @param since only notifications created strictly after this ISO-8601 instant (null = all).
     * @param includeDismissed when false (the poll worker's default) dismissed rows are excluded;
     *   pre-dismissed rows are never returned in either mode.
     */
    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("since") since: String? = null,
        @Query("include_dismissed") includeDismissed: Boolean = false
    ): List<NotificationDto>

    /**
     * Issue #43: acknowledge one notification (fired on notification tap, never merely on
     * posting). Idempotent server-side. Returns the updated `NotificationOut`, which this app
     * ignores — the tap handler is fire-and-forget.
     */
    @POST("v1/notifications/{id}/ack")
    suspend fun ackNotification(@Path("id") notificationId: Int)

    /**
     * Issue #44 (chores-web-backend#39): the caller's per-type notification-enablement map (an
     * account-level preference, unlike the device-local poll interval). Bare `type -> bool` JSON
     * object; every known type is present, absent-in-DB reported as enabled (backend#38).
     */
    @GET("v1/notifications/preferences")
    suspend fun getNotificationPreferences(): NotificationPreferencesDto

    /**
     * Issue #44: upsert the caller's per-type preferences. Keys naming unknown types are ignored
     * server-side; the response echoes the resulting map covering every known type.
     */
    @PUT("v1/notifications/preferences")
    suspend fun updateNotificationPreferences(
        @Body preferences: NotificationPreferencesDto
    ): NotificationPreferencesDto

    // --- Config / Settings (issues #12, #20, #22) ---

    @GET("v1/config")
    suspend fun getConfig(): ConfigDto

    @PUT("v1/config")
    suspend fun updateConfig(@Body request: ConfigDto): ConfigDto

    /**
     * Issue #35 (chores-web-backend#27): the backend's own public version/update-status — no
     * auth required. Distinct from this app's own client-side GitHub-releases check
     * ([com.derekwinters.chores.data.network.GitHubApi]), which the backend has no way to compute
     * since it doesn't know what Android app version is actually deployed. Replaces the removed
     * `/v1/config/updates/status` + `/v1/config/updates/check` endpoints, which (incorrectly)
     * doubled as this app's own version data.
     */
    @GET("version")
    suspend fun getBackendVersion(): BackendVersionDto

    // --- Data export/import (issue #22) ---

    /** Raw JSON backup; written to a file untouched rather than parsed into a typed model. */
    @GET("v1/export/config")
    suspend fun exportConfig(): ResponseBody

    @POST("v1/import/config")
    suspend fun importConfig(@Body body: RequestBody): ImportResultDto

    // --- Admin Points Log editor (issue #23) ---

    /** Offset-based pagination — matches `list_points_log_v1_admin_db_points_log_get` exactly. */
    @GET("v1/admin/db/points-log")
    suspend fun getPointsLog(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PointsLogPageDto

    @PATCH("v1/admin/db/points-log/{id}")
    suspend fun updatePointsLogEntry(
        @Path("id") entryId: Int,
        @Body request: UpdatePointsLogRequestDto
    ): PointsLogEntryDto

    @DELETE("v1/admin/db/points-log/{id}")
    suspend fun deletePointsLogEntry(@Path("id") entryId: Int)

    /**
     * `GET /v1/points/{person}`: per-person raw points history, keyed by username. Not yet wired
     * into any screen — this is a possible future feature (e.g. a per-person history view
     * alongside the existing [getPersonStats] summary), exposed here in the correct shape so it's
     * ready when that UI is built.
     */
    @GET("v1/points/{person}")
    suspend fun getPersonPointsHistory(@Path("person") username: String): List<PointsLogEntryDto>

    // --- Theming (issues #24, #25) ---

    /** Issue #24: full theme catalog (6 built-ins + custom) for admin management and issue #25's picker. */
    @GET("v1/theme/list")
    suspend fun getThemes(): List<ThemeDto>

    /**
     * Issue #24: create a new custom theme. The real backend has no "copy an existing theme by
     * id" endpoint — [request] must already carry the full 9-color palette, which
     * [com.derekwinters.chores.data.repository.ThemeRepository.createTheme] fills in client-side
     * from whichever existing theme the admin picked as a starting point.
     */
    @POST("v1/theme/save")
    suspend fun saveTheme(@Body request: ThemeSaveRequestDto): ThemeDto

    /**
     * Issue #24: partial update (name and/or a full replacement color set). Modeled here to match
     * the real API but not currently called by any screen — the admin edit dialog only renames,
     * via [renameTheme] — ready for if in-app color editing is added later.
     */
    @PATCH("v1/theme/update/{theme_id}")
    suspend fun updateTheme(@Path("theme_id") themeId: String, @Body request: ThemeUpdateRequestDto): ThemeDto

    /**
     * Issue #24: rename-only endpoint used by the admin edit dialog, whose only editable field is
     * the theme name.
     */
    @PATCH("v1/theme/rename/{theme_id}")
    suspend fun renameTheme(@Path("theme_id") themeId: String, @Body request: ThemeRenameRequestDto): ThemeDto

    /** Issue #24: non-built-in themes only; enforced server-side (the real API exposes no `is_builtin` flag to gate on client-side). */
    @DELETE("v1/theme/delete/{theme_id}")
    suspend fun deleteTheme(@Path("theme_id") themeId: String)

    /** Issue #24: the current site-wide default theme (admin only). Not currently wired to any screen. */
    @GET("v1/theme/default")
    suspend fun getDefaultTheme(): ThemeDto

    /** Issue #24: sets the household default theme (admin only). */
    @PUT("v1/theme/default/{theme_id}")
    suspend fun setDefaultTheme(@Path("theme_id") themeId: String): ThemeDto

    /** Issue #25: resolved theme (personal override, else household default), plus whether it's an override. */
    @GET("v1/theme/current")
    suspend fun getCurrentTheme(): CurrentThemeDto

    /**
     * Issue #25: the household default's id/name only (no colors) — accessible to all
     * authenticated users, unlike [getDefaultTheme] which is admin-only. Lets the Preferences
     * screen correctly label the "Default (name)" tile even while a *different* theme is active
     * as the caller's own personal override.
     */
    @GET("v1/theme/default-info")
    suspend fun getDefaultThemeInfo(): ThemeDefaultInfoDto

    /** Issue #25: clears the caller's personal theme override, reverting them to the household default. */
    @DELETE("v1/theme/personal")
    suspend fun clearPersonalTheme()

    /** Issue #25: sets the caller's personal theme override to [themeId]. */
    @POST("v1/theme/set/{theme_id}")
    suspend fun setPersonalTheme(@Path("theme_id") themeId: String): ThemeDto
}
