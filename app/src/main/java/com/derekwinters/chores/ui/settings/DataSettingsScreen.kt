package com.derekwinters.chores.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.theme.Space
import kotlinx.coroutines.launch

/** Issue #22: cross-screen nav callback the Data settings destination needs. */
data class DataSettingsNavActions(val onNavigateToPointsLog: () -> Unit = {})

/**
 * Issue #22: config export/import backup and log-retention setting, plus a nav entry to the
 * Admin Points Log editor (issue #23).
 *
 * Thin Hilt-wired wrapper around [DataSettingsContent].
 */
@Composable
fun DataSettingsScreen(
    modifier: Modifier = Modifier,
    navActions: DataSettingsNavActions = DataSettingsNavActions(),
    viewModel: DataSettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportState by viewModel.exportState.collectAsState()
    val importPreview by viewModel.importPreview.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val logRetentionDays by viewModel.logRetentionDays.collectAsState()
    val selectedImportFilename by viewModel.selectedImportFilename.collectAsState()
    val exportFilename by viewModel.exportFilename.collectAsState()

    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }

    fun extractFilename(uri: Uri): String {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        } ?: "file"
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            val filename = extractFilename(uri)
            viewModel.setExportFilename(filename)
            viewModel.exportConfig()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val filename = extractFilename(uri)
            viewModel.setSelectedImportFilename(filename)
            scope.launch {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    viewModel.previewImport(reader.readText())
                }
            }
        }
    }

    LaunchedEffect(exportState, pendingExportUri) {
        val json = (exportState as? UiState.Success)?.data
        val uri = pendingExportUri
        if (json != null && uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            pendingExportUri = null
            viewModel.clearExportState()
        }
    }

    LaunchedEffect(importState) {
        if (importState is UiState.Success || importState is UiState.Error) {
            viewModel.setSelectedImportFilename(null)
        }
    }

    val logRetentionInput by viewModel.logRetentionInput.collectAsState()
    val logRetentionState by viewModel.logRetentionState.collectAsState()

    DataSettingsContent(
        modifier = modifier,
        logRetentionDays = logRetentionDays,
        logRetentionInput = logRetentionInput,
        logRetentionState = logRetentionState,
        importPreview = importPreview,
        importState = importState,
        selectedImportFilename = selectedImportFilename,
        exportFilename = exportFilename,
        exportState = exportState,
        onExportClick = { exportLauncher.launch("chores-backup.json") },
        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
        onLogRetentionChange = viewModel::updateLogRetentionInput,
        onSaveLogRetention = viewModel::saveLogRetentionDays,
        onClearLogRetentionState = viewModel::clearLogRetentionState,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onDismissImportResult = viewModel::clearImportState,
        onNavigateToPointsLog = navActions.onNavigateToPointsLog
    )
}

@Composable
fun DataSettingsContent(
    logRetentionDays: Int?,
    logRetentionInput: String,
    logRetentionState: UiState<Int>,
    importPreview: ImportPreview?,
    importState: UiState<com.derekwinters.chores.data.repository.ImportSummary>,
    selectedImportFilename: String?,
    exportFilename: String?,
    exportState: UiState<String>,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onLogRetentionChange: (String) -> Unit,
    onSaveLogRetention: () -> Unit,
    onClearLogRetentionState: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onDismissImportResult: () -> Unit,
    onNavigateToPointsLog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(Space.lg)) {
        Divider(modifier = Modifier.padding(bottom = Space.lg))
        Text("Data", style = MaterialTheme.typography.titleMedium)

        // ========== Export & Import Section ==========
        Text("Export & Import", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = Space.lg))
        Text(
            "Back up your data by exporting to a file, or restore from a previous backup by importing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.sm)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Column(modifier = Modifier.weight(1f)) {
                Button(onClick = onExportClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Export Backup")
                }
                Text(
                    "Download your data as a JSON file for backup or migration purposes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.xs)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Button(onClick = onImportClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Import Backup")
                }
                Text(
                    "Upload a previously exported JSON file to restore or migrate your data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.xs)
                )
            }
        }

        // Show selected import filename confirmation
        if (selectedImportFilename != null) {
            Text(
                "Selected: $selectedImportFilename",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Space.md)
            )
        }

        // Show export state
        if (exportState is UiState.Success) {
            SettingsBanner(
                message = "Data exported successfully to: ${exportFilename ?: "file"}",
                type = BannerType.SUCCESS,
                modifier = Modifier.padding(top = Space.md)
            )
        } else if (exportState is UiState.Error) {
            SettingsBanner(
                message = exportState.message,
                type = BannerType.ERROR,
                modifier = Modifier.padding(top = Space.md)
            )
        }

        // ========== Log Retention Section ==========
        Divider(modifier = Modifier.padding(top = Space.lg, bottom = Space.lg))
        Text("Log Retention", style = MaterialTheme.typography.titleSmall)
        Text(
            "Control how long activity logs are kept. Older entries will be automatically deleted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.sm)
        )

        if (logRetentionInput.isNotEmpty()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().padding(top = Space.md),
                value = logRetentionInput,
                onValueChange = onLogRetentionChange,
                label = { Text("Days to keep log entries") }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = Space.md), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            Button(onClick = onSaveLogRetention, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
        }

        if (logRetentionState is UiState.Success) {
            SettingsBanner(
                message = "Log retention updated to ${logRetentionState.data} days",
                type = BannerType.SUCCESS,
                modifier = Modifier.padding(top = Space.sm)
            )
            TextButton(onClick = onClearLogRetentionState, modifier = Modifier.padding(top = Space.none)) {
                Text("OK")
            }
        } else if (logRetentionState is UiState.Error) {
            SettingsBanner(
                message = logRetentionState.message,
                type = BannerType.ERROR,
                modifier = Modifier.padding(top = Space.sm)
            )
            TextButton(onClick = onClearLogRetentionState, modifier = Modifier.padding(top = Space.none)) {
                Text("OK")
            }
        }

        // ========== Data Management Section ==========
        Divider(modifier = Modifier.padding(top = Space.lg, bottom = Space.lg))
        Text("Data Management", style = MaterialTheme.typography.titleSmall)
        Text(
            "Access detailed information about your activity logs.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.sm)
        )

        TextButton(modifier = Modifier.padding(top = Space.md), onClick = onNavigateToPointsLog) {
            Text("Admin Points Log")
        }

        if (importState is UiState.Success) {
            SettingsBanner(
                message = "Imported ${importState.data.peopleCount} people, ${importState.data.choresCount} chores, " +
                    "${importState.data.settingsCount} settings",
                type = BannerType.SUCCESS,
                modifier = Modifier.padding(top = Space.lg)
            )
            TextButton(onClick = onDismissImportResult) { Text("OK") }
        } else if (importState is UiState.Error) {
            SettingsBanner(message = importState.message, type = BannerType.ERROR, modifier = Modifier.padding(top = Space.lg))
            TextButton(onClick = onDismissImportResult) { Text("OK") }
        }
    }

    if (importPreview != null) {
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Confirm Import") },
            text = {
                Column {
                    Text("${importPreview.peopleCount} people, ${importPreview.choresCount} chores, ${importPreview.settingsCount} settings")
                    Text(
                        "This replaces all existing data and cannot be undone.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("Import") } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Cancel") } }
        )
    }
}
