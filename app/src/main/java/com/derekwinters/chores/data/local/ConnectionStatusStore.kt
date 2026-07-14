package com.derekwinters.chores.data.local

/**
 * Records the last time the app successfully reached the user's self-hosted backend (issue #43),
 * so issue #44's settings screen can raise a "not connected in X days" offline alert. Written on
 * every successful notification poll (see
 * [com.derekwinters.chores.notifications.NotificationPollWorker]).
 *
 * A timestamp is not a credential, so — like [VersionCheckCache] — this is backed by plain
 * (unencrypted) [android.content.SharedPreferences], leaving [
 * com.derekwinters.chores.data.auth.EncryptedCredentialStore] untouched.
 */
interface ConnectionStatusStore {
    /** Persists [epochMillis] as the most recent successful backend contact. */
    fun recordSuccessfulContact(epochMillis: Long)

    /** The last successful-contact timestamp, or null if the backend has never been reached. */
    fun lastSuccessfulContact(): Long?
}
