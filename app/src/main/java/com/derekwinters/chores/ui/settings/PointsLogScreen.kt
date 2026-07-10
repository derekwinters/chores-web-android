package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.PointsLogEntry
import com.derekwinters.chores.data.repository.PointsLogPage
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #23: admin table for directly correcting historical point credits — paginated (offset-
 * based server-side, per [PointsLogRepository.PAGE_SIZE]), inline edit (person and/or points),
 * and delete with a confirmation warning.
 *
 * Thin Hilt-wired wrapper around [PointsLogContent].
 */
@Composable
fun PointsLogScreen(modifier: Modifier = Modifier, viewModel: PointsLogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    PointsLogContent(
        modifier = modifier,
        uiState = uiState,
        onUpdate = viewModel::updateEntry,
        onDelete = viewModel::deleteEntry,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage
    )
}

@Composable
fun PointsLogContent(
    uiState: UiState<PointsLogPage>,
    onUpdate: (entryId: Int, person: String, points: Int) -> Unit,
    onDelete: (Int) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingEntry by remember { mutableStateOf<PointsLogEntry?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Text("Points Log", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(Space.lg))

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (uiState) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val page = uiState.data
                    if (page.entries.isEmpty()) {
                        Text(modifier = Modifier.align(Alignment.Center), text = "No points log entries")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(page.entries, key = { it.id }) { entry ->
                                PointsLogRow(entry, onClick = { editingEntry = entry })
                            }
                        }
                    }
                }
            }
        }

        if (uiState is UiState.Success) {
            val page = uiState.data
            // Issue #124: show the currently displayed range ("Showing X–Y of Z", matching web)
            // instead of only the total count. The range end is clamped to the total for a short
            // final page; an empty log reads "Showing 0–0 of 0".
            val rangeStart = if (page.total == 0) 0 else page.offset + 1
            val rangeEnd = minOf(page.offset + page.limit, page.total)
            Row(modifier = Modifier.fillMaxWidth().padding(Space.sm), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPreviousPage, enabled = page.offset > 0) { Text("Previous") }
                Text("Showing $rangeStart–$rangeEnd of ${page.total}")
                TextButton(onClick = onNextPage, enabled = page.offset + page.limit < page.total) { Text("Next") }
            }
        }
    }

    editingEntry?.let { entry ->
        EditPointsLogDialog(
            entry = entry,
            onSave = { person, points ->
                onUpdate(entry.id, person, points)
                editingEntry = null
            },
            onDelete = {
                onDelete(entry.id)
                editingEntry = null
            },
            onDismiss = { editingEntry = null }
        )
    }
}

@Composable
private fun PointsLogRow(entry: PointsLogEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.sm, vertical = Space.xs)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(Space.md), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(entry.person, style = MaterialTheme.typography.titleSmall)
                // Issue #121: web's data-correction table shows each entry's ID so admins can
                // correlate rows during data correction — mirror it in the row metadata line.
                Text("ID ${entry.id} · Chore #${entry.choreId} · ${formatDateTime(entry.completedAt)}", style = MaterialTheme.typography.bodySmall)
            }
            Text("${entry.points} pts")
        }
    }
}

@Composable
private fun EditPointsLogDialog(
    entry: PointsLogEntry,
    onSave: (person: String, points: Int) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var person by remember { mutableStateOf(entry.person) }
    var pointsText by remember { mutableStateOf(entry.points.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Points Entry") },
        text = {
            Column {
                // The backend's PointsLogUpdate takes `person` as a username string (reassigning
                // the entry to a different person is valid), not a person id — a free-text field
                // matches that directly without needing a separate id lookup.
                OutlinedTextField(value = person, onValueChange = { person = it }, label = { Text("Person (username)") })
                OutlinedTextField(value = pointsText, onValueChange = { pointsText = it }, label = { Text("Points") })
                // Issue #122: destructive Delete action styled in the error color, consistent
                // with User Management's red-Delete treatment.
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Entry") }
            }
        },
        confirmButton = {
            TextButton(onClick = { pointsText.toIntOrNull()?.let { onSave(person, it) } }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            // Issue #121: include the entry ID so admins can confirm they are deleting the
            // intended row, matching web's delete-confirmation copy.
            title = { Text("Delete entry #${entry.id}?") },
            text = { Text("This will reverse the points on the person, floored at 0, and cannot be undone.") },
            // Issue #122: confirming the delete is the destructive step — same red treatment.
            confirmButton = {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
