package com.derekwinters.chores.ui.login

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.LoginResponseDto
import com.derekwinters.chores.data.network.dto.UserInfoDto
import com.derekwinters.chores.data.repository.AuthRepository
import com.derekwinters.chores.ui.UiState
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behavior: "Login screen ... calls POST /auth/login, persists token + URL" and "Sealed
 * UiState + StateFlow pattern" (area: android, ui, network).
 */
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        credentialStore: FakeCredentialStore = FakeCredentialStore(),
        sessionManager: SessionManager = SessionManager(credentialStore),
        api: FakeChoresApi
    ) = LoginViewModel(AuthRepository(api, credentialStore, sessionManager)) to (credentialStore to sessionManager)

    @Test
    fun initialState_isIdle() {
        val (viewModel, _) = buildViewModel(api = FakeChoresApi())
        assertEquals(UiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun login_setsLoadingStateImmediately() {
        val (viewModel, _) = buildViewModel(
            api = FakeChoresApi(loginResult = LoginResponseDto("tok", "bearer", UserInfoDto("alice", false)))
        )

        viewModel.login("http://chores.example.com", "alice", "secret")

        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun login_success_updatesStateAndPersistsSession() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, stores) = buildViewModel(
            api = FakeChoresApi(loginResult = LoginResponseDto("tok123", "bearer", UserInfoDto("alice", false)))
        )
        val (credentialStore, sessionManager) = stores

        viewModel.login("http://chores.example.com", "alice", "secret")
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.uiState.value)
        assertEquals("tok123", credentialStore.getToken())
        assertEquals("http://chores.example.com", credentialStore.getServerUrl())
        assertTrue(sessionManager.isAuthenticated.value)
    }

    @Test
    fun login_failure_updatesStateToErrorWithoutAuthenticating() = runTest(mainDispatcherRule.testDispatcher) {
        val (viewModel, stores) = buildViewModel(api = FakeChoresApi(loginError = IOException("boom")))
        val (_, sessionManager) = stores

        viewModel.login("http://chores.example.com", "alice", "wrong")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(HttpErrorMessages.NETWORK_ERROR, (state as UiState.Error).message)
        assertTrue(!sessionManager.isAuthenticated.value)
    }
}
