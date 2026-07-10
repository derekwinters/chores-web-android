package com.derekwinters.chores.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.login.LoginScreen
import com.derekwinters.chores.ui.setup.SetupScreen
import com.derekwinters.chores.ui.theme.IconSize
import com.derekwinters.chores.ui.theme.Space

/**
 * Entry point shown while signed out (issue #11): asks for the server URL first, checks
 * `GET /auth/setup-status` against it, then routes to [SetupScreen] (first run) or [LoginScreen]
 * (normal). Replaces the old flow where Login owned the server URL field directly — Login still
 * has that field (pre-filled here) so a returning user can change server without restarting.
 *
 * Thin Hilt-wired wrapper around [AuthGateContent]; see AuthGateContentTest for the
 * server-check step's own behavior coverage (Setup/Login themselves are exercised via
 * SetupContentTest / LoginContentTest).
 */
@Composable
fun AuthGateScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthGateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    AuthGateContent(
        modifier = modifier,
        state = state,
        onCheckServer = viewModel::checkServer,
        onRetry = viewModel::retry,
        loginContent = { serverUrl -> LoginScreen(initialServerUrl = serverUrl) },
        setupContent = { serverUrl -> SetupScreen(serverUrl = serverUrl) }
    )
}

@Composable
fun AuthGateContent(
    state: AuthGateState,
    onCheckServer: (String) -> Unit,
    onRetry: () -> Unit,
    loginContent: @Composable (String) -> Unit,
    setupContent: @Composable (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        is AuthGateState.ShowLogin -> loginContent(state.serverUrl)
        is AuthGateState.ShowSetup -> setupContent(state.serverUrl)
        AuthGateState.EnterServerUrl, AuthGateState.Checking -> ServerUrlEntry(
            modifier = modifier,
            isChecking = state is AuthGateState.Checking,
            onCheckServer = onCheckServer
        )
        is AuthGateState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(Space.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = state.message, color = MaterialTheme.colorScheme.error)
            TextButton(modifier = Modifier.padding(top = Space.sm), onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ServerUrlEntry(
    isChecking: Boolean,
    onCheckServer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = Space.xl),
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(stringResource(R.string.server_url_label)) },
            placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !isChecking
        )

        Button(
            modifier = Modifier.padding(top = Space.lg),
            onClick = { onCheckServer(serverUrl) },
            enabled = !isChecking && serverUrl.isNotBlank()
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(IconSize.sm).padding(end = Space.sm))
            }
            Text(stringResource(R.string.continue_button))
        }
    }
}
