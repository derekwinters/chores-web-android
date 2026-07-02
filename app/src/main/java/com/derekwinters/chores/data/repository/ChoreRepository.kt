package com.derekwinters.chores.data.repository

import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.network.ChoresApi
import com.derekwinters.chores.data.network.dto.CompleteChoreRequestDto
import com.derekwinters.chores.data.network.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Complete-chore action: POST /chores/{id}/complete, with
 * Completer-picker dialog when current_assignee == null".
 */
@Singleton
class ChoreRepository @Inject constructor(
    private val api: ChoresApi
) {
    suspend fun getChores(): Result<List<Chore>> =
        safeApiCall { api.getChores() }.map { chores -> chores.map { it.toDomain() } }

    /**
     * @param completedBy the chosen Completer's username, required when the chore has no
     *   [Chore.currentAssignee]; null lets the server default to the current assignee.
     */
    suspend fun completeChore(choreId: Int, completedBy: String? = null): Result<Chore> =
        safeApiCall { api.completeChore(choreId, CompleteChoreRequestDto(completedBy)) }
            .map { it.toDomain() }
}
