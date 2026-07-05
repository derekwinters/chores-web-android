package com.derekwinters.chores.ui.log

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.LogEntry
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.LogRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Issue #19: Activity Log filters — person/chore incl. deep links, action type, date range. */
data class LogFilters(
    val person: String? = null,
    val chore: String? = null,
    val action: String? = null,
    val start: String? = null,
    val end: String? = null
) {
    /**
     * Issue #68: drives the "Clear filters" action's visibility, same `isActive` convention as
     * Chores' [com.derekwinters.chores.ui.chores.ChoreFilters.isActive] -- true whenever any
     * filter (including a deep-linked person/chore) is set, so the action only appears when
     * there's actually something to clear.
     */
    val isActive: Boolean
        get() = person != null || chore != null || action != null || start != null || end != null
}

/**
 * Issue #19: one page of results plus paging info, for the "N results" UI. The backend's
 * `GET /v1/log` returns a bare (unpaginated) array, so [total]/[page]/[totalPages] are all
 * computed client-side over the full filtered result set fetched by the current [LogFilters].
 */
data class ActivityLogPageState(
    val entries: List<LogEntry>,
    val total: Int,
    val page: Int,
    val totalPages: Int = 1
)

/**
 * Issue #19 behaviors: filters (person incl. synthetic "system"/"schedule" actors, chore, action
 * type, date range), pagination, and support for being deep-linked with a pre-set chore or person
 * filter (Chore card History action, User Detail activity link).
 */
@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    private val logRepository: LogRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _filters = MutableStateFlow(
        LogFilters(
            person = savedStateHandle.get<String>("person"),
            chore = savedStateHandle.get<String>("chore")
        )
    )
    val filters: StateFlow<LogFilters> = _filters.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<ActivityLogPageState>>(UiState.Loading)
    val uiState: StateFlow<UiState<ActivityLogPageState>> = _uiState.asStateFlow()

    /** The full filtered result set from the last network fetch; paging slices this in memory. */
    private var allEntries: List<LogEntry> = emptyList()
    private var currentPage = 1

    init {
        load()
    }

    fun updateFilters(filters: LogFilters) {
        _filters.value = filters
        currentPage = 1
        load()
    }

    /** Client-side paging (issue #19) — no re-fetch needed, [allEntries] already holds everything. */
    fun nextPage() {
        if (currentPage < totalPages()) {
            currentPage += 1
            emitPage()
        }
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage -= 1
            emitPage()
        }
    }

    private fun load() {
        _uiState.value = UiState.Loading
        val filters = _filters.value
        viewModelScope.launch {
            logRepository.getLog(
                person = filters.person,
                choreId = filters.chore,
                action = filters.action,
                startDate = filters.start,
                endDate = filters.end
            ).onSuccess { entries ->
                allEntries = entries
                emitPage()
            }.onFailure { error ->
                _uiState.value = UiState.Error(errorMessage(error))
            }
        }
    }

    private fun totalPages(): Int = maxOf(1, ceil(allEntries.size / PAGE_SIZE.toDouble()).toInt())

    private fun emitPage() {
        val fromIndex = (currentPage - 1) * PAGE_SIZE
        val pageEntries = if (fromIndex >= allEntries.size) {
            emptyList()
        } else {
            allEntries.subList(fromIndex, minOf(fromIndex + PAGE_SIZE, allEntries.size))
        }
        _uiState.value = UiState.Success(
            ActivityLogPageState(pageEntries, allEntries.size, currentPage, totalPages())
        )
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR

    private companion object {
        const val PAGE_SIZE = 20
    }
}
