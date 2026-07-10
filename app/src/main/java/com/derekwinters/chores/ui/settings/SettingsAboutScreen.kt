package com.derekwinters.chores.ui.settings

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.UpdateCheckStatus
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.common.formatDateTime
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #88: About settings section screen (independently-routed, shared SettingsViewModel scoped
 * to settings nav graph).
 */
@Composable
fun SettingsAboutScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings"))
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()

    SettingsAboutContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        updateStatus = updateStatus,
        onSave = viewModel::save,
        onCheckForUpdates = viewModel::checkForUpdates
    )
}

@Composable
fun SettingsAboutContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    updateStatus: UpdateCheckStatus?,
    onSave: (AppConfig) -> Unit,
    onCheckForUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                var draft by remember(uiState.data) { mutableStateOf(uiState.data) }
                val isSaving = saveState is UiState.Loading
                val isDirty = draft != uiState.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.lg)
                ) {
                    Divider(modifier = Modifier.padding(bottom = Space.lg))
                    Text("About", style = MaterialTheme.typography.titleMedium)

                    Text("Current version: ${updateStatus?.currentVersion ?: "unknown"}", modifier = Modifier.padding(top = Space.sm))
                    Text("Latest version: ${updateStatus?.latestVersion ?: "unknown"}")
                    if (updateStatus?.updateAvailable == true) {
                        Text("Update available!", color = MaterialTheme.colorScheme.error)
                    }
                    Text("Last checked: ${updateStatus?.lastCheckedAt?.let(::formatDateTime) ?: "never"}")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.sm),
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
                        SettingsBanner(message = saveState.message, type = BannerType.ERROR, modifier = Modifier.padding(top = Space.sm))
                    }

                    Button(
                        modifier = Modifier.padding(top = Space.lg),
                        onClick = { onSave(draft) },
                        enabled = isDirty && !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = Space.sm))
                        Text("Save")
                    }
                }
            }
        }
    }
}
