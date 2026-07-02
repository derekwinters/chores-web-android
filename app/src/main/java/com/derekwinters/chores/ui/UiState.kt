package com.derekwinters.chores.ui

/**
 * Issue #5 behavior: "Sealed UiState + StateFlow pattern for ChoreListViewModel" — the first
 * ViewModel pattern established in this codebase, reused by LoginViewModel for consistency.
 */
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
