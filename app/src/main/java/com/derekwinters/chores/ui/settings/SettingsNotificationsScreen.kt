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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.theme.Space
import java.util.Locale

/**
 * Issue #44: per-user Notifications settings — the WorkManager poll interval and offline alert
 * (device-local) plus the server-side per-type preferences. Reachable by every user, not just
 * admins (see `ChoresApp.kt`), unlike the admin-gated household config forms.
 *
 * Thin Hilt-wired wrapper around the testable [SettingsNotificationsContent]; follows the
 * screen/content split used by the other `ui/settings` screens.
 */
@Composable
fun SettingsNotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsNotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()

    SettingsNotificationsContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        onSelectInterval = viewModel::setPollInterval,
        onToggleOfflineAlert = viewModel::setOfflineAlertEnabled,
        onThresholdChange = viewModel::setOfflineAlertThresholdDays,
        onTogglePreference = viewModel::setPreference
    )
}

/** Bounded poll-interval choices — all ≥ WorkManager's 15-minute periodic floor. */
val POLL_INTERVAL_CHOICES: List<Pair<Long, String>> = listOf(
    15L to "15 minutes",
    30L to "30 minutes",
    60L to "1 hour",
    180L to "3 hours",
    360L to "6 hours",
    720L to "12 hours",
    1440L to "24 hours"
)

private fun intervalLabel(minutes: Long): String =
    POLL_INTERVAL_CHOICES.firstOrNull { it.first == minutes }?.second ?: "$minutes minutes"

/** "chore_due" -> "Chore due" for the per-type toggle rows (v1 emits only `chore_due`). */
private fun notificationTypeLabel(type: String): String =
    type.replace('_', ' ').replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }

@Composable
fun SettingsNotificationsContent(
    uiState: UiState<NotificationSettingsUiState>,
    saveState: UiState<Unit>,
    onSelectInterval: (Long) -> Unit,
    onToggleOfflineAlert: (Boolean) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onTogglePreference: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading ->
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val data = uiState.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.lg)
                ) {
                    // --- Poll interval (device-local) ---
                    Text("Poll interval", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "How often the app checks your server for new notifications. " +
                            "The minimum is 15 minutes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    PollIntervalPicker(
                        selectedMinutes = data.pollIntervalMinutes,
                        onSelectInterval = onSelectInterval
                    )

                    // --- Notification types (server-side, per-user) ---
                    Divider(modifier = Modifier.padding(vertical = Space.lg))
                    Text("Notification types", style = MaterialTheme.typography.titleMedium)
                    if (data.preferencesError != null) {
                        SettingsBanner(
                            message = data.preferencesError,
                            type = BannerType.ERROR,
                            modifier = Modifier.padding(top = Space.sm)
                        )
                    }
                    data.preferences.forEach { (type, enabled) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(notificationTypeLabel(type))
                            Switch(
                                modifier = Modifier.testTag("PreferenceToggle_$type"),
                                checked = enabled,
                                onCheckedChange = { onTogglePreference(type, it) }
                            )
                        }
                    }
                    if (saveState is UiState.Error) {
                        SettingsBanner(
                            message = saveState.message,
                            type = BannerType.ERROR,
                            modifier = Modifier.padding(top = Space.sm)
                        )
                    }

                    // --- Offline alert (device-local) ---
                    Divider(modifier = Modifier.padding(vertical = Space.lg))
                    Text("Offline alert", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Warn when not connected")
                        Switch(
                            modifier = Modifier.testTag("OfflineAlertToggle"),
                            checked = data.offlineAlertEnabled,
                            onCheckedChange = onToggleOfflineAlert
                        )
                    }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.sm)
                            .testTag("OfflineThresholdField"),
                        value = data.offlineAlertThresholdDays.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { if (it > 0) onThresholdChange(it) }
                        },
                        enabled = data.offlineAlertEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Alert after — N days without contact") }
                    )
                }
            }
        }
    }
}

@Composable
private fun PollIntervalPicker(
    selectedMinutes: Long,
    onSelectInterval: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Anchor is an OutlinedButton, not a read-only OutlinedTextField: a text field's own pointer
    // input swallows the tap before an outer `clickable` sees it, so the menu wouldn't open under
    // a test's (or a user's) click. A Button's onClick fires reliably.
    Box(modifier = Modifier.fillMaxWidth().padding(top = Space.sm)) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().testTag("PollIntervalField")
        ) {
            Text(text = intervalLabel(selectedMinutes), modifier = Modifier.fillMaxWidth())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            POLL_INTERVAL_CHOICES.forEach { (minutes, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelectInterval(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}
