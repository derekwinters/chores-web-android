package com.derekwinters.chores.ui.login

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState

/**
 * Issue #5 behavior: "Login screen: server URL + username/password fields, calls
 * POST /auth/login, persists token + URL". Successful login is observed by ChoresApp via
 * [com.derekwinters.chores.ui.SessionViewModel] — this screen doesn't navigate itself.
 *
 * Thin Hilt-wired wrapper around [LoginContent] so this composable itself doesn't need to be
 * exercised in tests (which would require a Hilt test component); see LoginContentTest.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LoginContent(
        modifier = modifier,
        uiState = uiState,
        onLogin = viewModel::login
    )
}

@Composable
fun LoginContent(
    uiState: UiState<Unit>,
    onLogin: (serverUrl: String, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = uiState is UiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_screen_title),
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text(stringResource(R.string.server_url_label)) },
            placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            enabled = !isLoading
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.username_label)) },
            singleLine = true,
            enabled = !isLoading
        )

        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        if (uiState is UiState.Error) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            modifier = Modifier.padding(top = 16.dp),
            onClick = { onLogin(serverUrl, username, password) },
            enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp).padding(end = 8.dp))
            }
            Text(stringResource(R.string.login_button))
        }
    }
}
