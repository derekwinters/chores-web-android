package com.derekwinters.chores.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
 * Issue #11: first-run "Create Admin Account" flow, shown by
 * [com.derekwinters.chores.ui.auth.AuthGateScreen] instead of Login when the backend reports
 * `setup_needed`.
 *
 * Thin Hilt-wired wrapper around [SetupContent]; see SetupContentTest for behavior coverage.
 */
@Composable
fun SetupScreen(
    serverUrl: String,
    modifier: Modifier = Modifier,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SetupContent(
        modifier = modifier,
        uiState = uiState,
        onCreateAccount = { username, password, requireAuth ->
            viewModel.createAdminAccount(serverUrl, username, password, requireAuth)
        }
    )
}

@Composable
fun SetupContent(
    uiState: UiState<Unit>,
    onCreateAccount: (username: String, password: String, requireAuth: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var requireAuth by remember { mutableStateOf(true) }

    val isLoading = uiState is UiState.Loading
    val passwordsMatch = password == confirmPassword

    // Issue #76: elevated, centered card container around the form, matching Login's `.login-card`
    // treatment (see issue #63) — the screen previously rendered the form directly on the screen
    // background with no card framing.
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
                // Setup's form has more fields than Login's (username/password/confirm +
                // Require-Authentication row + button), which can exceed a small device's
                // viewport height inside the fixed-position Card. verticalScroll keeps every
                // field (including the submit button) reachable instead of silently clipping/
                // overflowing past the visible/hit-testable area.
                modifier = Modifier
                    .padding(Space.xl)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Issue #79: app-branding heading above the title, mirroring Login's treatment
                // (issue #64).
                Text(
                    text = stringResource(R.string.login_app_branding),
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(R.string.setup_title),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = Space.xs)
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.xl),
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username_label)) },
                    singleLine = true,
                    // Issue #92: flat rectangular corners, mirroring Login (issue #66).
                    shape = SetupFieldShape,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = SetupFieldShape,
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
                    shape = SetupFieldShape,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled = !isLoading
                )

                // Issue #89: bordered/tinted callout box instead of plain text, mirroring
                // Login's error styling (issue #65).
                if (confirmPassword.isNotEmpty() && !passwordsMatch) {
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
                            text = stringResource(R.string.passwords_do_not_match),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else if (uiState is UiState.Error) {
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

                // Issue #83: checkbox-left layout (checkbox precedes its label, replacing the
                // previous Switch-on-the-right layout), with a dynamic hint beneath describing
                // the consequence of the current setting.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = requireAuth, onCheckedChange = { requireAuth = it }, enabled = !isLoading)
                    Text(stringResource(R.string.require_authentication_label))
                }
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.xs),
                    text = stringResource(
                        if (requireAuth) R.string.require_authentication_hint_enabled
                        else R.string.require_authentication_hint_disabled
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    modifier = Modifier.padding(top = Space.lg).fillMaxWidth(),
                    onClick = { onCreateAccount(username, password, requireAuth) },
                    enabled = !isLoading && username.isNotBlank() && password.isNotBlank() && passwordsMatch,
                    // Issue #92: flat rectangular shape + flat (no-elevation) blue button,
                    // mirroring Login (issue #66).
                    shape = SetupFieldShape,
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
                    Text(stringResource(R.string.setup_submit))
                }

                // Issue #85: footer disclaimer explaining this is a one-time, first-run flow.
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = Space.lg),
                    text = stringResource(R.string.setup_footer_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Issue #92: shared flat rectangular shape for Setup's input fields and buttons. */
private val SetupFieldShape = RoundedCornerShape(Corner.xs)
