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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateListOf
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
 * Issue #130: the edit dialog also exposes the full 9-color palette as hex inputs with live
 * swatch previews, saved via the update endpoint. Issue #131: each row has a "Copy" action that
 * seeds the create dialog with that theme's colors and a "<name> Copy" default name.
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
        onUpdateColors = viewModel::updateColors,
        onDelete = viewModel::deleteTheme
    )
}

@Composable
fun ThemeAdminContent(
    uiState: UiState<List<ThemeOption>>,
    onSetDefault: (String) -> Unit,
    onCreate: (name: String, sourceTheme: ThemeOption) -> Unit,
    onRename: (themeId: String, newName: String) -> Unit,
    onUpdateColors: (themeId: String, colors: ThemeOption) -> Unit,
    onDelete: (String) -> Unit,
    defaultThemeId: String? = null,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTheme by remember { mutableStateOf<ThemeOption?>(null) }
    var copySourceTheme by remember { mutableStateOf<ThemeOption?>(null) }

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
                            onCopy = { copySourceTheme = theme },
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

    // Issue #131: "copy theme" — same create-via-copy flow as the FAB, but seeded from the tapped
    // row's theme (the real backend has no server-side copy; createTheme resends its colors as the
    // new theme's palette).
    copySourceTheme?.let { source ->
        CreateThemeDialog(
            initialName = "${source.name} Copy",
            onCreate = { name ->
                onCreate(name, source)
                copySourceTheme = null
            },
            onDismiss = { copySourceTheme = null }
        )
    }

    editingTheme?.let { theme ->
        EditThemeDialog(
            theme = theme,
            onSave = { newName, updatedColors ->
                // Rename and color editing hit separate endpoints (PATCH rename vs. PATCH
                // update), so only issue the call(s) whose fields actually changed.
                if (newName != theme.name) onRename(theme.id, newName)
                if (updatedColors != theme) onUpdateColors(theme.id, updatedColors)
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
    onCopy: () -> Unit,
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
            Row {
                TextButton(onClick = onCopy) { Text("Copy") }
                TextButton(onClick = onEdit) { Text("Edit") }
            }
        }
    }
}

@Composable
private fun CreateThemeDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = ""
) {
    var name by remember { mutableStateOf(initialName) }
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
    onSave: (newName: String, updatedColors: ThemeOption) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(theme.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Issue #130: the 9 editable palette colors, in ThemeColorsDto order. Hex-text-input plus a
    // live swatch preview only — no native color-picker widget.
    val colorLabels = listOf(
        "Background", "Surface", "Surface 2", "Accent", "Primary",
        "Secondary", "Success", "Warning", "Error"
    )
    val colorValues = remember(theme) {
        mutableStateListOf(
            theme.background, theme.surface, theme.surface2, theme.accent, theme.primary,
            theme.secondary, theme.success, theme.warning, theme.error
        )
    }
    val allColorsValid = colorValues.all { isValidHexColor(it) }

    // No `isBuiltin` flag exists in the real API to gate this client-side; rename/delete/edit of a
    // protected built-in theme is simply expected to fail server-side (surfaced via actionState).
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Theme") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                colorLabels.forEachIndexed { index, label ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = colorValues[index],
                            onValueChange = { colorValues[index] = it },
                            label = { Text(label) },
                            isError = !isValidHexColor(colorValues[index]),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .background(parseHexColor(colorValues[index]), RoundedCornerShape(8.dp))
                        )
                    }
                }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete Theme") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        theme.copy(
                            background = colorValues[0],
                            surface = colorValues[1],
                            surface2 = colorValues[2],
                            accent = colorValues[3],
                            primary = colorValues[4],
                            secondary = colorValues[5],
                            success = colorValues[6],
                            warning = colorValues[7],
                            error = colorValues[8]
                        )
                    )
                },
                enabled = name.isNotBlank() && allColorsValid
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
