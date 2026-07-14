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

/**
 * In-memory [ChoresApi] test double. Its suspend functions never perform real I/O (no real
 * thread hop), so ViewModel tests driving it through `viewModelScope` on a shared
 * `StandardTestDispatcher` (see MainDispatcherRule) stay fully deterministic under
 * `advanceUntilIdle()` — unlike a real MockWebServer-backed call, whose completion crosses a
 * real background thread. MockWebServer is used instead for the repository/interceptor-level
 * tests, where the suspend call is awaited directly rather than through a fire-and-forget
 * `launch`.
 *
 * Only the chore/login surface (issue #5) is configurable via constructor params, since that's
 * all today's ViewModel tests exercise; every endpoint added by the v1.0.0 milestone
 * (issues #10-#25) has a trivial "not configured" stub here purely so this class keeps compiling
 * against the full [ChoresApi] interface.
 */
class FakeChoresApi(
    private val loginResult: LoginResponseDto? = null,
    private val loginError: Throwable? = null,
    private val choresResult: List<ChoreDto> = emptyList(),
    private val choresError: Throwable? = null,
    private val completeResult: ChoreDto? = null,
    private val completeError: Throwable? = null,
    private val currentUserResult: UserInfoDto = UserInfoDto("", false),
    private val currentUserError: Throwable? = null,
    private val setupStatusResult: SetupStatusDto = SetupStatusDto(setup_needed = false),
    private val dbStatusResult: DbStatusDto = DbStatusDto(status = "ready"),
    private val pointsSummaryResult: List<PointsSummaryDto> = emptyList(),
    private val peopleResult: List<PersonDto> = emptyList(),
    private val configResult: ConfigDto = ConfigDto(),
    private val backendVersionResult: BackendVersionDto? = null,
    private val backendVersionError: Throwable? = null,
    private val skipResult: ChoreDto? = null,
    private val markDueResult: ChoreDto? = null,
    private val createChoreResult: ChoreDto? = null,
    private val updateChoreResult: ChoreDto? = null,
    private val personStatsResult: UserStatsDto = UserStatsDto(),
    private val redemptionsResult: List<RedemptionDto> = emptyList(),
    private val redeemResult: PersonDto? = null,
    private val logResult: List<LogEntryDto> = emptyList(),
    private val notificationsResult: List<NotificationDto> = emptyList(),
    private val notificationsError: Throwable? = null,
    private val notificationPreferencesResult: NotificationPreferencesDto = emptyMap(),
    private val notificationPreferencesError: Throwable? = null,
    private val updateNotificationPreferencesResult: NotificationPreferencesDto? = null,
    private val updateNotificationPreferencesError: Throwable? = null,
    private val retentionResult: RetentionSettingsDto = RetentionSettingsDto(retention_days = 90),
    private val createPersonResult: PersonDto? = null,
    private val updatePersonResult: PersonDto? = null,
    private val updatePersonError: Throwable? = null,
    private val authLogResult: List<AuthLogEntryDto> = emptyList(),
    private val importConfigResult: ImportResultDto? = null,
    private val pointsLogResult: PointsLogPageDto = PointsLogPageDto(),
    private val personPointsHistoryResult: List<PointsLogEntryDto> = emptyList(),
    private val updatePointsLogResult: PointsLogEntryDto? = null,
    private val themesResult: List<ThemeDto> = emptyList(),
    private val saveThemeResult: ThemeDto? = null,
    private val updateThemeResult: ThemeDto? = null,
    private val renameThemeResult: ThemeDto? = null,
    private val defaultThemeResult: ThemeDto? = null,
    private val setDefaultThemeResult: ThemeDto? = null,
    private val currentThemeResult: CurrentThemeDto? = null,
    private val defaultThemeInfoResult: ThemeDefaultInfoDto? = null,
    private val setPersonalThemeResult: ThemeDto? = null
) : ChoresApi {

    var lastCompleteChoreId: Int? = null
        private set
    var lastCompleteRequest: CompleteChoreRequestDto? = null
        private set
    var lastSkipChoreId: Int? = null
        private set
    var lastMarkDueChoreId: Int? = null
        private set
    var lastDeleteChoreId: Int? = null
        private set
    var lastCreateChoreRequest: ChoreCreateRequestDto? = null
        private set
    var lastUpdateChoreId: Int? = null
        private set
    var lastUpdateChoreRequest: ChoreUpdateRequestDto? = null
        private set
    var lastCreatePersonRequest: CreatePersonRequestDto? = null
        private set
    var lastUpdatePersonId: Int? = null
        private set
    var lastDeletePersonId: Int? = null
        private set

    override suspend fun login(request: LoginRequestDto): LoginResponseDto {
        loginError?.let { throw it }
        return loginResult ?: error("FakeChoresApi.loginResult not configured")
    }

    override suspend fun getCurrentUser(): UserInfoDto {
        currentUserError?.let { throw it }
        return currentUserResult
    }

    override suspend fun logout() = Unit

    override suspend fun getSetupStatus(): SetupStatusDto = setupStatusResult

    override suspend fun setup(request: SetupRequestDto): LoginResponseDto =
        loginResult ?: error("FakeChoresApi.loginResult not configured")

    override suspend fun resetPassword(authorization: String, request: ResetPasswordRequestDto) = Unit

    override suspend fun getDbStatus(): DbStatusDto = dbStatusResult

    override suspend fun getChores(): List<ChoreDto> {
        choresError?.let { throw it }
        return choresResult
    }

    override suspend fun completeChore(choreId: Int, request: CompleteChoreRequestDto): ChoreDto {
        lastCompleteChoreId = choreId
        lastCompleteRequest = request
        completeError?.let { throw it }
        return completeResult ?: error("FakeChoresApi.completeResult not configured")
    }

    override suspend fun skipChore(choreId: Int): ChoreDto {
        lastSkipChoreId = choreId
        return skipResult ?: error("FakeChoresApi.skipResult not configured")
    }

    override suspend fun markChoreDue(choreId: Int): ChoreDto {
        lastMarkDueChoreId = choreId
        return markDueResult ?: error("FakeChoresApi.markDueResult not configured")
    }

    override suspend fun deleteChore(choreId: Int) {
        lastDeleteChoreId = choreId
    }

    override suspend fun createChore(request: ChoreCreateRequestDto): ChoreDto {
        lastCreateChoreRequest = request
        return createChoreResult ?: error("FakeChoresApi.createChoreResult not configured")
    }

    override suspend fun updateChore(choreId: Int, request: ChoreUpdateRequestDto): ChoreDto {
        lastUpdateChoreId = choreId
        lastUpdateChoreRequest = request
        return updateChoreResult ?: error("FakeChoresApi.updateChoreResult not configured")
    }

    override suspend fun reassignChore(choreId: Int, request: ReassignRequestDto): ChoreDto =
        error("not configured")

    override suspend fun getPeople(): List<PersonDto> = peopleResult

    override suspend fun getPointsSummary(): List<PointsSummaryDto> = pointsSummaryResult

    override suspend fun getPersonStats(username: String): UserStatsDto = personStatsResult

    override suspend fun createPerson(request: CreatePersonRequestDto): PersonDto {
        lastCreatePersonRequest = request
        return createPersonResult ?: error("FakeChoresApi.createPersonResult not configured")
    }

    override suspend fun updatePerson(personId: Int, request: UpdatePersonRequestDto): PersonDto {
        lastUpdatePersonId = personId
        updatePersonError?.let { throw it }
        return updatePersonResult ?: error("FakeChoresApi.updatePersonResult not configured")
    }

    override suspend fun deletePerson(personId: Int) {
        lastDeletePersonId = personId
    }

    override suspend fun redeemPoints(personId: Int, request: RedeemRequestDto): PersonDto =
        redeemResult ?: error("FakeChoresApi.redeemResult not configured")

    override suspend fun getRedemptions(personId: Int): List<RedemptionDto> = redemptionsResult

    var lastGetLogParams: List<Any?>? = null
        private set

    override suspend fun getLog(
        person: String?,
        choreId: String?,
        action: String?,
        actions: List<String>?,
        startDate: String?,
        endDate: String?
    ): List<LogEntryDto> {
        lastGetLogParams = listOf(person, choreId, action, actions, startDate, endDate)
        return logResult
    }

    var lastSetRetentionRequest: RetentionSettingsDto? = null
        private set

    override suspend fun getRetention(): RetentionSettingsDto = retentionResult

    override suspend fun setRetention(request: RetentionSettingsDto): RetentionSettingsDto {
        lastSetRetentionRequest = request
        return request
    }

    override suspend fun getAuthLog(
        username: String?,
        action: String?,
        startDate: String?,
        endDate: String?
    ): List<AuthLogEntryDto> = authLogResult

    var lastGetNotificationsSince: String? = null
        private set
    var lastGetNotificationsIncludeDismissed: Boolean? = null
        private set
    val ackedNotificationIds: MutableList<Int> = mutableListOf()

    override suspend fun getNotifications(since: String?, includeDismissed: Boolean): List<NotificationDto> {
        lastGetNotificationsSince = since
        lastGetNotificationsIncludeDismissed = includeDismissed
        notificationsError?.let { throw it }
        return notificationsResult
    }

    override suspend fun ackNotification(notificationId: Int) {
        ackedNotificationIds.add(notificationId)
    }

    var lastUpdatedNotificationPreferences: NotificationPreferencesDto? = null
        private set

    override suspend fun getNotificationPreferences(): NotificationPreferencesDto {
        notificationPreferencesError?.let { throw it }
        return notificationPreferencesResult
    }

    override suspend fun updateNotificationPreferences(
        preferences: NotificationPreferencesDto
    ): NotificationPreferencesDto {
        lastUpdatedNotificationPreferences = preferences
        updateNotificationPreferencesError?.let { throw it }
        return updateNotificationPreferencesResult ?: preferences
    }

    override suspend fun getConfig(): ConfigDto = configResult

    override suspend fun updateConfig(request: ConfigDto): ConfigDto = request

    override suspend fun getBackendVersion(): BackendVersionDto {
        backendVersionError?.let { throw it }
        return backendVersionResult ?: error("FakeChoresApi.backendVersionResult not configured")
    }

    override suspend fun exportConfig(): ResponseBody = error("not configured")

    override suspend fun importConfig(body: RequestBody): ImportResultDto =
        importConfigResult ?: error("FakeChoresApi.importConfigResult not configured")

    var lastGetPointsLogLimit: Int? = null
        private set
    var lastGetPointsLogOffset: Int? = null
        private set

    override suspend fun getPointsLog(limit: Int, offset: Int): PointsLogPageDto {
        lastGetPointsLogLimit = limit
        lastGetPointsLogOffset = offset
        return pointsLogResult
    }

    var lastUpdatePointsLogEntryId: Int? = null
        private set
    var lastUpdatePointsLogRequest: UpdatePointsLogRequestDto? = null
        private set
    var lastDeletePointsLogEntryId: Int? = null
        private set

    override suspend fun updatePointsLogEntry(
        entryId: Int,
        request: UpdatePointsLogRequestDto
    ): PointsLogEntryDto {
        lastUpdatePointsLogEntryId = entryId
        lastUpdatePointsLogRequest = request
        return updatePointsLogResult ?: error("FakeChoresApi.updatePointsLogResult not configured")
    }

    override suspend fun deletePointsLogEntry(entryId: Int) {
        lastDeletePointsLogEntryId = entryId
    }

    override suspend fun getPersonPointsHistory(username: String): List<PointsLogEntryDto> =
        personPointsHistoryResult

    var lastSaveThemeRequest: ThemeSaveRequestDto? = null
        private set
    var lastUpdateThemeId: String? = null
        private set
    var lastUpdateThemeRequest: ThemeUpdateRequestDto? = null
        private set
    var lastRenameThemeId: String? = null
        private set
    var lastRenameThemeRequest: ThemeRenameRequestDto? = null
        private set
    var lastDeleteThemeId: String? = null
        private set
    var lastSetDefaultThemeId: String? = null
        private set
    var lastSetPersonalThemeId: String? = null
        private set
    var lastClearPersonalThemeCalled: Boolean = false
        private set

    override suspend fun getThemes(): List<ThemeDto> = themesResult

    override suspend fun saveTheme(request: ThemeSaveRequestDto): ThemeDto {
        lastSaveThemeRequest = request
        return saveThemeResult ?: error("FakeChoresApi.saveThemeResult not configured")
    }

    override suspend fun updateTheme(themeId: String, request: ThemeUpdateRequestDto): ThemeDto {
        lastUpdateThemeId = themeId
        lastUpdateThemeRequest = request
        return updateThemeResult ?: error("FakeChoresApi.updateThemeResult not configured")
    }

    override suspend fun renameTheme(themeId: String, request: ThemeRenameRequestDto): ThemeDto {
        lastRenameThemeId = themeId
        lastRenameThemeRequest = request
        return renameThemeResult ?: error("FakeChoresApi.renameThemeResult not configured")
    }

    override suspend fun deleteTheme(themeId: String) {
        lastDeleteThemeId = themeId
    }

    override suspend fun getDefaultTheme(): ThemeDto =
        defaultThemeResult ?: error("FakeChoresApi.defaultThemeResult not configured")

    override suspend fun setDefaultTheme(themeId: String): ThemeDto {
        lastSetDefaultThemeId = themeId
        return setDefaultThemeResult ?: error("FakeChoresApi.setDefaultThemeResult not configured")
    }

    override suspend fun getCurrentTheme(): CurrentThemeDto =
        currentThemeResult ?: error("FakeChoresApi.currentThemeResult not configured")

    override suspend fun getDefaultThemeInfo(): ThemeDefaultInfoDto =
        defaultThemeInfoResult ?: error("FakeChoresApi.defaultThemeInfoResult not configured")

    override suspend fun clearPersonalTheme() {
        lastClearPersonalThemeCalled = true
    }

    override suspend fun setPersonalTheme(themeId: String): ThemeDto {
        lastSetPersonalThemeId = themeId
        return setPersonalThemeResult ?: error("FakeChoresApi.setPersonalThemeResult not configured")
    }
}
