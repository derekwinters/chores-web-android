package com.derekwinters.chores.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AuthRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #5 behavior: "Login screen: server URL + username/password fields, calls
 * POST /auth/login, persists token + URL".
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun login(serverUrl: String, username: String, password: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.login(serverUrl.trim(), username.trim(), password)
                .onSuccess { _uiState.value = UiState.Success(Unit) }
                .onFailure { error ->
                    val message = (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
                    _uiState.value = UiState.Error(message)
                }
        }
    }
}
