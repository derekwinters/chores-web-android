package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.GitHubReleaseDto

/**
 * In-memory [GitHubApi] test double (issue #35), following [FakeChoresApi]'s pattern: no real
 * I/O, so it's deterministic under `advanceUntilIdle()` when driven through `viewModelScope`.
 */
class FakeGitHubApi(
    private val releaseResult: GitHubReleaseDto? = null,
    private val releaseError: Throwable? = null
) : GitHubApi {

    var callCount: Int = 0
        private set
    var lastOwner: String? = null
        private set
    var lastRepo: String? = null
        private set

    override suspend fun getLatestRelease(owner: String, repo: String): GitHubReleaseDto {
        callCount++
        lastOwner = owner
        lastRepo = repo
        releaseError?.let { throw it }
        return releaseResult ?: error("FakeGitHubApi.releaseResult not configured")
    }
}
