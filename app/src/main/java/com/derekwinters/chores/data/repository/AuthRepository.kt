package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.auth.CredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.LoginRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #5 behavior: "Login screen ... calls POST /auth/login, persists token + URL".
 *
 * [serverUrl] is persisted *before* the login call is made, because
 * [com.derekwinters.chores.data.network.BaseUrlInterceptor] rewrites every request's host from
 * whatever is currently in [CredentialStore] — the login request itself needs to target the
 * server the user just typed in.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val api: ChoresApi,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager
) {
    suspend fun login(serverUrl: String, username: String, password: String): Result<Unit> {
        credentialStore.setServerUrl(serverUrl)

        return safeApiCall { api.login(LoginRequestDto(username, password)) }
            .map { response ->
                credentialStore.saveToken(token = response.access_token, tokenType = response.token_type)
                sessionManager.onLoginSuccess()
            }
    }
}
