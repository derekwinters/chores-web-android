package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response element for `GET /v1/people` (issues #12, #17, #18) — matches chores-web's
 * `PersonOut` exactly (field order mirrors the OpenAPI schema). Note there is no
 * `display_name`/`points_7d`/`points_30d`/`available_points` on this type; the rolling point
 * totals live on [PointsSummaryDto]/[UserStatsDto] instead, joined back to a person by
 * `username` (not `id`).
 */
@Serializable
data class PersonDto(
    val id: Int,
    val name: String,
    val username: String,
    val requires_password_reset: Boolean = false,
    val is_admin: Boolean = false,
    val color: String = "",
    val goal_7d: Int = 12,
    val goal_30d: Int = 50,
    val points: Int = 0,
    val points_redeemed: Int = 0,
    val preferred_theme: String? = null
)

/**
 * Per-person rolling points totals, `GET /v1/points/summary` (issue #12) — matches
 * `PointsSummaryEntry`. The join key back to a [PersonDto] is `person` (the username string),
 * not a numeric id.
 */
@Serializable
data class PointsSummaryDto(
    val person: String = "",
    val points_7d: Int = 0,
    val points_30d: Int = 0
)

/**
 * Response for `GET /v1/points/stats/{person}` (issue #17), where `{person}` is the username
 * string, not a numeric person id — matches `UserStatsOut`. `display_points` is chores-web's
 * server-computed spendable balance (`points - points_redeemed`); `total_points` is the lifetime
 * earned total. There is no `redeemed_total` wire field on this endpoint; issue #104's "Redeemed"
 * stat is derived client-side as `total_points - display_points` (see
 * [PersonStats][com.derekwinters.chores.data.model.PersonStats]'s `toDomain()` mapping).
 */
@Serializable
data class UserStatsDto(
    val name: String = "",
    val total_points: Int = 0,
    val display_points: Int = 0,
    val points_7d: Int = 0,
    val points_30d: Int = 0,
    val completed_count: Int = 0,
    val skipped_count: Int = 0
)

/**
 * Request body for `POST /v1/people` (create user), issue #18 — matches `PersonCreate`, which
 * requires an explicit `username` (chores-web does not auto-derive it server-side as previously
 * assumed). [PeopleRepository][com.derekwinters.chores.data.repository.PeopleRepository] derives
 * a username slug from the display name client-side so the Create User dialog can keep its
 * existing "display name + password" shape.
 */
@Serializable
data class CreatePersonRequestDto(
    val name: String,
    val username: String,
    val password: String? = null,
    val color: String? = null
)

/**
 * Request body for `PUT /v1/people/{person_id}` (edit user), issue #18 — matches `PersonUpdate`.
 * All fields optional/nullable so the client only sends what changed; `password` blank/null means
 * "unchanged".
 */
@Serializable
data class UpdatePersonRequestDto(
    val name: String? = null,
    val username: String? = null,
    val goal_7d: Int? = null,
    val goal_30d: Int? = null,
    val password: String? = null,
    val is_admin: Boolean? = null
)

/** Request body for `POST /v1/people/{person_id}/redeem`, issue #17 — matches `PersonRedemption`. */
@Serializable
data class RedeemRequestDto(
    val amount: Int
)

/**
 * Response element for `GET /v1/people/{person_id}/redemptions`, issue #17 — matches
 * `RedemptionLogOut`.
 */
@Serializable
data class RedemptionDto(
    val id: Int,
    val person_id: Int = 0,
    val amount: Int,
    val redeemed_by: String,
    val timestamp: String
)
