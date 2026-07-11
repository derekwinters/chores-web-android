package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.BackendVersionDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #35: [ConfigRepository.getBackendVersion] wraps the backend's public `GET /version`
 * (chores-web-backend#27). It must surface a plain [Result.failure] (never throw) on a 404/
 * timeout/network error so [com.derekwinters.chores.ui.settings.SettingsViewModel] can fall back
 * to the "unknown" / "unsupported check" display without crashing.
 */
class ConfigRepositoryTest {

    @Test
    fun getBackendVersion_success_mapsToAvailable() = runTest {
        val api = FakeChoresApi(
            backendVersionResult = BackendVersionDto(
                version = "2.2.0",
                latest_version = "2.3.0",
                update_available = true,
                checked_at = "2026-07-10T12:00:00Z"
            )
        )
        val repository = ConfigRepository(api)

        val result = repository.getBackendVersion()

        assertTrue(result.isSuccess)
        val available = result.getOrThrow()
        assertEquals("2.2.0", available.version)
        assertEquals("2.3.0", available.latestVersion)
        assertTrue(available.updateAvailable)
        assertEquals("2026-07-10T12:00:00Z", available.checkedAt)
    }

    @Test
    fun getBackendVersion_404_returnsFailure_doesNotThrow() = runTest {
        val api = FakeChoresApi(backendVersionError = ApiException(404, "Not found"))
        val repository = ConfigRepository(api)

        val result = repository.getBackendVersion()

        assertTrue(result.isFailure)
    }

    @Test
    fun getBackendVersion_networkFailure_returnsFailure_doesNotThrow() = runTest {
        val api = FakeChoresApi(backendVersionError = java.io.IOException("offline"))
        val repository = ConfigRepository(api)

        val result = repository.getBackendVersion()

        assertTrue(result.isFailure)
    }

    @Test
    fun getBackendVersion_unconfiguredFake_stillReturnsFailure_neverThrows() = runTest {
        // Simulates any unexpected fake-not-configured/shape-mismatch failure mode: safeApiCall
        // must convert it to Result.failure rather than letting it escape.
        val api = FakeChoresApi()
        val repository = ConfigRepository(api)

        val result = repository.getBackendVersion()

        assertTrue(result.isFailure)
    }
}
