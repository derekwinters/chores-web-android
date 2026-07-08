package com.derekwinters.chores.ui

import com.derekwinters.chores.data.model.Chore

/**
 * Issue #167 behavior: the Chores bottom-nav tab's numeric badge shows the signed-in user's own
 * "due now" count, not a household-wide total — reusing Dashboard's existing per-person
 * `dueNowCount` definition (see [com.derekwinters.chores.ui.dashboard.buildDashboardCards]:
 * assigned to them, or unassigned/open chores are everyone's to pick up) rather than
 * introducing a second counting rule.
 *
 * A null/blank [username] (identity not yet loaded) yields 0 rather than counting unassigned
 * chores as "everyone's" before the signed-in user is actually known.
 */
fun dueNowCountForUser(chores: List<Chore>, username: String?): Int {
    if (username.isNullOrBlank()) return 0
    return chores.count { (it.currentAssignee == username || it.currentAssignee == null) && it.isDue }
}
