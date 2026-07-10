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
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #88: Auth settings section screen (independently-routed, shared SettingsViewModel scoped
 * to settings nav graph).
 */
@Composable
fun SettingsAuthScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings")),
    onNavigateToAuthLog: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    SettingsAuthContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        onSave = viewModel::save,
        onNavigateToAuthLog = onNavigateToAuthLog
    )
}

@Composable
fun SettingsAuthContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    onSave: (AppConfig) -> Unit,
    onNavigateToAuthLog: () -> Unit = {},
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
                    Text("Auth Settings", style = MaterialTheme.typography.titleMedium)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.sm),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Require Authentication")
                        Switch(
                            modifier = Modifier.testTag("authEnabledSwitch"),
                            checked = draft.authEnabled,
                            onCheckedChange = { draft = draft.copy(authEnabled = it) }
                        )
                    }

                    Text(
                        text = "When enabled, users must enter a password to access the app. Disable to allow unrestricted access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.sm)
                    )

                    Divider(modifier = Modifier.padding(vertical = Space.lg))

                    Text("Auth Event Log", style = MaterialTheme.typography.titleMedium)

                    TextButton(onClick = onNavigateToAuthLog) { Text("View Event Log") }

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
