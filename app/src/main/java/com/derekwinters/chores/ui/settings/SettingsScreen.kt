package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.UpdateCheckStatus
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDateTime

/** Issue #20/#21/#22/#24: cross-screen nav callbacks the Settings destination needs. */
data class SettingsNavActions(
    val onNavigateToAuthLog: () -> Unit = {},
    val onNavigateToData: () -> Unit = {},
    val onNavigateToTheming: () -> Unit = {}
)

/**
 * Issue #88: Menu row callbacks for Settings sub-navigation (General, Auth, Chores, Theme, Data,
 * About).
 */
data class SettingsMenuNavActions(
    val onNavigateToGeneral: () -> Unit = {},
    val onNavigateToAuth: () -> Unit = {},
    val onNavigateToChores: () -> Unit = {},
    val onNavigateToTheme: () -> Unit = {},
    val onNavigateToData: () -> Unit = {},
    val onNavigateToAbout: () -> Unit = {}
)

/**
 * Issue #20: General/Auth/Chores/About settings forms, all against the shared config. Gated to
 * admins client-side (drawer hides Settings for non-admins; see ChoresApp) — the backend's
 * config-write endpoint isn't itself admin-gated, matching chores-web's own convention.
 *
 * Thin Hilt-wired wrapper around [SettingsContent].
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    navActions: SettingsNavActions = SettingsNavActions(),
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    SettingsContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        updateStatus = updateStatus,
        navActions = navActions,
        onSave = viewModel::save,
        onCheckForUpdates = viewModel::checkForUpdates
    )
}

@Composable
fun SettingsContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    updateStatus: UpdateCheckStatus?,
    navActions: SettingsNavActions,
    onSave: (AppConfig) -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                var draft by remember(uiState.data) { mutableStateOf(uiState.data) }
                val isSaving = saveState is UiState.Loading

                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text("General", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = draft.appTitle,
                        onValueChange = { draft = draft.copy(appTitle = it) },
                        label = { Text("App Title") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = draft.timezone,
                        onValueChange = { draft = draft.copy(timezone = it) },
                        label = { Text("Timezone") }
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Auth", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Require Authentication")
                        Switch(checked = draft.authEnabled, onCheckedChange = { draft = draft.copy(authEnabled = it) })
                    }
                    TextButton(onClick = navActions.onNavigateToAuthLog) { Text("Auth Event Log") }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Chores", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = draft.dueSoonDays.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { draft = draft.copy(dueSoonDays = it) } },
                        label = { Text("Notify when due in — N days") }
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        value = draft.dueTimeHour.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { draft = draft.copy(dueTimeHour = it) } },
                        label = { Text("Mark chores due at — hour") }
                    )

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = navActions.onNavigateToTheming) { Text("Household Default Theme") }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("Data", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = navActions.onNavigateToData) { Text("Data (Export/Import, Points Log)") }

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Current version: ${updateStatus?.currentVersion ?: "unknown"}")
                    Text("Latest version: ${updateStatus?.latestVersion ?: "unknown"}")
                    if (updateStatus?.updateAvailable == true) {
                        Text("Update available!", color = MaterialTheme.colorScheme.error)
                    }
                    Text("Last checked: ${updateStatus?.lastCheckedAt?.let(::formatDateTime) ?: "never"}")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Check for updates automatically")
                        Switch(
                            checked = draft.updateCheckEnabled,
                            onCheckedChange = { draft = draft.copy(updateCheckEnabled = it) }
                        )
                    }
                    TextButton(onClick = onCheckForUpdates) { Text("Check Now") }

                    if (saveState is UiState.Error) {
                        Text(saveState.message, color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        modifier = Modifier.padding(top = 16.dp),
                        onClick = { onSave(draft) },
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        Text("Save Settings")
                    }
                }
            }
        }
    }
}

/**
 * Issue #88: Settings menu showing sections (General, Auth, Chores, Theme, Data, About) as
 * independently-routed pages, matching chores-web's SettingsLayout.jsx sub-nav structure.
 *
 * Issue #167: [ChoresDestination.Settings][com.derekwinters.chores.ui.ChoresDestination.Settings]
 * lost its top-level `adminOnly` gate (Settings is now a fixed bottom-nav tab, visible to
 * everyone, since it must remain reachable for the folded-in Preferences entry below) — the
 * admin-only rows that mutate household config (General/Auth/Chores/Theme/Data) are gated here at
 * the row level via [isAdmin] instead. Preferences (the personal theme picker, previously only
 * reachable via the avatar dropdown) and About stay visible to all users.
 */
@Composable
fun SettingsMenuContent(
    isAdmin: Boolean,
    onNavigateToGeneral: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToChores: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToData: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPreferences: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsMenuItem(label = "Preferences", onClick = onNavigateToPreferences)
        if (isAdmin) {
            SettingsMenuItem(label = "General", onClick = onNavigateToGeneral)
            SettingsMenuItem(label = "Auth", onClick = onNavigateToAuth)
            SettingsMenuItem(label = "Chores", onClick = onNavigateToChores)
            SettingsMenuItem(label = "Theme", onClick = onNavigateToTheme)
            SettingsMenuItem(label = "Data", onClick = onNavigateToData)
        }
        SettingsMenuItem(label = "About", onClick = onNavigateToAbout)
    }
}

@Composable
private fun SettingsMenuItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
