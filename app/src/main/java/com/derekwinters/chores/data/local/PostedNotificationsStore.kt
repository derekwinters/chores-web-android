package com.derekwinters.chores.data.local

/**
 * Local record of which notification ids have already been posted to the system tray (issue #43),
 * giving the once-per-item guarantee across worker re-runs and process restarts. This is kept
 * client-side deliberately: the backend's `delivered_at` flips on the first list fetch and so
 * cannot distinguish "fetched" from "actually shown to the user".
 *
 * Backed by the same SharedPreferences store as [ConnectionStatusStore] (see
 * [SharedPrefsConnectionStatusStore]).
 */
interface PostedNotificationsStore {
    /** Whether [notificationId] has already been posted and should be skipped. */
    fun isPosted(notificationId: Int): Boolean

    /** Marks [notificationId] as posted so it is never posted again. */
    fun markPosted(notificationId: Int)
}
