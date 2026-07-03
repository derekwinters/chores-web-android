package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.AuthLogEntry
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime

/**
 * Issue #21: admin-only audit log for auth-related events, separate from the chore Activity Log.
 *
 * Thin Hilt-wired wrapper around [AuthLogContent].
 */
@Composable
fun AuthLogScreen(modifier: Modifier = Modifier, viewModel: AuthLogViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val filters by viewModel.filters.collectAsState()

    AuthLogContent(
        modifier = modifier,
        uiState = uiState,
        filters = filters,
        onFiltersChange = viewModel::updateFilters,
        onNextPage = viewModel::nextPage,
        onPreviousPage = viewModel::previousPage
    )
}

@Composable
fun AuthLogContent(
    uiState: UiState<AuthLogPageState>,
    filters: AuthLogFilters,
    onFiltersChange: (AuthLogFilters) -> Unit,
    onNextPage: () -> Unit,
    onPreviousPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            value = filters.username.orEmpty(),
            onValueChange = { value -> onFiltersChange(filters.copy(username = value.ifBlank { null })) },
            label = { Text("Username") },
            singleLine = true
        )

        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            when (uiState) {
                is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error -> Text(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
                is UiState.Success -> {
                    val page = uiState.data
                    if (page.entries.isEmpty()) {
                        Text(modifier = Modifier.align(Alignment.Center), text = "No auth events found")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(page.entries, key = { it.id }) { entry -> AuthLogRow(entry) }
                        }
                    }
                }
            }
        }

        if (uiState is UiState.Success) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPreviousPage, enabled = uiState.data.page > 1) { Text("Previous") }
                Text("Page ${uiState.data.page} (${uiState.data.total} total)")
                TextButton(onClick = onNextPage, enabled = uiState.data.page < uiState.data.totalPages) { Text("Next") }
            }
        }
    }
}

@Composable
private fun AuthLogRow(entry: AuthLogEntry) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(entry.action, style = MaterialTheme.typography.titleSmall)
            Text(entry.username, style = MaterialTheme.typography.bodyMedium)
            Text(formatDateTime(entry.timestamp), style = MaterialTheme.typography.bodySmall)
            entry.changedBy?.let { Text("Changed by: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}
