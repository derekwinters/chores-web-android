package com.derekwinters.chores.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.ui.UiState
import com.derekwinters.chores.ui.common.BannerType
import com.derekwinters.chores.ui.common.SettingsBanner
import com.derekwinters.chores.ui.common.formatDateTime
import com.derekwinters.chores.ui.theme.Space

/**
 * Issue #88: About settings section screen (independently-routed, shared SettingsViewModel scoped
 * to settings nav graph).
 *
 * Issue #35 (correcting #20's original design): three sections —
 *  1. This app's own version, from a client-side GitHub-releases check ([AppVersionUiState]),
 *     compared against `BuildConfig.VERSION_NAME`. The "Check for updates automatically"
 *     switch/interval (still part of [AppConfig]) and "Check Now" button now govern/trigger this
 *     check, not the backend.
 *  2. The backend's own version ([BackendVersionUiState], `GET /version`), visually distinct from
 *     (1) since the two are unrelated versions of two different pieces of software. Falls back to
 *     "unknown" / "unsupported check" — never a crash — if the backend doesn't support the
 *     endpoint or is unreachable.
 *  3. Outbound links to the docs repo and the frontend/backend release pages.
 */
@Composable
fun SettingsAboutScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(viewModelStoreOwner = navController.getBackStackEntry("settings"))
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val appVersionState by viewModel.appVersionState.collectAsState()
    val backendVersionState by viewModel.backendVersionState.collectAsState()

    SettingsAboutContent(
        modifier = modifier,
        uiState = uiState,
        saveState = saveState,
        appVersionState = appVersionState,
        backendVersionState = backendVersionState,
        onSave = viewModel::save,
        onCheckAppVersionNow = viewModel::checkAppVersionNow
    )
}

@Composable
fun SettingsAboutContent(
    uiState: UiState<AppConfig>,
    saveState: UiState<Unit>,
    appVersionState: AppVersionUiState,
    backendVersionState: BackendVersionUiState,
    onSave: (AppConfig) -> Unit,
    onCheckAppVersionNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                var draft by remember(uiState.data) { mutableStateOf(uiState.data) }
                val isSaving = saveState is UiState.Loading
                val isDirty = draft != uiState.data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Space.lg)
                ) {
                    Divider(modifier = Modifier.padding(bottom = Space.lg))

                    // --- This app's own version (client-side GitHub-releases check, issue #35) ---
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    AppVersionSection(appVersionState)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Space.sm),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Check for updates automatically")
                        Switch(
                            checked = draft.updateCheckEnabled,
                            onCheckedChange = { draft = draft.copy(updateCheckEnabled = it) }
                        )
                    }

                    TextButton(onClick = onCheckAppVersionNow) { Text("Check Now") }

                    // --- Backend's own version (GET /version, issue #35) ---
                    Divider(modifier = Modifier.padding(vertical = Space.lg))
                    Text("Backend version", style = MaterialTheme.typography.titleMedium)
                    BackendVersionSection(backendVersionState)

                    // --- Repo links (issue #35) ---
                    Divider(modifier = Modifier.padding(vertical = Space.lg))
                    Text("Links", style = MaterialTheme.typography.titleMedium)
                    RepoLinksSection(context)

                    if (saveState is UiState.Error) {
                        SettingsBanner(message = saveState.message, type = BannerType.ERROR, modifier = Modifier.padding(top = Space.sm))
                    }

                    Button(
                        modifier = Modifier.padding(top = Space.lg),
                        onClick = { onSave(draft) },
                        enabled = isDirty && !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.padding(end = Space.sm))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun AppVersionSection(appVersionState: AppVersionUiState) {
    when (appVersionState) {
        is AppVersionUiState.Loading -> Text(
            "Current version: checking…",
            modifier = Modifier.padding(top = Space.sm)
        )
        is AppVersionUiState.Checked -> {
            Text(
                "Current version: ${appVersionState.currentVersion}",
                modifier = Modifier.padding(top = Space.sm)
            )
            Text("Latest version: ${appVersionState.latestVersion}")
            if (appVersionState.updateAvailable) {
                Text("Update available!", color = MaterialTheme.colorScheme.error)
            }
        }
        is AppVersionUiState.Unavailable -> {
            Text(
                "Current version: ${appVersionState.currentVersion}",
                modifier = Modifier.padding(top = Space.sm)
            )
            Text("Latest version: unknown")
        }
    }
}

/**
 * Issue #35's required fallback: [BackendVersionUiState.Unsupported] renders version as the
 * literal string "unknown" and status as the literal string "unsupported check" — never a crash,
 * never blocking the rest of the screen.
 */
@Composable
private fun BackendVersionSection(backendVersionState: BackendVersionUiState) {
    when (backendVersionState) {
        is BackendVersionUiState.Loading -> Text(
            "Backend version: checking…",
            modifier = Modifier.padding(top = Space.sm)
        )
        is BackendVersionUiState.Available -> {
            Text(
                "Backend version: ${backendVersionState.version}",
                modifier = Modifier.padding(top = Space.sm)
            )
            Text(
                "Backend update status: " +
                    if (backendVersionState.updateAvailable) "update available" else "up to date"
            )
            backendVersionState.latestVersion?.let { Text("Backend latest version: $it") }
            backendVersionState.checkedAt?.let { Text("Backend last checked: ${formatDateTime(it)}") }
        }
        is BackendVersionUiState.Unsupported -> {
            Text(
                "Backend version: unknown",
                modifier = Modifier.padding(top = Space.sm)
            )
            Text("Backend update status: unsupported check")
        }
    }
}

@Composable
private fun RepoLinksSection(context: Context) {
    TextButton(
        modifier = Modifier.padding(top = Space.sm),
        onClick = { openUrl(context, RepoLinks.DOCS) }
    ) { Text("chores-web-docs") }
    TextButton(onClick = { openUrl(context, RepoLinks.FRONTEND_RELEASES) }) { Text("chores-web-frontend releases") }
    TextButton(onClick = { openUrl(context, RepoLinks.BACKEND_RELEASES) }) { Text("chores-web-backend releases") }
}

/** Issue #35: the three About-screen outbound links, opened via [Intent.ACTION_VIEW]. */
object RepoLinks {
    const val DOCS = "https://github.com/derekwinters/chores-web-docs"
    const val FRONTEND_RELEASES = "https://github.com/derekwinters/chores-web-frontend/releases"
    const val BACKEND_RELEASES = "https://github.com/derekwinters/chores-web-backend/releases"
}

private fun openUrl(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
