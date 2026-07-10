package com.derekwinters.chores.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #11: shows a "Database initializing…" state while [isReady] is false, then reveals
 * [content] — needed for freshly-started self-hosted backends whose DB migration hasn't finished
 * yet.
 */
@Composable
fun DbReadinessGate(isReady: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (isReady) {
        content()
        return
    }

    Column(
        modifier = modifier.fillMaxSize().padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Text(
            modifier = Modifier.padding(top = Space.lg),
            text = stringResource(R.string.database_initializing),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
