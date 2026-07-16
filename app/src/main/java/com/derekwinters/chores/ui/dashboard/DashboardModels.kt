package com.derekwinters.chores.ui.dashboard

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.data.model.PointsSummary
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Issue #12 trend coloring: "≥80% of goal = success, <50% = error/warning, else warning". */
enum class TrendStatus { SUCCESS, WARNING, ERROR }

/** Issue #123: a zero goal reads as "ahead"/success (nothing owed), matching web -- not warning. */
fun trendStatus(current: Int, goal: Int): TrendStatus {
    if (goal <= 0) return TrendStatus.SUCCESS
    val ratio = current.toDouble() / goal
    return when {
        ratio >= 0.8 -> TrendStatus.SUCCESS
        ratio < 0.5 -> TrendStatus.ERROR
        else -> TrendStatus.WARNING
    }
}

/** Issue #12: one Dashboard person card's worth of derived data. */
data class DashboardCard(
    val personId: Int,
    val username: String,
    val displayName: String,
    val points7d: Int,
    val goal7d: Int,
    val points30d: Int,
    val goal30d: Int,
    val dueNowCount: Int,
    val dueSoonCount: Int
) {
    val initial: String get() = displayName.trim().firstOrNull()?.uppercase() ?: "?"
    val trend7d: TrendStatus get() = trendStatus(points7d, goal7d)
    val trend30d: TrendStatus get() = trendStatus(points30d, goal30d)
}

/**
 * Issue #12 behavior: builds one card per person with progress vs. goals and Due Now/Due Soon
 * counts. A chore counts toward a person's Due Now/Due Soon bucket when it's assigned to them or
 * has no fixed assignee (open/unassigned chores are everyone's to pick up), matching the "that
 * person plus unassigned/open chores" deep-link wording.
 */
fun buildDashboardCards(
    people: List<Person>,
    pointsSummaries: List<PointsSummary>,
    chores: List<Chore>,
    dueSoonDays: Int,
    today: LocalDate = LocalDate.now()
): List<DashboardCard> {
    val summaryByUsername = pointsSummaries.associateBy { it.username }
    return people.map { person ->
        val summary = summaryByUsername[person.username]
        val relevantChores = chores.filter { it.currentAssignee == person.username || it.currentAssignee == null }
        DashboardCard(
            personId = person.id,
            username = person.username,
            displayName = person.displayName,
            points7d = summary?.points7d ?: 0,
            goal7d = person.goal7d,
            points30d = summary?.points30d ?: 0,
            goal30d = person.goal30d,
            dueNowCount = relevantChores.count { it.isDue },
            dueSoonCount = relevantChores.count { chore -> isDueSoon(chore, dueSoonDays, today) }
        )
    }
}

/**
 * Issue #16: Home is logged-in-user specific — it reuses the exact per-person Board card
 * ([DashboardCard]: trend data + Due Now/Due Soon counts) but shows only the signed-in user's own
 * card, filtered out of the full Board set by username. A null/blank username (identity not yet
 * loaded) or a username with no matching person yields an empty list, which Home renders as its
 * empty state rather than falling back to showing everyone's cards.
 */
fun homeCards(cards: List<DashboardCard>, username: String?): List<DashboardCard> =
    if (username.isNullOrBlank()) emptyList() else cards.filter { it.username == username }

private fun isDueSoon(chore: Chore, dueSoonDays: Int, today: LocalDate): Boolean {
    if (chore.isDue) return false
    val dueDate = chore.nextDue?.let(::parseDateOrNull) ?: return false
    return !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(dueSoonDays.toLong()))
}

private fun parseDateOrNull(value: String): LocalDate? = try {
    LocalDate.parse(value)
} catch (e: DateTimeParseException) {
    null
}
