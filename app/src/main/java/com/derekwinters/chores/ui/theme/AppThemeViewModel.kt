package com.derekwinters.chores.ui.theme

import androidx.lifecycle.ViewModel
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Issue #25 behavior: "Theme resolution order on app start: personal theme (if set) → household
 * default → hardcoded fallback (used if the initial theme fetch fails, e.g. offline)".
 *
 * Issue #156: thin collector over [ThemeRepository.resolvedTheme], which is the single source of
 * truth for the live app-wide theme (re-resolved on auth transitions and after theme-changing
 * writes — see [ThemeRepository] for why that logic moved out of this ViewModel's own `init {}`).
 */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    themeRepository: ThemeRepository
) : ViewModel() {

    /** Null means "hardcoded fallback" — [ChoresTheme] treats null as MaterialTheme's own default. */
    val currentTheme: StateFlow<ThemeOption?> = themeRepository.resolvedTheme
}
