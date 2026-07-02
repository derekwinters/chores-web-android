package com.derekwinters.chores.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.R

/**
 * Notification screen: lets the user trigger a local test notification.
 *
 * Stateless composable — no ViewModel/Hilt for this bootstrap issue (see issue #2 grilling notes).
 *
 * @param onSendTestNotification invoked when the user taps the "Send Test Notification" button.
 *   The caller is responsible for permission handling and actually posting the notification.
 */
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    onSendTestNotification: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.notification_screen_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = onSendTestNotification
        ) {
            Text(stringResource(R.string.send_test_notification))
        }
    }
}
