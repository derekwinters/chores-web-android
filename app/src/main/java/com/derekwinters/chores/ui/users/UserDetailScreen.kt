package com.derekwinters.chores.ui.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.CurrentUser
import com.derekwinters.chores.ui.CurrentUserViewModel
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatDate

/**
 * Issue #17: per-person detail screen — stats, redeem-points flow, redemption history, and a
 * chore-activity feed. The Redeem action is admin-only in this client even though chores-web's
 * own UI shows it to anyone (its backend endpoint is admin-gated, so a non-admin tapping Redeem
 * there just gets a 403 — see issue #17's "role-gating decision" note).
 *
 * Thin Hilt-wired wrapper around [UserDetailContent].
 */
@Composable
fun UserDetailScreen(
    modifier: Modifier = Modifier,
    onNavigateToHistory: () -> Unit = {},
    viewModel: UserDetailViewModel = hiltViewModel(),
    currentUserViewModel: CurrentUserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val redeemState by viewModel.redeemState.collectAsState()
    val currentUserState by currentUserViewModel.uiState.collectAsState()
    val isAdmin = (currentUserState as? UiState.Success<CurrentUser>)?.data?.isAdmin == true

    UserDetailContent(
        modifier = modifier,
        uiState = uiState,
        redeemState = redeemState,
        isAdmin = isAdmin,
        onValidateAmount = viewModel::validateRedeemAmount,
        onRedeem = viewModel::redeem,
        onDismissRedeemResult = viewModel::clearRedeemState,
        onHistoryClick = onNavigateToHistory
    )
}

@Composable
fun UserDetailContent(
    uiState: UiState<UserDetailData>,
    redeemState: UiState<Unit>,
    isAdmin: Boolean,
    onValidateAmount: (String) -> String?,
    onRedeem: (Int) -> Unit,
    onDismissRedeemResult: () -> Unit,
    onHistoryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRedeemDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val data = uiState.data
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                StatLine("Available points", data.stats.availablePoints)
                                StatLine("7-day total", data.stats.points7d)
                                StatLine("30-day total", data.stats.points30d)
                                StatLine("Total points earned", data.stats.totalPoints)
                                StatLine("Completed count", data.stats.completedCount)
                                StatLine("Skipped count", data.stats.skippedCount)

                                if (isAdmin) {
                                    Button(
                                        modifier = Modifier.padding(top = 12.dp),
                                        onClick = { showRedeemDialog = true }
                                    ) { Text("Redeem Points") }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Redemption History",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    if (data.redemptions.isEmpty()) {
                        item { Text("No redemptions yet") }
                    } else {
                        items(data.redemptions, key = { "redemption-${it.id}" }) { redemption ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${redemption.amount} pts", style = MaterialTheme.typography.bodyMedium)
                                    Text("by ${redemption.redeemedBy}", style = MaterialTheme.typography.bodyMedium)
                                    Text(formatDate(redemption.timestamp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Chore Activity", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onHistoryClick) { Text("View Full Log") }
                        }
                    }
                    if (data.activity.isEmpty()) {
                        item { Text("No activity yet") }
                    } else {
                        items(data.activity, key = { "activity-${it.id}" }) { entry ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.choreName, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        entry.action.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(formatDate(entry.timestamp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                if (showRedeemDialog) {
                    RedeemDialog(
                        availablePoints = data.stats.availablePoints,
                        onValidateAmount = onValidateAmount,
                        onConfirm = { amount ->
                            onRedeem(amount)
                            showRedeemDialog = false
                        },
                        onDismiss = { showRedeemDialog = false }
                    )
                }
            }
        }
    }

    if (redeemState is UiState.Error) {
        AlertDialog(
            onDismissRequest = onDismissRedeemResult,
            title = { Text("Redeem failed") },
            text = { Text(redeemState.message) },
            confirmButton = { TextButton(onClick = onDismissRedeemResult) { Text("OK") } }
        )
    }
}

@Composable
private fun StatLine(label: String, value: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value.toString())
    }
}

@Composable
private fun RedeemDialog(
    availablePoints: Int,
    onValidateAmount: (String) -> String?,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val error = if (amountText.isNotBlank()) onValidateAmount(amountText) else null
    val amount = amountText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redeem Points") },
        text = {
            Column {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                } else if (amount != null) {
                    Text("Before: $availablePoints -> After: ${availablePoints - amount}")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let(onConfirm) },
                enabled = amount != null && error == null
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
