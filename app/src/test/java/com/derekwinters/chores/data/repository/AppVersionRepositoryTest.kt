package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.local.FakeVersionCheckCache
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.network.FakeGitHubApi
import com.derekwinters.chores.data.network.dto.GitHubReleaseDto
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #35: [AppVersionRepository] is the client-side replacement for the old (incorrect)
 * backend-sourced "current app version" check — it compares `BuildConfig.VERSION_NAME` (passed
 * in by the caller, e.g. [com.derekwinters.chores.ui.settings.SettingsViewModel]) against this
 * app's own GitHub releases, with a small TTL cache so repeated Settings visits don't hit
 * GitHub's unauthenticated 60-requests/hour/IP limit.
 */
class AppVersionRepositoryTest {

    @Test
    fun checkForUpdate_latestNewerThanCurrent_reportsUpdateAvailable() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.2.0"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "1.1.0")

        assertTrue(result is AppVersionUiState.Checked)
        val checked = result as AppVersionUiState.Checked
        assertEquals("1.1.0", checked.currentVersion)
        assertEquals("1.2.0", checked.latestVersion)
        assertTrue(checked.updateAvailable)
    }

    @Test
    fun checkForUpdate_stripsLeadingVFromTagName() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v2.0.0"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "2.0.0") as AppVersionUiState.Checked

        assertEquals("2.0.0", result.latestVersion)
        assertFalse(result.updateAvailable)
    }

    @Test
    fun checkForUpdate_currentAtLeastAsNewAsLatest_noUpdateAvailable() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "1.1.0"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "1.2.0") as AppVersionUiState.Checked

        assertFalse(result.updateAvailable)
    }

    @Test
    fun checkForUpdate_numericVersionCompare_notLexicographic() = runTest {
        // A plain string compare would say "1.9.0" > "1.10.0" (lexicographic '9' > '1'); the
        // real (numeric, dot-segment) comparison must say the opposite.
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.10.0"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "1.9.0") as AppVersionUiState.Checked

        assertTrue(result.updateAvailable)
    }

    @Test
    fun checkForUpdate_reflectsCallerSuppliedCurrentVersion_notHardcoded() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v9.9.9"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "5.5.5") as AppVersionUiState.Checked

        assertEquals("5.5.5", result.currentVersion)
    }

    @Test
    fun checkForUpdate_secondCallWithinTtl_doesNotHitApiAgain() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.2.0"))
        val cache = FakeVersionCheckCache()
        val repository = AppVersionRepository(api, cache)

        repository.checkForUpdate(currentVersion = "1.1.0")
        repository.checkForUpdate(currentVersion = "1.1.0")

        assertEquals(1, api.callCount)
    }

    @Test
    fun checkForUpdate_staleCache_hitsApiAgain() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.2.0"))
        val staleTimestamp = System.currentTimeMillis() - (7 * 60 * 60 * 1000L) // 7h ago, > 6h TTL
        val cache = FakeVersionCheckCache(lastCheckedAtMillis = staleTimestamp, cachedLatestVersion = "1.1.9")
        val repository = AppVersionRepository(api, cache)

        repository.checkForUpdate(currentVersion = "1.1.0")

        assertEquals(1, api.callCount)
    }

    @Test
    fun checkForUpdate_forceRefresh_bypassesFreshCache() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.2.0"))
        val cache = FakeVersionCheckCache(lastCheckedAtMillis = System.currentTimeMillis(), cachedLatestVersion = "1.2.0")
        val repository = AppVersionRepository(api, cache)

        repository.checkForUpdate(currentVersion = "1.1.0", forceRefresh = true)

        assertEquals(1, api.callCount)
    }

    @Test
    fun checkForUpdate_networkFailureNoCache_returnsUnavailableWithCurrentVersionStillShown() = runTest {
        val api = FakeGitHubApi(releaseError = IOException("offline"))
        val repository = AppVersionRepository(api, FakeVersionCheckCache())

        val result = repository.checkForUpdate(currentVersion = "1.1.0")

        assertTrue(result is AppVersionUiState.Unavailable)
        assertEquals("1.1.0", (result as AppVersionUiState.Unavailable).currentVersion)
    }

    @Test
    fun checkForUpdate_networkFailureWithStaleCache_fallsBackToCachedLatest() = runTest {
        val api = FakeGitHubApi(releaseError = IOException("offline"))
        val staleTimestamp = System.currentTimeMillis() - (7 * 60 * 60 * 1000L)
        val cache = FakeVersionCheckCache(lastCheckedAtMillis = staleTimestamp, cachedLatestVersion = "1.1.9")
        val repository = AppVersionRepository(api, cache)

        val result = repository.checkForUpdate(currentVersion = "1.1.0")

        assertTrue(result is AppVersionUiState.Checked)
        assertEquals("1.1.9", (result as AppVersionUiState.Checked).latestVersion)
    }

    @Test
    fun checkForUpdate_successfulCheck_savesToCache() = runTest {
        val api = FakeGitHubApi(releaseResult = GitHubReleaseDto(tag_name = "v1.2.0"))
        val cache = FakeVersionCheckCache()
        val repository = AppVersionRepository(api, cache)

        repository.checkForUpdate(currentVersion = "1.1.0")

        assertEquals(1, cache.saveCallCount)
        assertEquals("1.2.0", cache.getCachedLatestVersion())
    }
}
