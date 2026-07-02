package com.derekwinters.chores.ui.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Sealed UiState + StateFlow pattern for ChoreListViewModel".
 */
@HiltViewModel
class ChoreListViewModel @Inject constructor(
    private val choreRepository: ChoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Chore>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Chore>>> = _uiState.asStateFlow()

    /** Tracks which chore is mid-completion so its row can show a progress indicator. */
    private val _completingChoreId = MutableStateFlow<Int?>(null)
    val completingChoreId: StateFlow<Int?> = _completingChoreId.asStateFlow()

    init {
        loadChores()
    }

    fun loadChores() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            choreRepository.getChores()
                .onSuccess { chores -> _uiState.value = UiState.Success(chores) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
    }

    /**
     * Completes [chore]. [completedBy] must be provided (from the Completer-picker dialog) when
     * [Chore.needsCompleterSelection] is true; the caller is responsible for showing that dialog
     * before calling this for such chores.
     */
    fun completeChore(chore: Chore, completedBy: String? = null) {
        _completingChoreId.value = chore.id
        viewModelScope.launch {
            choreRepository.completeChore(chore.id, completedBy)
                .onSuccess { loadChores() }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
            _completingChoreId.value = null
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
