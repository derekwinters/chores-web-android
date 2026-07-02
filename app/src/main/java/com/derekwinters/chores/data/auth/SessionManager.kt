package com.derekwinters.chores.data.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks whether the app currently has a usable session, and is the single writer that flips
 * the app between the Login screen and the bottom-nav Scaffold (issue #5 grilling: "Login screen
 * gates the existing bottom-nav Scaffold").
 *
 * This exists because the global-401 interceptor (behavior: "Global 401 handling") runs on an
 * OkHttp dispatcher thread, not in Compose — it can't navigate directly. Instead it calls
 * [onUnauthorized], which clears the token and flips [isAuthenticated]; ChoresApp observes that
 * StateFlow and swaps to the Login screen.
 */
@Singleton
class SessionManager @Inject constructor(
    private val credentialStore: CredentialStore
) {
    private val _isAuthenticated = MutableStateFlow(credentialStore.hasSession())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    /** Called after a successful login with a freshly-persisted session. */
    fun onLoginSuccess() {
        _isAuthenticated.value = true
    }

    /** Called by the global-401 interceptor, or an explicit user logout. */
    fun onUnauthorized() {
        credentialStore.clearToken()
        _isAuthenticated.value = false
    }
}
