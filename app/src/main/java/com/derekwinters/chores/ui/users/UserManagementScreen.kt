package com.derekwinters.chores.ui.users

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.ui.UiState

/**
 * Issue #18: admin-only user management — list grouped Administrators/Members, create, edit,
 * delete, and a per-user "History" link (wired via [onHistoryClick]).
 *
 * Thin Hilt-wired wrapper around [UserManagementContent].
 */
@Composable
fun UserManagementScreen(
    modifier: Modifier = Modifier,
    onHistoryClick: (username: String) -> Unit = {},
    viewModel: UserManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    UserManagementContent(
        modifier = modifier,
        uiState = uiState,
        actionState = actionState,
        onCreate = viewModel::createUser,
        onUpdate = viewModel::updateUser,
        onDelete = viewModel::deleteUser,
        onDismissActionError = viewModel::clearActionState,
        onHistoryClick = onHistoryClick
    )
}

@Composable
fun UserManagementContent(
    uiState: UiState<List<Person>>,
    actionState: UiState<Unit>,
    onCreate: (displayName: String, password: String) -> Unit,
    onUpdate: (personId: Int, displayName: String, username: String, goal7d: Int, goal30d: Int, password: String, isAdmin: Boolean?) -> Unit,
    onDelete: (Int) -> Unit,
    onDismissActionError: () -> Unit,
    onHistoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val admins = uiState.data.filter { it.isAdmin }
                val members = uiState.data.filter { !it.isAdmin }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item { Text("Administrators", style = MaterialTheme.typography.titleMedium) }
                    if (admins.isEmpty()) {
                        item { Text("No administrators") }
                    } else {
                        items(admins, key = { it.id }) { person ->
                            PersonRow(person, onClick = { editingPerson = person }, onHistoryClick = { onHistoryClick(person.username) })
                        }
                    }
                    item { Text("Members", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium) }
                    if (members.isEmpty()) {
                        item { Text("No members") }
                    } else {
                        items(members, key = { it.id }) { person ->
                            PersonRow(person, onClick = { editingPerson = person }, onHistoryClick = { onHistoryClick(person.username) })
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "Add user") }
            }
        }
    }

    if (showCreateDialog) {
        CreateUserDialog(
            onCreate = { name, password ->
                onCreate(name, password)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingPerson?.let { person ->
        val isOnlyPerson = (uiState as? UiState.Success)?.data?.size == 1
        EditUserDialog(
            person = person,
            disableAdminToggle = isOnlyPerson,
            onSave = { displayName, username, goal7d, goal30d, password, isAdmin ->
                onUpdate(person.id, displayName, username, goal7d, goal30d, password, isAdmin)
                editingPerson = null
            },
            onDelete = {
                onDelete(person.id)
                editingPerson = null
            },
            onDismiss = { editingPerson = null }
        )
    }

    if (actionState is UiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissActionError,
            title = { Text("Action failed") },
            text = { Text(actionState.message) },
            confirmButton = { TextButton(onClick = onDismissActionError) { Text("OK") } }
        )
    }
}

@Composable
private fun PersonRow(person: Person, onClick: () -> Unit, onHistoryClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.clickableRow(onClick)) {
                Text(person.displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(person.username, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onHistoryClick) { Text("History") }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable(onClick = onClick))

@Composable
private fun CreateUserDialog(onCreate: (String, String) -> Unit, onDismiss: () -> Unit) {
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create User") },
        text = {
            Column {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(displayName, password) },
                enabled = displayName.isNotBlank() && password.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EditUserDialog(
    person: Person,
    disableAdminToggle: Boolean,
    onSave: (displayName: String, username: String, goal7d: Int, goal30d: Int, password: String, isAdmin: Boolean?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf(person.displayName) }
    var username by remember { mutableStateOf(person.username) }
    var goal7d by remember { mutableStateOf(person.goal7d.toString()) }
    var goal30d by remember { mutableStateOf(person.goal30d.toString()) }
    var password by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(person.isAdmin) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit User") },
        text = {
            Column {
                OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("Display Name") })
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
                OutlinedTextField(value = goal7d, onValueChange = { goal7d = it }, label = { Text("7-day goal") })
                OutlinedTextField(value = goal30d, onValueChange = { goal30d = it }, label = { Text("30-day goal") })
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password (leave blank to keep)") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Administrator")
                    Switch(checked = isAdmin, onCheckedChange = { isAdmin = it }, enabled = !disableAdminToggle)
                }
                TextButton(onClick = { showDeleteConfirm = true }) { Text("Delete User") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(displayName, username, goal7d.toIntOrNull() ?: person.goal7d, goal30d.toIntOrNull() ?: person.goal30d, password, isAdmin)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this user?") },
            text = { Text("History, points, and log entries are not deleted.") },
            confirmButton = { TextButton(onClick = onDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}
