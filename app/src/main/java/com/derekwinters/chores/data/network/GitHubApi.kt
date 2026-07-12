package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.network.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * `api.github.com` surface (issue #35's client-side own-version check) — an entirely separate
 * host from [ChoresApi]'s self-hosted, user-entered backend, so it's given its own
 * Retrofit/OkHttp instance (see [com.derekwinters.chores.di.NetworkModule]'s `@GitHubRetrofit`
 * bindings) rather than going through [BaseUrlInterceptor]/[AuthInterceptor], which are specific
 * to the chores-web backend.
 */
interface GitHubApi {

    /**
     * Issue #35: compared against `BuildConfig.VERSION_NAME` to decide if a newer app release
     * exists. Unauthenticated GitHub API calls are capped at 60 requests/hour/IP — callers should
     * go through [com.derekwinters.chores.data.repository.AppVersionRepository]'s cache rather
     * than calling this directly on every screen visit.
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GitHubReleaseDto
}
