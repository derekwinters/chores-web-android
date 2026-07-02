package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import com.derekwinters.chores.data.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin Compose-facing wrapper around the app-scoped [SessionManager] singleton (issue #5
 * grilling: "Login screen gates the existing bottom-nav Scaffold"). [SessionManager] itself
 * isn't a ViewModel — it's also used from [com.derekwinters.chores.data.network.UnauthorizedInterceptor]
 * on an OkHttp thread — so this just exposes its StateFlow for `collectAsStateWithLifecycle()`.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    sessionManager: SessionManager
) : ViewModel() {
    val isAuthenticated: StateFlow<Boolean> = sessionManager.isAuthenticated
}
