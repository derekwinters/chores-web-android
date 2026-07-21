package com.derekwinters.chores.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.Notification
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.formatRelativeTimestamp
import com.derekwinters.chores.ui.theme.LocalThemeOption
import com.derekwinters.chores.ui.theme.NotificationTokens
import com.derekwinters.chores.ui.theme.Space
import com.derekwinters.chores.ui.theme.notificationBadgeShape
import com.derekwinters.chores.ui.theme.notificationLogRowShape
import com.derekwinters.chores.ui.theme.parseHexColor
import java.time.Instant

/**
 * Issue #45: the in-app Notification Log — a list of the signed-in user's notifications
 * (`GET /v1/notifications`) that **retains** acknowledged ("read") items as history, visually
 * distinguished from unread ones, with a per-row acknowledge affordance on unread rows. Reached
 * from the top-bar bell action (see `ui/ChoresApp.kt`).
 *
 * Thin Hilt-wired wrapper around the testable [NotificationLogContent], matching the screen/content
 * split used by `ui/log/ActivityLogScreen.kt`.
 */
@Composable
fun NotificationLogScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    NotificationLogContent(
        modifier = modifier,
        uiState = uiState,
        onAcknowledge = viewModel::acknowledge
    )
}

@Composable
fun NotificationLogContent(
    uiState: UiState<List<Notification>>,
    onAcknowledge: (Int) -> Unit,
    modifier: Modifier = Modifier,
    now: Instant = Instant.now()
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
                val notifications = uiState.data
                if (notifications.isEmpty()) {
                    Text(
                        modifier = Modifier.align(Alignment.Center).testTag("notificationLogEmpty"),
                        text = "No notifications"
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notifications, key = { it.id }) { notification ->
                            NotificationRow(notification = notification, onAcknowledge = onAcknowledge, now = now)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One log row. Unread rows carry a leading accent bar and a translucent fill (both tokenized via
 * [NotificationTokens]), an unread dot, and a "Mark as read" acknowledge button; read rows render
 * plain. The acknowledge affordance is a real [TextButton] rather than a whole-row tap so it
 * registers reliably in Compose tests (a read-only field/anchor would swallow the click) and so
 * read rows have no accidental tap target.
 */
@Composable
private fun NotificationRow(
    notification: Notification,
    onAcknowledge: (Int) -> Unit,
    now: Instant = Instant.now()
) {
    val unread = notification.isUnread
    val accent = unreadAccentColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.sm, vertical = Space.xs)
            .testTag(if (unread) "notificationRow_unread" else "notificationRow_read"),
        shape = notificationLogRowShape,
        colors = if (unread) {
            CardDefaults.cardColors(containerColor = accent.copy(alpha = NotificationTokens.unreadFillAlpha))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (unread) {
                Box(
                    modifier = Modifier
                        .width(NotificationTokens.unreadBarWidth)
                        .fillMaxHeight()
                        .background(accent)
                        .testTag("notificationUnreadBar")
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = NotificationTokens.logRowPaddingX,
                        vertical = NotificationTokens.logRowPaddingY
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f).testTag("notificationTitle"),
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal
                    )
                    if (unread) {
                        Box(
                            modifier = Modifier
                                .padding(start = Space.sm)
                                .size(NotificationTokens.badgeSize)
                                .clip(notificationBadgeShape)
                                .background(accent)
                                .testTag("notificationUnreadDot")
                        )
                    }
                }
                Text(
                    modifier = Modifier.padding(top = Space.xxs),
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    modifier = Modifier.padding(top = Space.xs).testTag("notificationTimestamp"),
                    text = formatRelativeTimestamp(notification.createdAt, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unread) {
                    TextButton(
                        modifier = Modifier.testTag("acknowledgeButton_${notification.id}"),
                        onClick = { onAcknowledge(notification.id) }
                    ) {
                        Text("Mark as read")
                    }
                }
            }
        }
    }
}

/**
 * Unread accent color: the runtime theme's `primary` (via [LocalThemeOption]), falling back to
 * `MaterialTheme.colorScheme.primary` — the same theme-first, `parseHexColor` lookup the Activity
 * Log's badges/aged-timestamp coloring use, rather than a hardcoded color. The token layer supplies
 * dimensions and alphas only; colors stay theme-driven.
 */
@Composable
private fun unreadAccentColor(): Color {
    val themeOption = LocalThemeOption.current
    return themeOption?.primary?.let(::parseHexColor) ?: MaterialTheme.colorScheme.primary
}
