package com.derekwinters.chores.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [VersionCheckCache] backed by plain (unencrypted) [SharedPreferences] — unlike
 * [com.derekwinters.chores.data.auth.EncryptedCredentialStore], the cached data here (a public
 * GitHub release tag + a timestamp) isn't sensitive, so it doesn't need Keystore-backed
 * encryption.
 */
@Singleton
class SharedPrefsVersionCheckCache @Inject constructor(
    @ApplicationContext context: Context
) : VersionCheckCache {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    override fun getLastCheckedAtMillis(): Long? =
        if (prefs.contains(KEY_LAST_CHECKED_AT)) prefs.getLong(KEY_LAST_CHECKED_AT, 0L) else null

    override fun getCachedLatestVersion(): String? = prefs.getString(KEY_LATEST_VERSION, null)

    override fun save(checkedAtMillis: Long, latestVersion: String) {
        prefs.edit()
            .putLong(KEY_LAST_CHECKED_AT, checkedAtMillis)
            .putString(KEY_LATEST_VERSION, latestVersion)
            .apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "chores_version_check_prefs"
        const val KEY_LAST_CHECKED_AT = "last_checked_at_millis"
        const val KEY_LATEST_VERSION = "latest_version"
    }
}
