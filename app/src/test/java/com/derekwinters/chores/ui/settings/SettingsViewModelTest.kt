package com.derekwinters.chores.ui.settings

import com.derekwinters.chores.BuildConfig
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.local.FakeVersionCheckCache
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.FakeGitHubApi
import com.derekwinters.chores.data.network.dto.BackendVersionDto
import com.derekwinters.chores.data.network.dto.ConfigDto
import com.derekwinters.chores.data.network.dto.GitHubReleaseDto
import com.derekwinters.chores.data.repository.AppVersionRepository
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Issue #20 behaviors: load/save the shared config across the four Settings forms.
 *
 * Issue #35: [SettingsViewModel.appVersionState] now comes from a client-side
 * [AppVersionRepository] (GitHub releases, compared against [BuildConfig.VERSION_NAME]) instead
 * of the backend, and [SettingsViewModel.backendVersionState] is a new, separate state sourced
 * from [ConfigRepository.getBackendVersion] with a graceful "Unsupported" fallback on failure.
 */
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        api: FakeChoresApi = FakeChoresApi(configResult = ConfigDto()),
        gitHubApi: FakeGitHubApi = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v${BuildConfig.VERSION_NAME}")),
        cache: FakeVersionCheckCache = FakeVersionCheckCache()
    ): SettingsViewModel = SettingsViewModel(
        ConfigRepository(api),
        AppVersionRepository(gitHubApi, cache)
    )

    @Test
    fun load_populatesConfig() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel(api = FakeChoresApi(configResult = ConfigDto(title = "My Chores")))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals("My Chores", (state as UiState.Success).data.appTitle)
    }

    @Test
    fun save_success_updatesStateAndUiState() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.save(ConfigDto(title = "New Title").toDomain())
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.saveState.value)
        assertEquals("New Title", (viewModel.uiState.value as UiState.Success).data.appTitle)
    }

    @Test
    fun load_appVersion_usesBuildConfigVersionName_andDetectsUpdate() = runTest(mainDispatcherRule.testDispatcher) {
        val gitHubApi = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v999.0.0"))
        val viewModel = viewModel(gitHubApi = gitHubApi)
        advanceUntilIdle()

        val state = viewModel.appVersionState.value
        assertTrue(state is AppVersionUiState.Checked)
        val checked = state as AppVersionUiState.Checked
        assertEquals(BuildConfig.VERSION_NAME, checked.currentVersion)
        assertEquals("999.0.0", checked.latestVersion)
        assertTrue(checked.updateAvailable)
    }

    @Test
    fun load_appVersion_gitHubCheckFails_reportsUnavailableButKeepsCurrentVersion() = runTest(mainDispatcherRule.testDispatcher) {
        val gitHubApi = FakeGitHubApi(releaseError = ApiException(-1, "offline"))
        val viewModel = viewModel(gitHubApi = gitHubApi)
        advanceUntilIdle()

        val state = viewModel.appVersionState.value
        assertTrue(state is AppVersionUiState.Unavailable)
        assertEquals(BuildConfig.VERSION_NAME, (state as AppVersionUiState.Unavailable).currentVersion)
    }

    @Test
    fun checkAppVersionNow_triggersAnotherGitHubCheck() = runTest(mainDispatcherRule.testDispatcher) {
        val gitHubApi = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.0.0"))
        val viewModel = viewModel(gitHubApi = gitHubApi)
        advanceUntilIdle()
        val callsAfterLoad = gitHubApi.callCount

        viewModel.checkAppVersionNow()
        advanceUntilIdle()

        assertTrue(gitHubApi.callCount > callsAfterLoad)
    }

    @Test
    fun load_backendVersion_success_populatesAvailableState() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            configResult = ConfigDto(),
            backendVersionResult = BackendVersionDto(version = "2.2.0", latest_version = "2.3.0", update_available = true)
        )
        val viewModel = viewModel(api = api)
        advanceUntilIdle()

        val state = viewModel.backendVersionState.value
        assertTrue(state is BackendVersionUiState.Available)
        val available = state as BackendVersionUiState.Available
        assertEquals("2.2.0", available.version)
        assertEquals("2.3.0", available.latestVersion)
        assertTrue(available.updateAvailable)
    }

    @Test
    fun load_backendVersion_failure_fallsBackToUnsupported() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            configResult = ConfigDto(),
            backendVersionError = ApiException(404, "Not found")
        )
        val viewModel = viewModel(api = api)
        advanceUntilIdle()

        assertEquals(BackendVersionUiState.Unsupported, viewModel.backendVersionState.value)
    }

    @Test
    fun load_backendVersion_notConfigured_defaultsToUnsupported_doesNotCrash() = runTest(mainDispatcherRule.testDispatcher) {
        // FakeChoresApi() with no backendVersionResult/backendVersionError configured throws
        // IllegalStateException internally — safeApiCall must convert that to Result.failure
        // rather than crashing this test (mirrors an unexpected real-world response shape).
        val viewModel = viewModel(api = FakeChoresApi(configResult = ConfigDto()))
        advanceUntilIdle()

        assertEquals(BackendVersionUiState.Unsupported, viewModel.backendVersionState.value)
    }
}
