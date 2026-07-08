package com.derekwinters.chores.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.repository.ChoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #167 behavior: sources the bottom nav's Chores-tab "due now" badge count. Deliberately
 * decoupled from [com.derekwinters.chores.ui.dashboard.DashboardViewModel] (own polling loop,
 * own chores fetch) rather than sharing its state — nav chrome lives outside any single screen's
 * lifecycle, so it's scoped to the Activity (see ADR-0004) alongside [CurrentUserViewModel]/
 * [AppTitleViewModel] rather than to a NavHost destination.
 *
 * Exposes the raw chores list rather than a pre-computed count so the signed-in username (known
 * only to the composable tree, via [CurrentUserViewModel]) can be combined with it at the call
 * site via [dueNowCountForUser] — this ViewModel has no notion of "current user" itself.
 */
@HiltViewModel
class NavBadgeViewModel @Inject constructor(
    private val choreRepository: ChoreRepository
) : ViewModel() {

    private val _chores = MutableStateFlow<List<Chore>>(emptyList())
    val chores: StateFlow<List<Chore>> = _chores.asStateFlow()

    private var pollingStarted = false

    init {
        refresh()
        startPolling()
    }

    fun refresh() {
        viewModelScope.launch {
            choreRepository.getChores().onSuccess { chores -> _chores.value = chores }
        }
    }

    private fun startPolling() {
        if (pollingStarted) return
        pollingStarted = true
        viewModelScope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                refresh()
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 60_000L
    }
}
