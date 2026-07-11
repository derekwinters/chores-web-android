package com.derekwinters.chores.data.local

/** In-memory [VersionCheckCache] test double (issue #35). */
class FakeVersionCheckCache(
    private var lastCheckedAtMillis: Long? = null,
    private var cachedLatestVersion: String? = null
) : VersionCheckCache {

    var saveCallCount: Int = 0
        private set

    override fun getLastCheckedAtMillis(): Long? = lastCheckedAtMillis

    override fun getCachedLatestVersion(): String? = cachedLatestVersion

    override fun save(checkedAtMillis: Long, latestVersion: String) {
        saveCallCount++
        lastCheckedAtMillis = checkedAtMillis
        cachedLatestVersion = latestVersion
    }
}
