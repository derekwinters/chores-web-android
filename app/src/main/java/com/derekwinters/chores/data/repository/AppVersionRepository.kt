package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.local.VersionCheckCache
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.isVersionNewer
import com.derekwinters.chores.data.network.GitHubApi
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #35: client-side "is a newer app release available" check against this app's own GitHub
 * releases, replacing the earlier (incorrect) use of the backend's update-check endpoints for
 * this purpose — the backend has no way to know what version of the Android app is deployed;
 * only the app itself does, via `BuildConfig.VERSION_NAME`.
 *
 * Rate-limited via [cache]: GitHub's unauthenticated API allows only 60 requests/hour/IP, so a
 * fresh network check only runs if the cached result is older than [CACHE_TTL_MILLIS] (or
 * [forceRefresh] is set, e.g. from the About screen's "Check Now" action). On a network failure,
 * a stale cached result (if any) is still surfaced rather than discarded, so a transient offline
 * blip doesn't erase a previously-known "update available" signal.
 */
@Singleton
class AppVersionRepository @Inject constructor(
    private val gitHubApi: GitHubApi,
    private val cache: VersionCheckCache
) {

    suspend fun checkForUpdate(currentVersion: String, forceRefresh: Boolean = false): AppVersionUiState {
        val now = System.currentTimeMillis()
        val lastCheckedAt = cache.getLastCheckedAtMillis()
        val cachedLatest = cache.getCachedLatestVersion()

        // Null-checking lastCheckedAt directly in this condition (rather than via a separate
        // Boolean) keeps it smart-cast to non-null Long throughout the block below.
        if (!forceRefresh && lastCheckedAt != null && cachedLatest != null &&
            (now - lastCheckedAt) < CACHE_TTL_MILLIS
        ) {
            return AppVersionUiState.Checked(
                currentVersion = currentVersion,
                latestVersion = cachedLatest,
                updateAvailable = isVersionNewer(cachedLatest, currentVersion),
                lastCheckedAtMillis = lastCheckedAt
            )
        }

        return safeApiCall { gitHubApi.getLatestRelease(owner = REPO_OWNER, repo = REPO_NAME) }
            .map { dto ->
                val latest = dto.tag_name.removePrefix("v")
                cache.save(now, latest)
                AppVersionUiState.Checked(
                    currentVersion = currentVersion,
                    latestVersion = latest,
                    updateAvailable = isVersionNewer(latest, currentVersion),
                    lastCheckedAtMillis = now
                )
            }
            .getOrElse {
                if (cachedLatest != null) {
                    AppVersionUiState.Checked(
                        currentVersion = currentVersion,
                        latestVersion = cachedLatest,
                        updateAvailable = isVersionNewer(cachedLatest, currentVersion),
                        lastCheckedAtMillis = lastCheckedAt ?: now
                    )
                } else {
                    AppVersionUiState.Unavailable(currentVersion = currentVersion)
                }
            }
    }

    private companion object {
        const val REPO_OWNER = "derekwinters"
        const val REPO_NAME = "chores-web-android"

        // 6 hours: comfortably avoids GitHub's 60 req/hour/IP unauthenticated limit even across
        // several installs sharing one IP (e.g. a household's devices on the same LAN), while
        // still refreshing "is there a new release" within the same day for anyone who opens the
        // About screen.
        const val CACHE_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
