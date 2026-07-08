package com.derekwinters.chores.ui.theme

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.auth.FakeCredentialStore
import com.derekwinters.chores.data.auth.SessionManager
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.CurrentThemeDto
import com.derekwinters.chores.data.network.dto.ThemeColorsDto
import com.derekwinters.chores.data.network.dto.ThemeDefaultInfoDto
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Issue #156: [ThemeRepository] now depends on [SessionManager] to drive its shared
 *  [ThemeRepository.resolvedTheme] flow; these tests don't exercise that flow directly, so an
 *  unauthenticated fake session is enough. */
private fun themeRepository(api: FakeChoresApi) = ThemeRepository(api, SessionManager(FakeCredentialStore()))

private fun themeColorsDto() = ThemeColorsDto(
    bg = "#000000", surface = "#111111", surface2 = "#222222", accent = "#333333",
    primary = "#444444", secondary = "#555555", success = "#0F0", warning = "#FF0", error = "#F00"
)

private fun themeDto(id: String, name: String) = ThemeDto(id = id, name = name, colors = themeColorsDto())

private fun currentThemeDto(id: String, name: String, isPersonal: Boolean = false) =
    CurrentThemeDto(id = id, name = name, colors = themeColorsDto(), is_personal = isPersonal)

/** Issue #25 behaviors: resolves current theme (personal override vs. household default). */
class ThemePreferenceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_exposesThemesAndCurrentSelection() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            themesResult = listOf(themeDto("1", "Dark"), themeDto("2", "Light")),
            currentThemeResult = currentThemeDto("2", "Light", isPersonal = true),
            defaultThemeInfoResult = ThemeDefaultInfoDto(id = "1", name = "Dark")
        )
        val viewModel = ThemePreferenceViewModel(themeRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        val data = (state as UiState.Success).data
        assertEquals(2, data.themes.size)
        assertTrue(data.current.isPersonalOverride)
        assertEquals("Light", data.current.theme.name)
        assertEquals("Dark", data.defaultInfo.name)
    }

    @Test
    fun selectTheme_clearOverride_callsClearPersonalEndpoint() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            currentThemeResult = currentThemeDto("1", "Dark"),
            defaultThemeInfoResult = ThemeDefaultInfoDto(id = "1", name = "Dark")
        )
        val viewModel = ThemePreferenceViewModel(themeRepository(api))
        advanceUntilIdle()

        viewModel.selectTheme(null)
        advanceUntilIdle()

        assertTrue(api.lastClearPersonalThemeCalled)
        assertNull(api.lastSetPersonalThemeId)
    }

    @Test
    fun selectTheme_specificTheme_sendsThemeId() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(
            currentThemeResult = currentThemeDto("1", "Dark"),
            defaultThemeInfoResult = ThemeDefaultInfoDto(id = "1", name = "Dark"),
            setPersonalThemeResult = themeDto("3", "Pink")
        )
        val viewModel = ThemePreferenceViewModel(themeRepository(api))
        advanceUntilIdle()

        viewModel.selectTheme("3")
        advanceUntilIdle()

        assertEquals("3", api.lastSetPersonalThemeId)
    }
}
