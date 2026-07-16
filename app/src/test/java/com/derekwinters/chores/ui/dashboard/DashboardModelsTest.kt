package com.derekwinters.chores.ui.dashboard

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.data.model.PointsSummary
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Issue #12 behaviors: "progress bar ... trend coloring" and "'Due Now' / 'Due Soon' counts per
 * person".
 */
class DashboardModelsTest {

    @Test
    fun trendStatus_ratioAtLeast80Percent_isSuccess() {
        assertEquals(TrendStatus.SUCCESS, trendStatus(current = 10, goal = 12))
    }

    @Test
    fun trendStatus_ratioBelow50Percent_isError() {
        assertEquals(TrendStatus.ERROR, trendStatus(current = 4, goal = 12))
    }

    @Test
    fun trendStatus_ratioBetween_isWarning() {
        assertEquals(TrendStatus.WARNING, trendStatus(current = 7, goal = 12))
    }

    @Test
    fun trendStatus_zeroGoal_isSuccess() {
        // Issue #123: a zero goal reads as "ahead"/success (nothing owed), matching web.
        assertEquals(TrendStatus.SUCCESS, trendStatus(current = 0, goal = 0))
    }

    private fun chore(id: Int, state: String, nextDue: String?, assignee: String?) = Chore(
        id = id,
        name = "Chore$id",
        points = 5,
        state = state,
        nextDue = nextDue,
        currentAssignee = assignee,
        eligiblePeople = emptyList()
    )

    // Issue #16: Home is logged-in-user specific — homeCards() filters the full Board set to the
    // signed-in user's own card.

    private fun card(username: String) = DashboardCard(
        personId = username.hashCode(),
        username = username,
        displayName = username.replaceFirstChar { it.uppercase() },
        points7d = 0,
        goal7d = 0,
        points30d = 0,
        goal30d = 0,
        dueNowCount = 0,
        dueSoonCount = 0
    )

    @Test
    fun homeCards_returnsOnlyTheSignedInUsersCard() {
        val cards = listOf(card("alice"), card("bob"))

        val home = homeCards(cards, username = "alice")

        assertEquals(1, home.size)
        assertEquals("alice", home.single().username)
    }

    @Test
    fun homeCards_nullOrBlankUsername_returnsEmpty() {
        val cards = listOf(card("alice"), card("bob"))

        // Identity not yet loaded: don't fall back to showing everyone's cards.
        assertEquals(emptyList<DashboardCard>(), homeCards(cards, username = null))
        assertEquals(emptyList<DashboardCard>(), homeCards(cards, username = "  "))
    }

    @Test
    fun homeCards_usernameWithNoMatchingPerson_returnsEmpty() {
        val cards = listOf(card("alice"), card("bob"))

        assertEquals(emptyList<DashboardCard>(), homeCards(cards, username = "carol"))
    }

    @Test
    fun buildDashboardCards_countsDueNowAndDueSoon_includingUnassignedChores() {
        val today = LocalDate.of(2026, 7, 2)
        val alice = Person(1, "alice", "Alice", isAdmin = false, goal7d = 12, goal30d = 50)
        val chores = listOf(
            chore(1, state = "due", nextDue = "2026-07-01", assignee = "alice"), // due now, alice's
            chore(2, state = "due", nextDue = "2026-07-01", assignee = null), // due now, unassigned
            chore(3, state = "not_due", nextDue = "2026-07-04", assignee = "alice"), // due soon (2 days)
            chore(4, state = "not_due", nextDue = "2026-08-01", assignee = "alice"), // too far out
            chore(5, state = "due", nextDue = null, assignee = "bob") // someone else's, excluded
        )

        val cards = buildDashboardCards(
            people = listOf(alice),
            pointsSummaries = listOf(PointsSummary(username = "alice", points7d = 10, points30d = 40)),
            chores = chores,
            dueSoonDays = 3,
            today = today
        )

        assertEquals(1, cards.size)
        val card = cards.single()
        assertEquals(2, card.dueNowCount)
        assertEquals(1, card.dueSoonCount)
        assertEquals(10, card.points7d)
        assertEquals(TrendStatus.SUCCESS, card.trend7d)
    }
}
