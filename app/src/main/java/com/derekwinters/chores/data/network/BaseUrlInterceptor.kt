package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.CredentialStore
import javax.inject.Inject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites each outgoing request's scheme/host/port from the user-entered server URL stored in
 * [CredentialStore] (issue #5 behavior: "OkHttp interceptor rewrites request URL from stored
 * server-URL preference").
 *
 * Retrofit requires a `baseUrl` at construction time, but the server address is runtime/
 * user-entered and can change between installs (self-hosted backend, no fixed origin — see ADR
 * 0002). Rather than rebuilding the Retrofit/OkHttp singletons when the URL changes, Retrofit is
 * built against a placeholder base URL (see NetworkModule) and every request's scheme/host/port
 * is swapped out here, keeping the path/query that Retrofit generated from the `@GET`/`@POST`
 * annotations untouched.
 */
class BaseUrlInterceptor @Inject constructor(
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val serverUrl = credentialStore.getServerUrl()?.toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val rewrittenUrl = original.url.newBuilder()
            .scheme(serverUrl.scheme)
            .host(serverUrl.host)
            .port(serverUrl.port)
            .build()

        return chain.proceed(original.newBuilder().url(rewrittenUrl).build())
    }
}
