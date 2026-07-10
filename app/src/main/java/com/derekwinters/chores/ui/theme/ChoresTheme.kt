package com.derekwinters.chores.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.tokens.DesignTokens

/**
 * Issue #120: exposes the raw [ThemeOption] currently applied by [ChoresTheme] to descendants
 * that need one of its colors with no first-class Material3 [ColorScheme] slot (e.g. `success`/
 * `warning`, used by Dashboard's trend coloring) — see [ChoresTheme]'s doc for why those two
 * colors aren't mapped onto the ColorScheme itself. Null means "no theme resolved yet / hardcoded
 * fallback", same as [ChoresTheme]'s own `themeOption` parameter.
 */
val LocalThemeOption = staticCompositionLocalOf<ThemeOption?> { null }

/**
 * Issue #24: this app's own theming mechanism (equivalent to chores-web's CSS-custom-property
 * approach) — maps a backend [ThemeOption]'s 9 hex colors onto a Compose Material3 [ColorScheme].
 * `background`/`surface`/`surface2` map to Material3's background/surface/surfaceVariant since
 * M3 doesn't have a third neutral surface tier; `success`/`warning` don't have first-class M3
 * slots either, so callers needing them (e.g. Dashboard's trend coloring) read the [ThemeOption]
 * directly via [LocalThemeOption] rather than through `MaterialTheme.colorScheme`.
 */
/**
 * Pre-theme default scheme built from the design-tokens artifact's dark set — the same
 * values chores-web-frontend prepaints on `:root` before its runtime theme resolves
 * (rollout: derekwinters/chores-web-docs#11, this repo's #13). Replaces the previous
 * bare-Material3 fallback so both clients show the same baseline until /theme resolves.
 */
internal fun tokenDefaultColorScheme(): ColorScheme = darkColorScheme(
    primary = Color(DesignTokens.ColorDark.PRIMARY),
    secondary = Color(DesignTokens.ColorDark.SECONDARY),
    tertiary = Color(DesignTokens.ColorDark.ACCENT),
    background = Color(DesignTokens.ColorDark.BACKGROUND),
    surface = Color(DesignTokens.ColorDark.SURFACE),
    surfaceVariant = Color(DesignTokens.ColorDark.SURFACE2),
    error = Color(DesignTokens.ColorDark.ERROR),
)

/**
 * Iteration 4 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's #24):
 * the `points` color role — the gold accent web applies to numeric point values. [ThemeOption]'s
 * 9-color wire format carries no points slot, so (like `success`/`warning` via
 * [LocalThemeOption]) it comes straight from the design-tokens artifact, picking the dark or
 * light set by the same background-luminance rule [ChoresTheme] uses to pick its base scheme.
 * Both sets are the same gold today, so the split is future-proofing, not a visible fork.
 */
fun pointsColor(themeOption: ThemeOption?): Color {
    val background = themeOption?.background?.let(::parseHexColor)
        ?: Color(DesignTokens.ColorDark.BACKGROUND)
    return if (background.luminance() > 0.5f) {
        Color(DesignTokens.ColorLight.POINTS)
    } else {
        Color(DesignTokens.ColorDark.POINTS)
    }
}

/** [pointsColor] against the currently applied theme ([LocalThemeOption]). */
@Composable
fun pointsColor(): Color = pointsColor(LocalThemeOption.current)

@Composable
fun ChoresTheme(themeOption: ThemeOption?, content: @Composable () -> Unit) {
    val colorScheme = if (themeOption == null) {
        tokenDefaultColorScheme()
    } else {
        val background = parseHexColor(themeOption.background)
        val primary = parseHexColor(themeOption.primary)
        val secondary = parseHexColor(themeOption.secondary)
        val accent = parseHexColor(themeOption.accent)
        val surface = parseHexColor(themeOption.surface)
        val surface2 = parseHexColor(themeOption.surface2)
        val error = parseHexColor(themeOption.error)

        // Picks the light/dark base scheme from the theme's actual background luminance (e.g.
        // "paper"/"light" vs. "dark"/"charcoal") so the slots this app doesn't map from the
        // backend's 9 colors (onPrimary, onSurface, ...) still get reasonable built-in contrast
        // defaults instead of always assuming a dark theme.
        if (background.luminance() > 0.5f) {
            lightColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = accent,
                background = background,
                surface = surface,
                surfaceVariant = surface2,
                error = error
            )
        } else {
            darkColorScheme(
                primary = primary,
                secondary = secondary,
                tertiary = accent,
                background = background,
                surface = surface,
                surfaceVariant = surface2,
                error = error
            )
        }
    }

    CompositionLocalProvider(LocalThemeOption provides themeOption) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
