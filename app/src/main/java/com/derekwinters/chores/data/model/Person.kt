package com.derekwinters.chores.data.model

import com.derekwinters.chores.data.network.dto.PersonDto
import com.derekwinters.chores.data.network.dto.UserStatsDto
import com.derekwinters.chores.data.network.dto.PointsSummaryDto
import com.derekwinters.chores.data.network.dto.RedemptionDto

/** Domain model for a household member (issues #12, #17, #18). */
data class Person(
    val id: Int,
    val username: String,
    val displayName: String,
    val isAdmin: Boolean,
    val goal7d: Int,
    val goal30d: Int,
    val points: Int = 0,
    val pointsRedeemed: Int = 0
)

fun PersonDto.toDomain(): Person = Person(
    id = id,
    username = username,
    displayName = name,
    isAdmin = is_admin,
    goal7d = goal_7d,
    goal30d = goal_30d,
    points = points,
    pointsRedeemed = points_redeemed
)

/**
 * Issue #12: rolling 7/30-day point totals used to drive the Dashboard progress bars. Joined back
 * to a [Person] by [username] (chores-web's `PointsSummaryEntry.person`), not by id.
 */
data class PointsSummary(
    val username: String,
    val points7d: Int,
    val points30d: Int
)

fun PointsSummaryDto.toDomain(): PointsSummary = PointsSummary(
    username = person,
    points7d = points_7d,
    points30d = points_30d
)

/**
 * Issue #17: User Detail stats panel — matches chores-web's `UserStatsOut`. [availablePoints] is
 * the server-computed spendable balance (`display_points`); [totalPoints] is the lifetime earned
 * total (`total_points`).
 *
 * Issue #104: [redeemed] is the lifetime redeemed total shown on web's User Detail as "Redeemed".
 * `UserStatsOut` has no `redeemed_total`/`points_redeemed` wire field of its own, but chores-web's
 * `/points/stats/{person}` handler derives `display_points` server-side as
 * `total_points - points_redeemed` (see chores-web backend/app/routers/points.py), so the same
 * total is recovered client-side as `total_points - display_points` without any API change.
 */
data class PersonStats(
    val name: String = "",
    val availablePoints: Int = 0,
    val totalPoints: Int = 0,
    val points7d: Int = 0,
    val points30d: Int = 0,
    val completedCount: Int = 0,
    val skippedCount: Int = 0,
    val redeemed: Int = 0
)

fun UserStatsDto.toDomain(): PersonStats = PersonStats(
    name = name,
    availablePoints = display_points,
    totalPoints = total_points,
    points7d = points_7d,
    points30d = points_30d,
    completedCount = completed_count,
    skippedCount = skipped_count,
    redeemed = total_points - display_points
)

/** Issue #17: a single redemption-history row. */
data class Redemption(
    val id: Int,
    val personId: Int = 0,
    val amount: Int,
    val redeemedBy: String,
    val timestamp: String
)

fun RedemptionDto.toDomain(): Redemption = Redemption(
    id = id,
    personId = person_id,
    amount = amount,
    redeemedBy = redeemed_by,
    timestamp = timestamp
)
