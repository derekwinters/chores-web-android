package com.derekwinters.chores.ui.chores

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.network.dto.ChoreDto
import com.derekwinters.chores.data.repository.ChoreRepository
import com.derekwinters.chores.ui.UiState
import java.io.IOException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/points/state/
 * next_due", "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null", and "Sealed UiState + StateFlow pattern for ChoreListViewModel"
 * (area: android, ui, network).
 */
class ChoreListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val assignedChoreDto = ChoreDto(
        id = 1,
        name = "Dishes",
        points = 5,
        state = "due",
        next_due = "2026-07-05",
        current_assignee = "alice",
        eligible_people = listOf("alice", "bob")
    )
    private val unassignedChoreDto = ChoreDto(
        id = 2,
        name = "Trash",
        points = 3,
        state = "due",
        next_due = null,
        current_assignee = null,
        eligible_people = listOf("alice", "bob")
    )

    @Test
    fun init_loadsChores_andStartsInLoadingState() {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresResult = listOf(assignedChoreDto))))
        assertEquals(UiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun loadChores_success_mapsDtosToDomainChores() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(
            ChoreRepository(FakeChoresApi(choresResult = listOf(assignedChoreDto, unassignedChoreDto)))
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val chores = (state as UiState.Success).data
        assertEquals(2, chores.size)
        assertEquals("alice", chores[0].currentAssignee)
        assertTrue(chores[1].needsCompleterSelection)
        assertEquals(listOf("alice", "bob"), chores[1].eligiblePeople)
    }

    @Test
    fun loadChores_failure_updatesStateToError() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = ChoreListViewModel(ChoreRepository(FakeChoresApi(choresError = IOException("boom"))))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Error)
        assertEquals(HttpErrorMessages.NETWORK_ERROR, (state as UiState.Error).message)
    }

    @Test
    fun completeChore_assignedChore_sendsNullCompletedBy_andReloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(assignedChoreDto),
            completeResult = assignedChoreDto.copy(state = "done")
        )
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        val chore = Chore(1, "Dishes", 5, "due", "2026-07-05", "alice", listOf("alice", "bob"))
        viewModel.completeChore(chore)
        advanceUntilIdle()

        assertEquals(1, api.lastCompleteChoreId)
        assertNull(api.lastCompleteRequest?.completed_by)
        assertNull(viewModel.completingChoreId.value)
    }

    @Test
    fun completeChore_withCompleter_sendsChosenUsername() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            choresResult = listOf(unassignedChoreDto),
            completeResult = unassignedChoreDto.copy(state = "done", current_assignee = "bob")
        )
        val viewModel = ChoreListViewModel(ChoreRepository(api))
        advanceUntilIdle()

        val chore = Chore(2, "Trash", 3, "due", null, null, listOf("alice", "bob"))
        assertTrue(chore.needsCompleterSelection)
        viewModel.completeChore(chore, completedBy = "bob")
        advanceUntilIdle()

        assertEquals(2, api.lastCompleteChoreId)
        assertEquals("bob", api.lastCompleteRequest?.completed_by)
    }
}
