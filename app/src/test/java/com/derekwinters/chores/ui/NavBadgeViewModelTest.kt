package com.derekwinters.chores.ui

import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.repository.ChoreRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Issue #167 behavior: an Activity-scoped [NavBadgeViewModel] with its own polling loop sources
 * the Chores bottom-nav badge count, decoupled from [com.derekwinters.chores.ui.dashboard.DashboardViewModel]
 * since nav chrome lives outside any single screen's lifecycle (see ADR-0004). Mirrors
 * DashboardViewModelTest's pattern of cancelling the endless auto-refresh loop before the test
 * body ends so runTest's implicit drain doesn't hang.
 */
class NavBadgeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_loadsChoresFromRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(
                ChoreDto(id = 1, name = "Dishes", points = 5, state = "due", current_assignee = "alice")
            )
        )
        val viewModel = NavBadgeViewModel(ChoreRepository(api))
        runCurrent()

        val chores = viewModel.chores.value
        assertEquals(1, chores.size)
        assertEquals("Dishes", chores.single().name)

        viewModel.viewModelScope.cancel()
    }

    @Test
    fun initialState_isEmptyBeforeFirstLoadCompletes() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(choresResult = listOf(ChoreDto(id = 1, name = "Dishes", points = 5, state = "due")))
        val viewModel = NavBadgeViewModel(ChoreRepository(api))

        // Before runCurrent() drains the init{} launch, the flow hasn't been populated yet.
        assertEquals(emptyList<Any>(), viewModel.chores.value)

        runCurrent()
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun choresApiFailure_leavesChoresEmptyRatherThanCrashing() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(choresError = RuntimeException("boom"))
        val viewModel = NavBadgeViewModel(ChoreRepository(api))
        runCurrent()

        assertEquals(emptyList<Any>(), viewModel.chores.value)

        viewModel.viewModelScope.cancel()
    }
}
