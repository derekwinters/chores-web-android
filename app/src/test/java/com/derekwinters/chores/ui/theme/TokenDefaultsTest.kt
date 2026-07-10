package com.derekwinters.chores.ui.theme

import androidx.compose.ui.graphics.Color
import com.derekwinters.chores.tokens.DesignTokens
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Iteration 2 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's #13):
 * the pre-theme default ColorScheme comes from the published design-tokens artifact's dark set
 * (the same values chores-web-frontend prepaints on :root), not hand-authored literals.
 */
class TokenDefaultsTest {

    @Test
    fun `token dark set drives the default color scheme`() {
        val scheme = tokenDefaultColorScheme()
        assertEquals(Color(DesignTokens.ColorDark.BACKGROUND), scheme.background)
        assertEquals(Color(DesignTokens.ColorDark.SURFACE), scheme.surface)
        assertEquals(Color(DesignTokens.ColorDark.SURFACE2), scheme.surfaceVariant)
        assertEquals(Color(DesignTokens.ColorDark.PRIMARY), scheme.primary)
        assertEquals(Color(DesignTokens.ColorDark.SECONDARY), scheme.secondary)
        assertEquals(Color(DesignTokens.ColorDark.ACCENT), scheme.tertiary)
        assertEquals(Color(DesignTokens.ColorDark.ERROR), scheme.error)
    }

    @Test
    fun `artifact carries the backend dark palette verbatim`() {
        assertEquals(0xFF080C14, DesignTokens.ColorDark.BACKGROUND)
        assertEquals(0xFF16202E, DesignTokens.ColorDark.SURFACE)
        assertEquals(0xFF1E2D40, DesignTokens.ColorDark.SURFACE2)
        assertEquals(0xFF73B1DD, DesignTokens.ColorDark.ACCENT)
    }

    @Test
    fun `artifact carries the foundation scales and decisions`() {
        assertEquals(16, DesignTokens.Space.LG)
        assertEquals(24, DesignTokens.Space.XL)
        assertEquals(64, DesignTokens.Size.TOPBAR) // decision: M3 TopAppBar default
        assertEquals(12, DesignTokens.Radius.MD)
        assertEquals(8, DesignTokens.Elevation.ELEVATION_2)
        assertEquals(300, DesignTokens.Motion.DURATION_LG)
        assertEquals(0.92f, DesignTokens.Motion.SCALE_IN_START)
        assertEquals(0.15f, DesignTokens.Alpha.TINT)
    }
}
