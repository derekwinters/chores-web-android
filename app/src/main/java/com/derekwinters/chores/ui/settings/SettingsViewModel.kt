package com.derekwinters.chores.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.derekwinters.chores.BuildConfig
import com.derekwinters.chores.data.model.AppConfig
import com.derekwinters.chores.data.model.AppVersionUiState
import com.derekwinters.chores.data.model.BackendVersionUiState
import com.derekwinters.chores.data.network.ApiException
import com.derekwinters.chores.data.network.HttpErrorMessages
import com.derekwinters.chores.data.repository.AppVersionRepository
import com.derekwinters.chores.data.repository.ConfigRepository
import com.derekwinters.chores.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Issue #20 behaviors: General/Auth/Chores/About settings forms. General/Auth/Chores read/write
 * the shared `GET/PUT /v1/config` endpoint (`AppConfig`).
 *
 * Issue #35 (correcting the original #20 design): the About tab's "Current version"/"Latest
 * version" section no longer comes from the backend — the backend has no way to know what
 * version of this Android app is actually deployed, only the app itself does
 * ([BuildConfig.VERSION_NAME]). [appVersionState] is driven by [AppVersionRepository], which
 * checks this app's own GitHub releases directly, client-side, decoupled from the backend
 * entirely. The backend's own version/update status is now a *separate* "Backend version"
 * section, [backendVersionState], sourced from the public `GET /version` endpoint
 * (chores-web-backend#27) via [ConfigRepository.getBackendVersion] — falling back to
 * [BackendVersionUiState.Unsupported] (rendered as "unknown" / "unsupported check") if that call
 * fails, e.g. against an older backend that predates the endpoint.
 *
 * The "Check for updates automatically" switch and interval (still part of [AppConfig], still
 * round-tripped via the normal [save] flow — no backend contract change needed) and the "Check
 * Now" button are repurposed by this same issue: they now control/trigger the client-side
 * GitHub check ([checkAppVersionNow]) rather than the removed backend update-check endpoints,
 * since only the client-side check has anything left to "check now" — the backend section is
 * simply whatever `GET /version` currently reports, live, on every load.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val appVersionRepository: AppVersionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AppConfig>>(UiState.Loading)
    val uiState: StateFlow<UiState<AppConfig>> = _uiState.asStateFlow()

    private val _saveState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val saveState: StateFlow<UiState<Unit>> = _saveState.asStateFlow()

    private val _appVersionState = MutableStateFlow<AppVersionUiState>(AppVersionUiState.Loading)
    val appVersionState: StateFlow<AppVersionUiState> = _appVersionState.asStateFlow()

    private val _backendVersionState =
        MutableStateFlow<BackendVersionUiState>(BackendVersionUiState.Loading)
    val backendVersionState: StateFlow<BackendVersionUiState> = _backendVersionState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            configRepository.getConfig()
                .onSuccess { config -> _uiState.value = UiState.Success(config) }
                .onFailure { error -> _uiState.value = UiState.Error(errorMessage(error)) }
        }
        viewModelScope.launch {
            // getBackendVersion() already never throws (safeApiCall), so a 404/timeout/offline
            // backend simply lands here as Result.failure -> the required Unsupported fallback.
            _backendVersionState.value = configRepository.getBackendVersion()
                .getOrDefault(BackendVersionUiState.Unsupported)
        }
        checkAppVersion(forceRefresh = false)
    }

    /** Issue #35: About tab's "Check Now" — re-checks this app's own GitHub releases now, bypassing the cache. */
    fun checkAppVersionNow() {
        checkAppVersion(forceRefresh = true)
    }

    private fun checkAppVersion(forceRefresh: Boolean) {
        viewModelScope.launch {
            _appVersionState.value = AppVersionUiState.Loading
            _appVersionState.value = appVersionRepository.checkForUpdate(
                currentVersion = BuildConfig.VERSION_NAME,
                forceRefresh = forceRefresh
            )
        }
    }

    fun save(updated: AppConfig) {
        _saveState.value = UiState.Loading
        viewModelScope.launch {
            configRepository.updateConfig(updated)
                .onSuccess { config ->
                    _uiState.value = UiState.Success(config)
                    _saveState.value = UiState.Success(Unit)
                }
                .onFailure { error -> _saveState.value = UiState.Error(errorMessage(error)) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        (error as? ApiException)?.message ?: HttpErrorMessages.NETWORK_ERROR
}
