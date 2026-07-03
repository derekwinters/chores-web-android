package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState

/**
 * Issue #24: household default theme management — 6 built-in themes (protected server-side; the
 * real API exposes no `is_builtin` flag) plus custom themes (create via copy, rename, delete).
 *
 * Thin Hilt-wired wrapper around [ThemeAdminContent].
 */
@Composable
fun ThemeAdminScreen(modifier: Modifier = Modifier, viewModel: ThemeAdminViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val defaultThemeId by viewModel.defaultThemeId.collectAsState()

    ThemeAdminContent(
        modifier = modifier,
        uiState = uiState,
        defaultThemeId = defaultThemeId,
        onSetDefault = viewModel::setDefaultTheme,
        onCreate = viewModel::createTheme,
        onRename = viewModel::renameTheme,
        onDelete = viewModel::deleteTheme
    )
}

@Composable
fun ThemeAdminContent(
    uiState: UiState<List<ThemeOption>>,
    onSetDefault: (String) -> Unit,
    onCreate: (name: String, sourceTheme: ThemeOption) -> Unit,
    onRename: (themeId: String, newName: String) -> Unit,
    onDelete: (String) -> Unit,
    defaultThemeId: String? = null,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTheme by remember { mutableStateOf<ThemeOption?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(uiState.data, key = { it.id }) { theme ->
                        ThemeRow(
                            theme = theme,
                            isActive = defaultThemeId == theme.id,
                            onSetDefault = { onSetDefault(theme.id) },
                            onEdit = { editingTheme = theme }
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Create theme") }
            }
        }
    }

    if (showCreateDialog) {
        val firstTheme = (uiState as? UiState.Success)?.data?.firstOrNull()
        CreateThemeDialog(
            onCreate = { name ->
                if (firstTheme != null) onCreate(name, firstTheme)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingTheme?.let { theme ->
        EditThemeDialog(
            theme = theme,
            onSave = { newName ->
                onRename(theme.id, newName)
                editingTheme = null
            },
            onDelete = {
                onDelete(theme.id)
                editingTheme = null
            },
            onDismiss = { editingTheme = null }
        )
    }
}

@Composable
private fun ThemeRow(
    theme: ThemeOption,
    isActive: Boolean = false,
    onSetDefault: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onSetDefault)
            .then(
                if (isActive) {
                    Modifier.shadow(elevation = 12.dp, shape = RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf(theme.primary, theme.secondary, theme.accent, theme.background).forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(4.dp)
                            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .background(parseHexColor(hex), RoundedCornerShape(8.dp))
                    )
                }
                Text(theme.name, modifier = Modifier.padding(start = 8.dp))
            }
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}

@Composable
private fun CreateThemeDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Custom Theme") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditThemeDialog(
    theme: ThemeOption,
    onSave: (newName: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(theme.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // No `isBuiltin` flag exists in the real API to gate this client-side; rename/delete of a
    // protected built-in theme is simply expected to fail server-side (surfaced via actionState).
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Theme") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete Theme") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this theme?") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
