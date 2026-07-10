package com.derekwinters.chores.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.data.model.Person
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.theme.AvatarSize
import com.derekwinters.chores.ui.theme.Space
import com.derekwinters.chores.ui.theme.StrokeWidth

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
                modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val admins = uiState.data.filter { it.isAdmin }
                val members = uiState.data.filter { !it.isAdmin }
                LazyColumn(modifier = Modifier.fillMaxSize().padding(Space.lg)) {
                    item { SectionHeader("Administrators") }
                    if (admins.isEmpty()) {
                        item { Text("No administrators") }
                    } else {
                        items(admins, key = { it.id }) { person ->
                            PersonRow(
                                person,
                                onEditClick = { editingPerson = person },
                                onDeleteClick = { onDelete(person.id) },
                                onHistoryClick = { onHistoryClick(person.username) }
                            )
                        }
                    }
                    item { SectionHeader("Members", modifier = Modifier.padding(top = Space.lg)) }
                    if (members.isEmpty()) {
                        item { Text("No members") }
                    } else {
                        items(members, key = { it.id }) { person ->
                            PersonRow(
                                person,
                                onEditClick = { editingPerson = person },
                                onDeleteClick = { onDelete(person.id) },
                                onHistoryClick = { onHistoryClick(person.username) }
                            )
                        }
                    }
                }

                // Issue #94: extended FAB with an "Add User" text label alongside the icon,
                // consistent with the Chores List's labeled "Add Chore" FAB (issue #70).
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(Space.lg),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_user)) }
                )
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

/**
 * Issue #90: section headers ("Administrators" / "Members") render uppercase with a short
 * accent-colored underline beneath, matching web's section-header treatment. The underlying
 * string passed to [Text] is uppercased directly (Compose has no CSS-style text-transform), so
 * the semantics tree reflects exactly what's rendered.
 */
@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .padding(top = Space.xs)
                .width(Space.xxl)
                .height(StrokeWidth.emphasis)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun PersonRow(
    person: Person,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    // Issue #86: a per-row delete confirmation, independent of the Edit dialog's own delete
    // flow, so Delete is a directly visible/actionable row control rather than buried in a dialog.
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = Space.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Space.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.clickableRow(onEditClick), verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(person)
                Column(modifier = Modifier.padding(start = Space.md)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            person.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("personDisplayName_${person.id}")
                        )
                        RolePill(
                            isAdmin = person.isAdmin,
                            modifier = Modifier.padding(start = Space.sm),
                            textTestTag = "personRolePill_${person.id}"
                        )
                    }
                    Text(person.username, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onHistoryClick) { Text("History") }
                IconButton(onClick = onEditClick, modifier = Modifier.testTag("personEdit_${person.id}")) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit ${person.displayName}")
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.testTag("personDelete_${person.id}")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete ${person.displayName}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this user?") },
            text = { Text("History, points, and log entries are not deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteClick()
                    showDeleteConfirm = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }
}

/**
 * Issue #78: avatar circle (display-name initial) per user row, matching web's per-row avatar
 * and the same visual treatment as the top bar's user-menu avatar (see [ChoresApp]).
 */
@Composable
private fun PersonAvatar(person: Person, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(AvatarSize.lg)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
    ) {
        Text(
            text = person.displayName.take(1).uppercase(),
            color = MaterialTheme.colorScheme.onTertiary,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Issue #82: admin/member role pill badge shown next to each row's display name, so role is
 * visible per-row rather than relying solely on which section (Administrators/Members) a user
 * appears under.
 *
 * [textTestTag], if provided, is applied to the inner [Text] rather than the outer [Box] — the
 * text semantics live on the Text node itself, so tagging the Box (a separate, un-merged node)
 * would leave the tagged node with no text of its own and break text-based test assertions.
 */
@Composable
private fun RolePill(isAdmin: Boolean, modifier: Modifier = Modifier, textTestTag: String? = null) {
    val containerColor: Color
    val contentColor: Color
    val label: String
    if (isAdmin) {
        containerColor = MaterialTheme.colorScheme.primaryContainer
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        label = "Admin"
    } else {
        containerColor = MaterialTheme.colorScheme.surfaceVariant
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        label = "Member"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(containerColor)
            .padding(horizontal = Space.sm, vertical = Space.xxs)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = if (textTestTag != null) Modifier.testTag(textTestTag) else Modifier
        )
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
