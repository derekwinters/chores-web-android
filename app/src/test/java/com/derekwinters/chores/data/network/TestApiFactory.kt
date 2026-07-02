package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.CredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.di.NetworkModule

/**
 * Builds a real [ChoresApi] using the exact same Retrofit/OkHttp/Json wiring as production
 * (NetworkModule's `@Provides` functions are plain functions on a Kotlin `object`, so they're
 * callable directly without a Hilt component) but pointed at a test [CredentialStore] /
 * [SessionManager] instead of the real EncryptedSharedPreferences-backed ones. Used by
 * repository/interceptor-stack integration tests against MockWebServer.
 */
fun buildTestApi(credentialStore: CredentialStore, sessionManager: SessionManager): ChoresApi {
    val json = NetworkModule.provideJson()
    val client = NetworkModule.provideOkHttpClient(
        BaseUrlInterceptor(credentialStore),
        AuthInterceptor(credentialStore),
        UnauthorizedInterceptor(sessionManager)
    )
    val retrofit = NetworkModule.provideRetrofit(client, json)
    return NetworkModule.provideChoresApi(retrofit)
}
