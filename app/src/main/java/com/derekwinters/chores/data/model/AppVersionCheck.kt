package com.derekwinters.chores.data.model

/**
 * Client-side "is a newer app release available" check against this app's own GitHub releases
 * (issue #35) — replaces the earlier (incorrect) reliance on the backend's update-check
 * endpoints, which only ever reflected the *backend's* own version, never this app's. Only the
 * app itself knows what version of itself is running
 * ([com.derekwinters.chores.BuildConfig.VERSION_NAME]), so this check is entirely client-side
 * against `GET /repos/derekwinters/chores-web-android/releases/latest`.
 *
 * See [com.derekwinters.chores.data.model.BackendVersionUiState] for the separate, backend-owned
 * counterpart.
 */
sealed interface AppVersionUiState {
    data object Loading : AppVersionUiState

    data class Checked(
        val currentVersion: String,
        val latestVersion: String,
        val updateAvailable: Boolean,
        val lastCheckedAtMillis: Long
    ) : AppVersionUiState

    /**
     * The GitHub releases check failed (offline, rate-limited, GitHub outage, etc.) and no cached
     * result was available to fall back on. [currentVersion] is still shown (it's a local
     * constant, not fetched), but latest/update-available are unknown.
     */
    data class Unavailable(val currentVersion: String) : AppVersionUiState
}

/**
 * Dot-separated numeric version comparison (e.g. "1.2.0" vs "1.10.0" — a plain string compare
 * would wrongly say "1.10.0" < "1.2.0" since '1' < '2' lexicographically at the first differing
 * character). Non-numeric components compare as 0. A leading "v" must already be stripped by the
 * caller (see [com.derekwinters.chores.data.repository.AppVersionRepository]).
 */
fun isVersionNewer(candidate: String, baseline: String): Boolean {
    val candidateParts = candidate.split(".").map { it.toIntOrNull() ?: 0 }
    val baselineParts = baseline.split(".").map { it.toIntOrNull() ?: 0 }
    val length = maxOf(candidateParts.size, baselineParts.size)
    for (i in 0 until length) {
        val c = candidateParts.getOrElse(i) { 0 }
        val b = baselineParts.getOrElse(i) { 0 }
        if (c != b) return c > b
    }
    return false
}
