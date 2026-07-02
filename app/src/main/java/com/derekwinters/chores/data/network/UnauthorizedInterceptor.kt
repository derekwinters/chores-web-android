package com.derekwinters.chores.data.network

import com.derekwinters.chores.data.auth.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Global 401 handling (issue #5 behavior: "Global 401 handling: clear token, navigate to
 * Login"), mirroring `frontend/src/api/client.js`'s behavior of clearing the token on any 401.
 *
 * This only clears local session state; it does not retry the request or navigate directly —
 * [SessionManager.onUnauthorized] flips `isAuthenticated`, and ChoresApp (observing that
 * StateFlow) is what actually swaps the UI back to the Login screen. The 401 response is passed
 * through unchanged so the caller still surfaces the failure via [ApiException].
 */
class UnauthorizedInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            sessionManager.onUnauthorized()
        }
        return response
    }
}
