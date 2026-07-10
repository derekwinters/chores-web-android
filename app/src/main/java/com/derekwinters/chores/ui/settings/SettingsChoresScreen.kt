package com.derekwinters.chores.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #88: Chores settings section screen (independently-routed, shared SettingsViewModel scoped
 * to settings nav graph).
 */
@Composable
fun SettingsChoresScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings")),
    onNavigateToData: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    SettingsChoresContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        onSave = viewModel::save,
        onNavigateToData = onNavigateToData
    )
}

@Composable
fun SettingsChoresContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    onSave: (AppConfig) -> Unit,
    onNavigateToData: () -> Unit = {},
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
                var dueHourMenuExpanded by remember { mutableStateOf(false) }
                val availableHours = remember { DueHourUtils.getAvailableHours() }
                val currentHourOption = remember(draft.dueTimeHour) {
                    DueHourUtils.findHourOption(draft.dueTimeHour)
                        ?: HourOption(draft.dueTimeHour, DueHourUtils.formatHourLabel(draft.dueTimeHour))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.lg)
                ) {
                    Divider(modifier = Modifier.padding(bottom = Space.lg))
                    Text("Chores Settings", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Configure when chores are marked as due soon and when the due time resets each day.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Space.xs)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.lg),
                        value = draft.dueSoonDays.toString(),
                        onValueChange = { value -> value.toIntOrNull()?.let { draft = draft.copy(dueSoonDays = it) } },
                        label = { Text("Due Soon Days") }
                    )

                    Text(
                        "Due Time Hour",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.lg)
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(top = Space.sm)) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dueHourMenuExpanded = true }
                                .testTag("DueHourField"),
                            value = currentHourOption.displayLabel,
                            onValueChange = {},
                            label = { Text("Due Time Hour") },
                            readOnly = true
                        )
                        DropdownMenu(
                            expanded = dueHourMenuExpanded,
                            onDismissRequest = { dueHourMenuExpanded = false }
                        ) {
                            availableHours.forEach { hour ->
                                DropdownMenuItem(
                                    text = { Text(hour.displayLabel) },
                                    onClick = {
                                        draft = draft.copy(dueTimeHour = hour.hour)
                                        dueHourMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TextButton(onClick = onNavigateToData) { Text("Data (Export/Import, Points Log)") }

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
