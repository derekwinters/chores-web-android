package com.derekwinters.chores.ui.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.Chore
import com.derekwinters.chores.ui.UiState

/**
 * Issue #5 behaviors: "Chore list screen: GET /chores, render name/assignee-or-Completer/
 * points/state/next_due" and "Complete-chore action ... with Completer-picker dialog when
 * current_assignee == null". Replaces HomeScreen's hello-world content on the Home tab (issue
 * #5 grilling); the Notification tab (issue #2) is unaffected.
 *
 * Thin Hilt-wired wrapper around [ChoreListContent]; see ChoreListContentTest for behavior
 * coverage that doesn't require a Hilt test component.
 */
@Composable
fun ChoreListScreen(
    modifier: Modifier = Modifier,
    viewModel: ChoreListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val completingChoreId by viewModel.completingChoreId.collectAsState()

    ChoreListContent(
        modifier = modifier,
        uiState = uiState,
        completingChoreId = completingChoreId,
        onComplete = viewModel::completeChore
    )
}

@Composable
fun ChoreListContent(
    uiState: UiState<List<Chore>>,
    completingChoreId: Int?,
    onComplete: (Chore, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var choreAwaitingCompleter by remember { mutableStateOf<Chore?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is UiState.Error -> {
                Text(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            is UiState.Success -> {
                if (state.data.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.chore_list_empty)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.data, key = { it.id }) { chore ->
                            ChoreRow(
                                chore = chore,
                                isCompleting = completingChoreId == chore.id,
                                onCompleteClick = {
                                    if (chore.needsCompleterSelection) {
                                        choreAwaitingCompleter = chore
                                    } else {
                                        onComplete(chore, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    choreAwaitingCompleter?.let { chore ->
        CompleterPickerDialog(
            people = chore.eligiblePeople,
            onConfirm = { completedBy ->
                onComplete(chore, completedBy)
                choreAwaitingCompleter = null
            },
            onDismiss = { choreAwaitingCompleter = null }
        )
    }
}

@Composable
private fun ChoreRow(
    chore: Chore,
    isCompleting: Boolean,
    onCompleteClick: () -> Unit
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chore.name, style = MaterialTheme.typography.titleMedium)

            val assigneeLabel = chore.currentAssignee ?: stringResource(R.string.chore_completer_label)
            Text(text = assigneeLabel, style = MaterialTheme.typography.bodyMedium)

            Text(
                text = stringResource(R.string.chore_points_format, chore.points),
                style = MaterialTheme.typography.bodySmall
            )
            Text(text = chore.state, style = MaterialTheme.typography.bodySmall)
            chore.nextDue?.let {
                Text(
                    text = stringResource(R.string.chore_next_due_format, it),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (isCompleting) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                } else {
                    Button(onClick = onCompleteClick) {
                        Text(stringResource(R.string.complete_chore_button))
                    }
                }
            }
        }
    }
}
