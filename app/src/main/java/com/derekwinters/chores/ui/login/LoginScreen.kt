package com.derekwinters.chores.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.R
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.theme.Corner
import com.derekwinters.chores.ui.theme.Elevations
import com.derekwinters.chores.ui.theme.IconSize
import com.derekwinters.chores.ui.theme.Sizes
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #5 behavior: "Login screen: server URL + username/password fields, calls
 * POST /auth/login, persists token + URL". Successful login is observed by ChoresApp via
 * [com.derekwinters.chores.ui.SessionViewModel] — this screen doesn't navigate itself. Issue #11
 * extends this with the forced-password-reset flow.
 *
 * Thin Hilt-wired wrapper around [LoginContent]/[ForcedPasswordResetContent] so this composable
 * itself doesn't need to be exercised in tests (which would require a Hilt test component); see
 * LoginContentTest.
 *
 * @param initialServerUrl pre-fills the Server URL field (issue #11: arriving here from
 *   [com.derekwinters.chores.ui.auth.AuthGateScreen] after a setup-status check already knows it).
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    initialServerUrl: String = "",
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val resetRequired by viewModel.resetRequired.collectAsState()

    val resetToken = resetRequired
    if (resetToken != null) {
        ForcedPasswordResetContent(
            modifier = modifier,
            uiState = uiState,
            onSubmit = viewModel::submitPasswordReset,
            onCancel = viewModel::cancelPasswordReset
        )
    } else {
        LoginContent(
            modifier = modifier,
            uiState = uiState,
            initialServerUrl = initialServerUrl,
            onLogin = viewModel::login
        )
    }
}

@Composable
fun LoginContent(
    uiState: UiState<Unit>,
    onLogin: (serverUrl: String, username: String, password: String) -> Unit,
    modifier: Modifier = Modifier,
    initialServerUrl: String = ""
) {
    var serverUrl by remember { mutableStateOf(initialServerUrl) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading = uiState is UiState.Loading

    // Issue #63: elevated, centered card container around the form, matching web's `.login-card`
    // treatment (the screen previously rendered the form directly on the screen background with
    // no card framing).
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = Sizes.formMax)
                .padding(Space.xl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevations.level2)
        ) {
            Column(
                modifier = Modifier.padding(Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Issue #64: app-branding heading above the login title, matching web's branded
                // header (and issue #58's nav-shell serif treatment, for visual consistency).
                Text(
                    text = stringResource(R.string.login_app_branding),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.login_screen_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = Space.xs)
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.xl),
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(R.string.server_url_label)) },
                    placeholder = { Text(stringResource(R.string.server_url_placeholder)) },
                    singleLine = true,
                    // Issue #66: flat rectangular corners, matching web (default M3 shape is
                    // more rounded).
                    shape = LoginFieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.sm),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username_label)) },
                    singleLine = true,
                    shape = LoginFieldShape,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Space.sm),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = LoginFieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading
                )

                // Issue #65: bordered/tinted callout box instead of plain red text, matching
                // web's error styling.
                if (uiState is UiState.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.sm)
                            .clip(RoundedCornerShape(Corner.xs))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(Corner.xs))
                            .padding(Space.md)
                    ) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Button(
                    modifier = Modifier.padding(top = Space.lg).fillMaxWidth(),
                    onClick = { onLogin(serverUrl, username, password) },
                    enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    // Issue #66: flat rectangular shape + flat (no-elevation) blue button,
                    // matching web (default M3 Button is a fully-rounded "stadium" shape with
                    // elevation).
                    shape = LoginFieldShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = Elevations.level0,
                        pressedElevation = Elevations.level0,
                        disabledElevation = Elevations.level0
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(IconSize.sm).padding(end = Space.sm))
                    }
                    Text(stringResource(R.string.login_button))
                }
            }
        }
    }
}

/** Issue #66: shared flat rectangular shape for Login's input fields and submit button. */
private val LoginFieldShape = RoundedCornerShape(Corner.xs)

/**
 * Issue #11: shown instead of the normal login form when the backend returns a 403 with a
 * `reset_token` — lets the user set a new password (min 8 chars, confirm) rather than seeing a
 * dead-end login error.
 */
@Composable
fun ForcedPasswordResetContent(
    uiState: UiState<Unit>,
    onSubmit: (newPassword: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val isLoading = uiState is UiState.Loading
    val passwordsMatch = newPassword == confirmPassword
    val meetsMinLength = newPassword.length >= 8

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.password_reset_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            modifier = Modifier.padding(top = Space.sm),
            text = stringResource(R.string.password_reset_description),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = Space.xl),
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text(stringResource(R.string.new_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text(stringResource(R.string.confirm_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        if (newPassword.isNotEmpty() && !meetsMinLength) {
            Text(
                modifier = Modifier.padding(top = Space.sm),
                text = stringResource(R.string.password_too_short),
                color = MaterialTheme.colorScheme.error
            )
        } else if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                modifier = Modifier.padding(top = Space.sm),
                text = stringResource(R.string.passwords_do_not_match),
                color = MaterialTheme.colorScheme.error
            )
        } else if (uiState is UiState.Error) {
            Text(
                modifier = Modifier.padding(top = Space.sm),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            modifier = Modifier.padding(top = Space.lg),
            onClick = { onSubmit(newPassword) },
            enabled = !isLoading && meetsMinLength && passwordsMatch
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(IconSize.sm).padding(end = Space.sm))
            }
            Text(stringResource(R.string.password_reset_submit))
        }

        TextButton(modifier = Modifier.padding(top = Space.sm), onClick = onCancel, enabled = !isLoading) {
            Text(stringResource(R.string.cancel))
        }
    }
}
