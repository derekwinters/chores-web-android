package com.derekwinters.chores.ui.theme

import com.derekwinters.chores.MainDispatcherRule
import com.derekwinters.chores.data.network.FakeChoresApi
import com.derekwinters.chores.data.network.dto.ThemeColorsDto
import com.derekwinters.chores.data.network.dto.ThemeDto
import com.derekwinters.chores.data.model.toDomain
import com.derekwinters.chores.data.repository.ThemeRepository
import com.derekwinters.chores.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun themeColorsDto() = ThemeColorsDto(
    bg = "#000000",
    surface = "#111111",
    surface2 = "#222222",
    accent = "#333333",
    primary = "#444444",
    secondary = "#555555",
    success = "#00FF00",
    warning = "#FFFF00",
    error = "#FF0000"
)

private fun themeDto(id: String, name: String) = ThemeDto(id = id, name = name, colors = themeColorsDto())

/** Issue #24 behaviors: theme list, create-via-copy, rename, delete, set-default. */
class ThemeAdminViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun load_populatesThemes() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(themesResult = listOf(themeDto("1", "Dark")))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is UiState.Success)
        assertEquals(1, (state as UiState.Success).data.size)
    }

    @Test
    fun createTheme_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val sourceTheme = themeDto("1", "Dark").toDomain()
        val api = FakeChoresApi(saveThemeResult = themeDto("2", "My Theme"))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.createTheme("My Theme", sourceTheme)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals("My Theme", api.lastSaveThemeRequest?.name)
        assertEquals(sourceTheme.primary, api.lastSaveThemeRequest?.colors?.primary)
    }

    @Test
    fun renameTheme_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(renameThemeResult = themeDto("2", "Renamed"))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.renameTheme("2", "Renamed")
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals("2", api.lastRenameThemeId)
        assertEquals("Renamed", api.lastRenameThemeRequest?.name)
    }

    @Test
    fun updateColors_success_sendsFullPaletteWithoutName() = runTest(mainDispatcherRule.testDispatcher) {
        val editedTheme = themeDto("2", "My Theme").toDomain().copy(primary = "#ABCDEF")
        val api = FakeChoresApi(updateThemeResult = themeDto("2", "My Theme"))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.updateColors("2", editedTheme)
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals("2", api.lastUpdateThemeId)
        assertEquals("#ABCDEF", api.lastUpdateThemeRequest?.colors?.primary)
        assertEquals(editedTheme.background, api.lastUpdateThemeRequest?.colors?.bg)
        // Rename stays on the rename endpoint; the color update must not resend the name.
        assertEquals(null, api.lastUpdateThemeRequest?.name)
    }

    @Test
    fun setDefaultTheme_callsRepository() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi(setDefaultThemeResult = themeDto("1", "Dark"))
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.setDefaultTheme("1")
        advanceUntilIdle()

        assertEquals("1", api.lastSetDefaultThemeId)
    }

    @Test
    fun deleteTheme_success_reloadsList() = runTest(mainDispatcherRule.testDispatcher) {
        val api = FakeChoresApi()
        val viewModel = ThemeAdminViewModel(ThemeRepository(api))
        advanceUntilIdle()

        viewModel.deleteTheme("2")
        advanceUntilIdle()

        assertEquals(UiState.Success(Unit), viewModel.actionState.value)
        assertEquals("2", api.lastDeleteThemeId)
    }
}
