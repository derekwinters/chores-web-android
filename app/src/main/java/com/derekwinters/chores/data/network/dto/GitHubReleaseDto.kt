package com.derekwinters.chores.data.network.dto

import kotlinx.serialization.Serializable

/**
 * Response body for `GET https://api.github.com/repos/{owner}/{repo}/releases/latest` (issue
 * #35's client-side own-version check). Only [tag_name] is used — GitHub's real release payload
 * has dozens of other fields (assets, body, author, ...) this app has no use for; the shared
 * [kotlinx.serialization.json.Json]'s `ignoreUnknownKeys` (see
 * [com.derekwinters.chores.di.NetworkModule]) drops the rest.
 */
@Serializable
data class GitHubReleaseDto(
    val tag_name: String
)
