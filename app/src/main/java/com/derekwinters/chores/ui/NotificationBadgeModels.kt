package com.derekwinters.chores.ui

import com.derekwinters.chores.data.model.Notification

/**
 * Issue #45: the top-bar bell's numeric badge shows the count of **unread** (unacknowledged)
 * notifications. Derived at the call site from [NotificationBadgeViewModel]'s raw list rather than
 * pre-computed in the ViewModel — the same decoupling rationale as [dueNowCountForUser] over
 * [NavBadgeViewModel] (nav chrome lives outside any single screen and the ViewModel has no notion
 * of screen state).
 *
 * An empty / not-yet-loaded list yields 0, so the badge simply doesn't render until there's
 * something to show (see `ChoresApp`'s bell action).
 */
fun unreadNotificationCount(notifications: List<Notification>): Int =
    notifications.count { it.isUnread }
