package com.derekwinters.chores.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single plain-[SharedPreferences] backing for both [ConnectionStatusStore] (issue #44's offline
 * alert reads the last-contact timestamp) and [PostedNotificationsStore] (issue #43's once-per-
 * item guarantee). Both interfaces are bound to this one singleton in
 * [com.derekwinters.chores.di.StorageModule], so they share the same prefs file — the "same
 * store" option called out in the issue.
 *
 * Unencrypted on purpose: neither a contact timestamp nor a set of already-shown notification ids
 * is sensitive, so this mirrors [SharedPrefsVersionCheckCache] rather than
 * [com.derekwinters.chores.data.auth.EncryptedCredentialStore].
 */
@Singleton
class SharedPrefsConnectionStatusStore @Inject constructor(
    @ApplicationContext context: Context
) : ConnectionStatusStore, PostedNotificationsStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    override fun recordSuccessfulContact(epochMillis: Long) {
        prefs.edit().putLong(KEY_LAST_CONTACT, epochMillis).apply()
    }

    override fun lastSuccessfulContact(): Long? =
        if (prefs.contains(KEY_LAST_CONTACT)) prefs.getLong(KEY_LAST_CONTACT, 0L) else null

    override fun isPosted(notificationId: Int): Boolean =
        postedIds().contains(notificationId.toString())

    override fun markPosted(notificationId: Int) {
        // Copy-on-write: SharedPreferences returns an immutable/shared Set, so mutate a copy.
        val updated = HashSet(postedIds())
        updated.add(notificationId.toString())
        prefs.edit().putStringSet(KEY_POSTED_IDS, updated).apply()
    }

    private fun postedIds(): Set<String> =
        prefs.getStringSet(KEY_POSTED_IDS, emptySet()) ?: emptySet()

    private companion object {
        const val PREFS_FILE_NAME = "chores_connection_status_prefs"
        const val KEY_LAST_CONTACT = "last_successful_contact_millis"
        const val KEY_POSTED_IDS = "posted_notification_ids"
    }
}
