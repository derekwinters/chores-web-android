package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.tokens.DesignTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Iteration 4 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's #24):
 * the 0.3.0 artifact carries the `DesignTokens.Component` tier this app's composables bind to
 * (pill badge, chore row, card elevations, top bar), plus the mapping helpers Tokens.kt /
 * ChoresTheme.kt add on top (elevation-index resolution, pill shape, the points color role).
 */
class ComponentTokensTest {

    @Test
    fun `artifact carries the component constants the app binds to`() {
        // Pill badge (ActivityLogScreen's PillBadge).
        assertEquals(9999, DesignTokens.Component.PILL_BADGE_RADIUS)
        assertEquals(10, DesignTokens.Component.PILL_BADGE_PADDING_X)
        assertEquals(4, DesignTokens.Component.PILL_BADGE_PADDING_Y)
        assertEquals(0.15f, DesignTokens.Component.PILL_BADGE_FILL_ALPHA)

        // Chore row (ChoreListScreen's ChoreRow).
        assertEquals(16, DesignTokens.Component.CHORE_ROW_PADDING_OUTER_X)
        assertEquals(8, DesignTokens.Component.CHORE_ROW_PADDING_OUTER_Y)
        assertEquals(16, DesignTokens.Component.CHORE_ROW_PADDING_INNER)
        assertEquals(20, DesignTokens.Component.CHORE_ROW_PADDING_INNER_WITH_BAR)
        assertEquals(4, DesignTokens.Component.CHORE_ROW_ACCENT_BAR_WIDTH)
        assertEquals(4, DesignTokens.Component.CHORE_ROW_ACTION_GAP)

        // Cards (elevation values are level indices, not dp — see the resolver test below).
        assertEquals(0, DesignTokens.Component.CARD_ELEVATION_RESTING)
        assertEquals(2, DesignTokens.Component.CARD_ELEVATION_STAT)
        assertEquals(3, DesignTokens.Component.CARD_ELEVATION_SELECTED)

        // Top bar (ChoresApp): height equals M3 TopAppBar's 64dp default, avatar is 32dp.
        assertEquals(64, DesignTokens.Component.TOP_BAR_HEIGHT)
        assertEquals(32, DesignTokens.Component.TOP_BAR_AVATAR_SIZE)
    }

    @Test
    fun `elevation level indices resolve onto the foundation dp scale`() {
        assertEquals(0.dp, elevationForLevel(0))
        assertEquals(DesignTokens.Elevation.ELEVATION_2.dp, elevationForLevel(2))
        assertEquals(8, DesignTokens.Elevation.ELEVATION_2)

        assertEquals(0.dp, CardTokens.restingElevation)
        assertEquals(8.dp, CardTokens.statElevation)
        assertEquals(12.dp, CardTokens.selectedElevation)
    }

    @Test
    fun `component token re-exports mirror the artifact`() {
        assertEquals(RoundedCornerShape(9999.dp), pillShape)
        assertEquals(10.dp, PillBadgeTokens.paddingX)
        assertEquals(4.dp, PillBadgeTokens.paddingY)
        assertEquals(0.15f, PillBadgeTokens.fillAlpha)
        assertEquals(4.dp, ChoreRowTokens.accentBarWidth)
        assertEquals(20.dp, ChoreRowTokens.paddingInnerWithBar)
        assertEquals(32.dp, TopBarTokens.avatarSize)
    }

    @Test
    fun `points color picks the dark or light token set by background luminance`() {
        // Both sets are the same gold today; the assertion still pins the selection rule.
        assertEquals(Color(DesignTokens.ColorDark.POINTS), pointsColor(null))
        assertEquals(Color(DesignTokens.ColorDark.POINTS), pointsColor(themeOption(background = "#080c14")))
        assertEquals(Color(DesignTokens.ColorLight.POINTS), pointsColor(themeOption(background = "#f0ede6")))
        assertEquals(0xFFC9A84C, DesignTokens.ColorDark.POINTS)
        assertEquals(0xFFC9A84C, DesignTokens.ColorLight.POINTS)
    }

    private fun themeOption(background: String) = ThemeOption(
        id = "test",
        name = "Test",
        background = background,
        surface = "#16202e",
        surface2 = "#1e2d40",
        accent = "#73b1dd",
        primary = "#3574b3",
        secondary = "#4a5568",
        success = "#3db87a",
        warning = "#e8a930",
        error = "#e05c6a"
    )
}
