package com.derekwinters.chores.ui

import com.derekwinters.chores.data.model.Chore
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue #167 behavior: the Chores bottom-nav tab's "due now" badge count is the signed-in user's
 * own due-now count — reusing [com.derekwinters.chores.ui.dashboard.buildDashboardCards]'
 * per-person `dueNowCount` definition (assigned to them, or unassigned/open chores, currently
 * `state == "due"`) rather than a household-wide total (area: android).
 */
class NavBadgeModelsTest {

    private fun chore(id: Int, state: String, assignee: String?) = Chore(
        id = id,
        name = "Chore$id",
        points = 5,
        state = state,
        nextDue = null,
        currentAssignee = assignee,
        eligiblePeople = emptyList()
    )

    @Test
    fun dueNowCountForUser_countsOwnDueChoresPlusUnassigned_excludesOthers() {
        val chores = listOf(
            chore(1, state = "due", assignee = "alice"), // alice's, due
            chore(2, state = "due", assignee = null), // unassigned, due
            chore(3, state = "not_due", assignee = "alice"), // alice's, not due
            chore(4, state = "due", assignee = "bob") // someone else's, excluded
        )

        assertEquals(2, dueNowCountForUser(chores, username = "alice"))
    }

    @Test
    fun dueNowCountForUser_noDueChores_isZero() {
        val chores = listOf(chore(1, state = "not_due", assignee = "alice"))

        assertEquals(0, dueNowCountForUser(chores, username = "alice"))
    }

    @Test
    fun dueNowCountForUser_nullUsername_isZeroRegardlessOfUnassignedChores() {
        // Username not yet loaded (CurrentUserViewModel still resolving) shouldn't flash a count
        // derived from unassigned chores that aren't necessarily this not-yet-known user's.
        val chores = listOf(chore(1, state = "due", assignee = null))

        assertEquals(0, dueNowCountForUser(chores, username = null))
    }

    @Test
    fun dueNowCountForUser_blankUsername_isZero() {
        val chores = listOf(chore(1, state = "due", assignee = null))

        assertEquals(0, dueNowCountForUser(chores, username = ""))
    }

    @Test
    fun dueNowCountForUser_emptyChoreList_isZero() {
        assertEquals(0, dueNowCountForUser(emptyList(), username = "alice"))
    }
}
