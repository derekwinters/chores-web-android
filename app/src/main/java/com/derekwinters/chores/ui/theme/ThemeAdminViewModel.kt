package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #24 behaviors: household default theme management — 6 built-in themes (protected:
 * can't rename/delete, only copy) plus custom themes (create via copy, rename, delete
 * non-built-ins), and setting the household default. The real API exposes no `is_builtin` flag,
 * so "protected" isn't pre-checked here; a rename/delete of a built-in theme is expected to fail
 * server-side and surfaces through [actionState] like any other error.
 */
@HiltViewModel
class ThemeAdminViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ThemeOption>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<ThemeOption>>> = _uiState.asStateFlow()

    private val _defaultThemeId = MutableStateFlow<String?>(null)
    val defaultThemeId: StateFlow<String?> = _defaultThemeId.asStateFlow()

    private val _actionState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val actionState: StateFlow<UiState<Unit>> = _actionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.getThemes()
                .onSuccess { themes -> _uiState.value = UiState.Success(themes) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }

            themeRepository.getDefaultTheme()
                .onSuccess { theme -> _defaultThemeId.value = theme.id }
                .onFailure { _defaultThemeId.value = null }
        }
    }

    fun setDefaultTheme(themeId: String) {
        viewModelScope.launch {
            themeRepository.setDefaultTheme(themeId).onSuccess { load() }
        }
    }

    /** Creates a new custom theme named [name] by copying [sourceTheme]'s colors. */
    fun createTheme(name: String, sourceTheme: ThemeOption) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.createTheme(name, sourceTheme)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Renames [themeId] to [newName]; the server rejects renaming a built-in theme. */
    fun renameTheme(themeId: String, newName: String) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.renameTheme(themeId, newName)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /**
     * Issue #130: replaces [themeId]'s colors with [colors]'s full 9-color palette. Like rename,
     * editing a protected built-in theme is rejected server-side and surfaces via [actionState].
     */
    fun updateColors(themeId: String, colors: ThemeOption) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.updateColors(themeId, colors)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /** Non-built-in themes only; enforced server-side too. */
    fun deleteTheme(themeId: String) {
        _actionState.value = UiState.Loading
        viewModelScope.launch {
            themeRepository.deleteTheme(themeId)
                .onSuccess {
                    _actionState.value = UiState.Success(Unit)
                    load()
                }
                .onFailure { error -> _actionState.value = UiState.Error(errorMessage(error)) }
        }
    }

    fun clearActionState() {
        _actionState.value = UiState.Idle
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
