package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.CredentialStore
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches `Authorization: <token_type> <token>` to every outgoing request when a session
 * exists (issue #5 behavior: "OkHttp interceptor attaches Authorization: Bearer <token>
 * header"). Requests made before login (e.g. the login call itself) pass through unmodified.
 */
class AuthInterceptor @Inject constructor(
    private val credentialStore: CredentialStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = credentialStore.getToken()
        if (token.isNullOrBlank()) {
            return chain.proceed(original)
        }

        val tokenType = credentialStore.getTokenType()?.takeIf { it.isNotBlank() } ?: "Bearer"
        val authorized = original.newBuilder()
            .header("Authorization", "$tokenType $token")
            .build()

        return chain.proceed(authorized)
    }
}
