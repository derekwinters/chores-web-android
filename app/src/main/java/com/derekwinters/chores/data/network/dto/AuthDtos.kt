package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Request body for `POST /v1/auth/login`, matching chores-web's `LoginRequest` schema
 * (backend/app/schemas.py).
 */
@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

/**
 * Response body for `POST /v1/auth/login`, matching chores-web's `LoginResponse` schema.
 *
 * `access_token` is the 365-day JWT (see backend/app/routers/auth.py `_create_jwt_token`);
 * `token_type` is expected to be "bearer" and is used verbatim when building the
 * `Authorization` header so we don't hard-code casing/spelling.
 */
@Serializable
data class LoginResponseDto(
    val access_token: String,
    val token_type: String,
    val user: UserInfoDto
)

/** Matches chores-web's `UserInfo` schema, returned by login and `GET /v1/auth/me`. */
@Serializable
data class UserInfoDto(
    val username: String,
    val is_admin: Boolean
)
