package com.derekwinters.chores.data.local

/**
 * Persists the last time+result of the client-side GitHub-releases check (issue #35) across
 * process restarts, so [com.derekwinters.chores.data.repository.AppVersionRepository] can
 * rate-limit calls to GitHub's unauthenticated 60-requests/hour/IP limit instead of hitting it on
 * every About-screen visit.
 */
interface VersionCheckCache {
    fun getLastCheckedAtMillis(): Long?
    fun getCachedLatestVersion(): String?
    fun save(checkedAtMillis: Long, latestVersion: String)
}
